import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Main {
    public static void main(String[] args) {
        String resolverAddress = args[1].substring(11); // Extract resolver address (e.g., "8.8.8.8:53")
        String[] resolverParts = resolverAddress.split(":");
        String resolverIP = resolverParts[0];
        int resolverPort = Integer.parseInt(resolverParts[1]);

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Request
                DNSMessage requestMessage = new DNSMessage(buf);

                // Handle multiple questions
                for (String questionDomain : requestMessage.map.keySet()) {
                    // Create a new message for each question
                    DNSMessage singleQuestionMessage = new DNSMessage(requestMessage.id, requestMessage.flags, questionDomain);

                    // Forward the DNS query to the resolver
                    byte[] queryBuffer = singleQuestionMessage.array();
                    DatagramPacket queryPacket = new DatagramPacket(queryBuffer, queryBuffer.length,
                            new InetSocketAddress(resolverIP, resolverPort));
                    serverSocket.send(queryPacket);

                    // Receive the response from the resolver
                    final byte[] resolverBuf = new byte[512];
                    DatagramPacket resolverResponsePacket = new DatagramPacket(resolverBuf, resolverBuf.length);
                    serverSocket.receive(resolverResponsePacket);

                    // Parse the resolver's response and add it to the original response
                    DNSMessage responseMessage = new DNSMessage(resolverBuf);

                    // Merge the response back
                    requestMessage.addResponse(responseMessage);
                }

                // Send the response back to the client
                byte[] responseBuffer = requestMessage.array();
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBuffer, responseBuffer.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class DNSMessage {
    public short id;
    public short flags;
    public short qdcount;
    public short ancount;
    public short nscount;
    public short arcount;
    public Map<String, byte[]> map = new HashMap<>();

    public DNSMessage(byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        // Parse header section
        id = buffer.getShort();
        flags = buffer.getShort();
        qdcount = buffer.getShort();
        ancount = buffer.getShort();
        nscount = buffer.getShort();
        arcount = buffer.getShort();

        // Parse question section
        for (int i = 0; i < qdcount; i++) {
            map.put(decodeDomainName(buffer), new byte[]{8, 8, 8, 8});
            buffer.getShort(); // Type
            buffer.getShort(); // Class
        }

        // Parse answer section
        for (int i = 0; i < ancount; i++) {
            decodeDomainName(buffer); // Name
            buffer.getShort(); // Type
            buffer.getShort(); // Class
            buffer.getInt();   // TTL
            int rdlength = buffer.getShort(); // RDLENGTH
            byte[] rdata = new byte[rdlength];
            buffer.get(rdata);
            map.put("", rdata); // Add response data to the map
        }
    }

    public DNSMessage(short id, short flags, String questionDomain) {
        this.id = id;
        this.flags = flags;
        this.qdcount = 1;
        this.ancount = 0;
        this.nscount = 0;
        this.arcount = 0;
        this.map.put(questionDomain, new byte[]{8, 8, 8, 8});
    }

    public byte[] array() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        // Write header
        buffer.putShort(id);
        buffer.putShort(flags);
        buffer.putShort((short) map.size());
        buffer.putShort((short) map.size());
        buffer.putShort(nscount);
        buffer.putShort(arcount);

        // Write question section
        for (String domain : map.keySet()) {
            buffer.put(encodeDomainName(domain));
            buffer.putShort((short) 1); // Type = A
            buffer.putShort((short) 1); // Class = IN
        }

        // Write answer section
        for (String domain : map.keySet()) {
            buffer.put(encodeDomainName(domain));
            buffer.putShort((short) 1);   // Type = A
            buffer.putShort((short) 1);   // Class = IN
            buffer.putInt(60);            // TTL
            buffer.putShort((short) 4);   // Length
            buffer.put(map.get(domain));  // Data
        }
        return buffer.array();
    }

    public void addResponse(DNSMessage response) {
        map.putAll(response.map); // Merge the response into the current map
    }

    private byte[] encodeDomainName(String domain) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String label : domain.split("\\.")) {
            out.write(label.length());
            out.writeBytes(label.getBytes());
        }
        out.write(0); // Terminating null byte
        return out.toByteArray();
    }

    private String decodeDomainName(ByteBuffer buffer) {
        byte labelLength;
        StringJoiner labels = new StringJoiner(".");
        while ((labelLength = buffer.get()) != 0) {
            if ((labelLength & 0xC0) == 0xC0) {
                // It's a pointer, we won't handle compression here
                buffer.get(); // Skip the next byte
                break;
            } else {
                byte[] label = new byte[labelLength];
                buffer.get(label);
                labels.add(new String(label));
            }
        }
        return labels.toString();
    }
}

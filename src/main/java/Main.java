import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                DNSMessage requestMessage = DNSMessage.fromArray(buf);
                byte[] responseBuffer = requestMessage.createResponseArray();
                
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class DNSMessage {
    private short id;
    private short flags;
    private short qdcount;
    private short ancount;
    private short nscount;
    private short arcount;

    public DNSMessage(short id, short flags, short qdcount, short ancount, short nscount, short arcount) {
        this.id = id;
        this.flags = flags;
        this.qdcount = qdcount;
        this.ancount = ancount;
        this.nscount = nscount;
        this.arcount = arcount;
    }

    public static DNSMessage fromArray(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        short id = buffer.getShort();
        short flags = buffer.getShort();
        short qdcount = buffer.getShort();
        short ancount = buffer.getShort();
        short nscount = buffer.getShort();
        short arcount = buffer.getShort();

        // Extract the Opcode from the received flags (bits 1-4)
        short opcode = (short) (flags & 0x7800); // 0x7800 is the mask for Opcode bits
        short responseFlags;

        if (opcode != 0) {
            // If Opcode is not 0, return RCODE 4 (Not Implemented)
            responseFlags = (short) ((flags & 0x8000) | opcode | 0x0004);
        } else {
            // Mimic Opcode, set QR to 1, RA to 0, other flags as received
            responseFlags = (short) ((flags & 0x0110) | 0x8000);
        }
        return new DNSMessage(id, responseFlags, qdcount, ancount, nscount, arcount);
    }

    public byte[] createResponseArray() {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        // Write header
        buffer.putShort(id);
        buffer.putShort(flags);
        buffer.putShort(qdcount);
        buffer.putShort(ancount);
        buffer.putShort(nscount);
        buffer.putShort(arcount);

        // Write question (dummy data for testing purposes)
        buffer.put(encodeDomainName("codecrafters.io"));
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);

        // Write answer section (dummy data for testing purposes)
        buffer.put(encodeDomainName("codecrafters.io"));
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(60);
        buffer.putShort((short) 4);
        buffer.put(new byte[]{8, 8, 8, 8});

        return buffer.array();
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
}

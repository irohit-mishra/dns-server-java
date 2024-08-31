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

        // Extract the fields from the request packet
        short id = buffer.getShort();
        short flags = buffer.getShort();
        short qdcount = buffer.getShort();
        short ancount = buffer.getShort();
        short nscount = buffer.getShort();
        short arcount = buffer.getShort();

        // Prepare the response flags
        short opcode = (short) (flags & 0x7800); // Extract Opcode (bits 11-14)
        short rd = (short) (flags & 0x0100); // Extract Recursion Desired (bit 8)
        short rcode = (short) ((opcode == 0) ? 0 : 4); // Set RCODE: 0 if standard query, 4 if not implemented

        // Set the QR bit to 1 (indicating response), retain Opcode, RD, and set other fields to expected values
        short responseFlags = (short) (0x8000 | (opcode & 0x7800) | (rd & 0x0100) | rcode);

        return new DNSMessage(id, responseFlags, qdcount, ancount, nscount, arcount);
    }

    public byte[] createResponseArray() {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        // Write header
        buffer.putShort(id);        // ID
        buffer.putShort(flags);     // Flags (QR, Opcode, RD, RCODE, etc.)
        buffer.putShort(qdcount);   // QDCOUNT (Question Count)
        buffer.putShort(ancount);   // ANCOUNT (Answer Record Count)
        buffer.putShort(nscount);   // NSCOUNT (Authority Record Count)
        buffer.putShort(arcount);   // ARCOUNT (Additional Record Count)

        // Write a dummy question section for testing purposes
        buffer.put(encodeDomainName("codecrafters.io"));
        buffer.putShort((short) 1); // QTYPE (A)
        buffer.putShort((short) 1); // QCLASS (IN)

        // Write a dummy answer section for testing purposes
        buffer.put(encodeDomainName("codecrafters.io"));
        buffer.putShort((short) 1); // TYPE (A)
        buffer.putShort((short) 1); // CLASS (IN)
        buffer.putInt(60);          // TTL (Time to Live)
        buffer.putShort((short) 4); // RDLENGTH
        buffer.put(new byte[]{8, 8, 8, 8}); // RDATA (IP Address)

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

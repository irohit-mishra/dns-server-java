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
    private byte[] questionName;
    private short questionType;
    private short questionClass;

    public DNSMessage(short id, short flags, short qdcount, short ancount, short nscount, short arcount,
                      byte[] questionName, short questionType, short questionClass) {
        this.id = id;
        this.flags = flags;
        this.qdcount = qdcount;
        this.ancount = ancount;
        this.nscount = nscount;
        this.arcount = arcount;
        this.questionName = questionName;
        this.questionType = questionType;
        this.questionClass = questionClass;
    }

    public static DNSMessage fromArray(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Extract the header fields from the request packet
        short id = buffer.getShort();
        short flags = buffer.getShort();
        short qdcount = buffer.getShort();
        short ancount = buffer.getShort();
        short nscount = buffer.getShort();
        short arcount = buffer.getShort();

        // Parse the question section
        byte[] questionName = parseDomainName(buffer);
        short questionType = buffer.getShort();
        short questionClass = buffer.getShort();

        // Prepare the response flags
        short opcode = (short) (flags & 0x7800); // Extract Opcode (bits 11-14)
        short rd = (short) (flags & 0x0100); // Extract Recursion Desired (bit 8)
        short rcode = (short) ((opcode == 0) ? 0 : 4); // Set RCODE: 0 if standard query, 4 if not implemented

        // Set the QR bit to 1 (indicating response), retain Opcode, RD, and set other fields to expected values
        short responseFlags = (short) (0x8000 | (opcode & 0x7800) | (rd & 0x0100) | rcode);

        // Set ancount to 1 to indicate one answer record
        return new DNSMessage(id, responseFlags, qdcount, (short) 1, nscount, arcount, questionName, questionType, questionClass);
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

        // Write the question section (mimic the original request)
        buffer.put(questionName);
        buffer.putShort(questionType);
        buffer.putShort(questionClass);

        // Write the answer section
        buffer.put(questionName);   // Name
        buffer.putShort(questionType); // Type (A)
        buffer.putShort(questionClass); // Class (IN)
        buffer.putInt(60);          // TTL (Time to Live)
        buffer.putShort((short) 4); // RDLENGTH
        buffer.put(new byte[]{8, 8, 8, 8}); // RDATA (IP Address)

        return buffer.array();
    }

    private static byte[] parseDomainName(ByteBuffer buffer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            byte len = buffer.get();
            if (len == 0) {
                break;
            }
            byte[] label = new byte[len];
            buffer.get(label);
            out.write(len);
            out.write(label);
        }
        out.write(0); // Terminating null byte
        return out.toByteArray();
    }
}

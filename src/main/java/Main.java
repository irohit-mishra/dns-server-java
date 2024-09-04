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

        short id = buffer.getShort();
        short flags = buffer.getShort();
        short qdcount = buffer.getShort();
        short ancount = buffer.getShort();
        short nscount = buffer.getShort();
        short arcount = buffer.getShort();

        ByteArrayOutputStream questionBytes = new ByteArrayOutputStream();
        for (int i = 0; i < qdcount; i++) {
            byte[] questionName = parseDomainName(buffer);
            questionBytes.write(questionName);

            short questionType = buffer.getShort();
            questionBytes.write(ByteBuffer.allocate(2).putShort(questionType).array());

            short questionClass = buffer.getShort();
            questionBytes.write(ByteBuffer.allocate(2).putShort(questionClass).array());
        }

        return new DNSMessage(id, flags, qdcount, qdcount, nscount, arcount,
                questionBytes.toByteArray(), buffer.getShort(), buffer.getShort());
    }

    public byte[] createResponseArray() {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        buffer.putShort(id);
        buffer.putShort((short) (flags | 0x8000)); // Set response flag
        buffer.putShort(qdcount);
        buffer.putShort(qdcount); // Set ANCOUNT equal to QDCOUNT
        buffer.putShort(nscount);
        buffer.putShort(arcount);

        int offset = 0;
        for (int i = 0; i < qdcount; i++) {
            byte[] name = extractQuestionName(i);
            buffer.put(name);   // Name
            buffer.putShort(questionType); // Type (A)
            buffer.putShort(questionClass); // Class (IN)
            buffer.putInt(60);          // TTL (Time to Live)
            buffer.putShort((short) 4); // RDLENGTH
            buffer.put(new byte[]{8, 8, 8, 8}); // RDATA (IP Address)
            offset += name.length + 4 + 4 + 2 + 4 + 2 + 4;
        }

        return buffer.array();
    }

    private static byte[] parseDomainName(ByteBuffer buffer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            byte len = buffer.get();
            if ((len & 0xC0) == 0xC0) { // Handle compression
                int offset = ((len & 0x3F) << 8) | (buffer.get() & 0xFF);
                byte[] name = parseCompressedDomainName(buffer, offset);
                out.write(name);
                break;
            } else if (len == 0) {
                break;
            } else {
                byte[] label = new byte[len];
                buffer.get(label);
                out.write(len);
                out.write(label);
            }
        }
        out.write(0); // End with null byte
        return out.toByteArray();
    }

    private static byte[] parseCompressedDomainName(ByteBuffer buffer, int offset) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int originalPosition = buffer.position();
        buffer.position(offset);

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
        out.write(0); // End with null byte
        buffer.position(originalPosition);
        return out.toByteArray();
    }

    private byte[] extractQuestionName(int questionIndex) {
        // Logic to extract the question name for each question based on the index
        return questionName;
    }
}


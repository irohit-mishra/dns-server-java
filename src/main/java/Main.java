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

                byte[] responseBuffer = new DNSMessage().array();
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class DNSMessage {
    public short id = 1234;

    public short flags = (short) 0b10000000_00000000;

    public short qdcount = 1;
    public short ancount = 1;
    public short nscount = 0;
    public short arcount = 0;

    public DNSMessage() {}

    public byte[] array() {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        // Write header
        buffer.putShort(id);
        buffer.putShort(flags);
        buffer.putShort(qdcount);
        buffer.putShort(ancount);
        buffer.putShort(nscount);
        buffer.putShort(arcount);

        // Write question 
        buffer.put(encodeDomainName("codecrafters.io"));
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);

        // Write answer section
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

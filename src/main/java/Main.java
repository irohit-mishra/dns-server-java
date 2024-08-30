import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Prepare response buffer
                short ID = (short) 1234;
                final var bitset = new BitSet(8);
                bitset.flip(7);

                // Encode the domain name example.com
                byte[] name = new byte[] {
                    0x07,  // Length of "example"
                    'e', 'x', 'a', 'm', 'p', 'l', 'e',
                    0x03,  // Length of "com"
                    'c', 'o', 'm',
                    0x00   // Null byte to terminate the domain name
                };

                short qtype = 1;  // Type A
                short qclass = 1; // Class IN

                final byte[] bufResponse = ByteBuffer.allocate(512)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putShort(ID)                         // Transaction ID
                        .put(bitset.toByteArray())             // Flags
                        .putShort((short) 1)                  // QDCOUNT (number of questions)
                        .putShort((short) 0)                  // ANCOUNT
                        .putShort((short) 0)                  // NSCOUNT
                        .putShort((short) 0)                  // ARCOUNT
                        .put(name)                            // Question Name
                        .putShort(qtype)                      // Question Type
                        .putShort(qclass)                     // Question Class
                        .array();

                final DatagramPacket packetResponse = new DatagramPacket(bufResponse, bufResponse.length, packet.getSocketAddress());
                serverSocket.send(packetResponse);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

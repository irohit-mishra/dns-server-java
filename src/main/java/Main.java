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

                // Set the flags for response
                final var bitset = new BitSet(16);  // Should be 16 bits
                bitset.set(15);  // QR flag (response)

                // Convert bitset to 2 bytes
                byte[] flags = bitset.toByteArray();
                if (flags.length < 2) {
                    flags = new byte[]{0, flags[0]};
                }

                // Encode the domain name codecrafters.io
                byte[] name = new byte[] {
                    0x0C,  // Length of "codecrafters"
                    'c', 'o', 'd', 'e', 'c', 'r', 'a', 'f', 't', 'e', 'r', 's',
                    0x02,  // Length of "io"
                    'i', 'o',
                    0x00   // Null byte to terminate the domain name
                };

                short qtype = 1;  // Type A
                short qclass = 1; // Class IN

                final byte[] bufResponse = ByteBuffer.allocate(512)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putShort(ID)                          // Transaction ID
                        .put(flags)                            // Flags (2 bytes)
                        .putShort((short) 1)                   // QDCOUNT (number of questions)
                        .putShort((short) 0)                   // ANCOUNT
                        .putShort((short) 0)                   // NSCOUNT
                        .putShort((short) 0)                   // ARCOUNT
                        .put(name)                             // Question Name
                        .putShort(qtype)                       // Question Type
                        .putShort(qclass)                      // Question Class
                        .array();

                final DatagramPacket packetResponse = new DatagramPacket(bufResponse, bufResponse.length, packet.getSocketAddress());
                serverSocket.send(packetResponse);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

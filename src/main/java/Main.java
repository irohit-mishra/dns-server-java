import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
public class Main {
  public static void main(String[] args) {
    try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
      while (true) {
        final byte[] buf = new byte[512];
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
        serverSocket.receive(packet);
        System.out.println("Received data");
        // Request
        DNSMessage requestMessage = new DNSMessage(buf);
        // Response
        DNSMessage responseMessage = requestMessage;
        // Set flags
        char[] requestFlags =
            String.format("%16s", Integer.toBinaryString(responseMessage.flags))
                .replace(' ', '0')
                .toCharArray();
        requestFlags[0] = '1';  // QR
        requestFlags[13] = '1'; // RCODE
        responseMessage.flags =
            (short)Integer.parseInt(new String(requestFlags), 2);
        byte[] responseBuffer = responseMessage.array();
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
  public short id = 1234;
  public short flags;
  // public short qdcount;
  // public short ancount;
  public short nscount;
  public short arcount;
  public Map<String, byte[]> map = new HashMap<>();
  public DNSMessage(byte[] array) {
    ByteBuffer buffer = ByteBuffer.wrap(array);
    // Parse header section
    id = buffer.getShort();
    flags = buffer.getShort();
    int qdcount = buffer.getShort();
    buffer.getShort(); // ancount
    nscount = buffer.getShort();
    arcount = buffer.getShort();
    // Parse question section
    for (int i = 0; i < qdcount; i++) {
      map.put(decodeDomainName(buffer), new byte[] {8, 8, 8, 8});
      buffer.getShort(); // Type
      buffer.getShort(); // Class
    }
  }
  public byte[] array() {
    ByteBuffer buffer = ByteBuffer.allocate(512);
    // Write header
    buffer.putShort(id);
    buffer.putShort(flags);
    buffer.putShort((short)map.size());
    buffer.putShort((short)map.size());
    buffer.putShort(nscount);
    buffer.putShort(arcount);
    // Write question section
    for (String domain : map.keySet()) {
      buffer.put(encodeDomainName(domain));
      buffer.putShort((short)1); // Type = A
      buffer.putShort((short)1); // Class = IN
    }
    // Write answer section
    for (String domain : map.keySet()) {
      buffer.put(encodeDomainName(domain));
      buffer.putShort((short)1);   // Type = A
      buffer.putShort((short)1);   // Class = IN
      buffer.putInt(60);           // TTL
      buffer.putShort((short)4);   // Length
      buffer.put(map.get(domain)); // Data
    }
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
  private String decodeDomainName(ByteBuffer buffer) {
    byte labelLength;
    StringJoiner labels = new StringJoiner(".");
    boolean compressed = false;
    int position = 0;
    while ((labelLength = buffer.get()) != 0) {
      if ((labelLength & 0xC0) == 0xC0) {
        compressed = true;
        // It's a pointer. Create a new ByteBuffer from the current one to
        // handle jumps
        int offset = ((labelLength & 0x3F) << 8) | (buffer.get() & 0xFF);
        // Implement jumping logic
        position = buffer.position();
        buffer.position(offset);
      } else {
        byte[] label = new byte[labelLength];
        buffer.get(label);
        labels.add(new String(label));
      }
    }
    if (compressed) {
      buffer.position(position);
    }
    return labels.toString();
  }
}
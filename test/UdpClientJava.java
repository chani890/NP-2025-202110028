import java.net.*;

public class UdpClientJava {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();

        String msg = "Hello from Java";
        byte[] buf = msg.getBytes();

        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 2600);
        socket.send(packet);

        byte[] recvBuf = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
        socket.receive(recvPacket);

        String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
        System.out.println("Received: " + response);

        socket.close();
    }
}

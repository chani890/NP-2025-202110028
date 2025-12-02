import java.net.*;
import java.util.Scanner;

public class UdpChatClient {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("localhost");
        int port = 2600;

        Scanner sc = new Scanner(System.in);

        System.out.println("UDP Chat Client Started. Type messages.");

        while (true) {
            System.out.print("You: ");
            String msg = sc.nextLine();

            if (msg.equalsIgnoreCase("exit")) break;

            byte[] sendBuf = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
            socket.send(sendPacket);

            // Receive echo
            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(recvPacket);

            String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
            System.out.println("Server: " + response);
        }

        socket.close();
    }
}

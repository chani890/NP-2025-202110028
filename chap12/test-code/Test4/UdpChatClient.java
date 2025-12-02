import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UdpChatClient {
    private String host;
    private int port;
    private int bufSize;
    private String nickname;

    public UdpChatClient(String host, int port, int bufSize, String nickname) {
        this.host = host;
        this.port = port;
        this.bufSize = bufSize;
        this.nickname = nickname;
    }

    public void start() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddr = InetAddress.getByName(host);

        // send join message
        String joinMsg = nickname + " joined the chat";
        byte[] join = joinMsg.getBytes("UTF-8");
        socket.send(new DatagramPacket(join, join.length, serverAddr, port));

        // receive thread
        Thread recvThread = new Thread(() -> {
            byte[] buf = new byte[bufSize];
            try {
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength(), "UTF-8");
                    System.out.println("\n[MSG] " + msg);
                    System.out.print("> ");
                }
            } catch (Exception ignored) {}
        });

        recvThread.setDaemon(true);
        recvThread.start();

        // send loop
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine();

            if (line.equalsIgnoreCase("exit")) {
                String byeMsg = nickname + " left the chat";
                byte[] bye = byeMsg.getBytes("UTF-8");
                socket.send(new DatagramPacket(bye, bye.length, serverAddr, port));
                break;
            }

            String fullMsg = nickname + ": " + line;
            byte[] out = fullMsg.getBytes("UTF-8");
            socket.send(new DatagramPacket(out, out.length, serverAddr, port));
        }

        socket.close();
        sc.close();
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nickname: ");
        String nick = sc.nextLine();

        UdpChatClient client = new UdpChatClient("localhost", 2600, 1024, nick);
        client.start();
    }
}

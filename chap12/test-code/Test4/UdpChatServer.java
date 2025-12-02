import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class UdpChatServer {
    private int port;
    private int bufSize;
    private Set<ClientAddr> clients = new HashSet<>();

    public UdpChatServer(int port, int bufSize) {
        this.port = port;
        this.bufSize = bufSize;
    }

    public void start() throws Exception {
        byte[] buf = new byte[bufSize];

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[SERVER] Java UDP Chat Server started (*:" + port + ")");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                ClientAddr sender = new ClientAddr(packet.getAddress(), packet.getPort());

                if (!clients.contains(sender)) {
                    clients.add(sender);
                    System.out.println("[JOIN] " + sender + " joined, users: " + clients.size());
                }

                System.out.println("[RECV] " + sender + " : " + msg);

                // broadcast message to all other clients
                String toSend = sender + " : " + msg;
                byte[] out = toSend.getBytes("UTF-8");
                for (ClientAddr c : clients) {
                    if (!c.equals(sender)) {
                        DatagramPacket outPacket =
                                new DatagramPacket(out, out.length, c.addr, c.port);
                        socket.send(outPacket);
                    }
                }
            }
        }
    }

    private static class ClientAddr {
        InetAddress addr;
        int port;

        ClientAddr(InetAddress addr, int port) {
            this.addr = addr;
            this.port = port;
        }

        @Override
        public int hashCode() {
            return addr.hashCode() * 31 + port;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ClientAddr)) return false;
            ClientAddr other = (ClientAddr) o;
            return addr.equals(other.addr) && port == other.port;
        }

        @Override
        public String toString() {
            return addr.getHostAddress() + ":" + port;
        }
    }

    public static void main(String[] args) throws Exception {
        UdpChatServer server = new UdpChatServer(2600, 1024);
        server.start();
    }
}

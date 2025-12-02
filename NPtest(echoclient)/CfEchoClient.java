import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CfEchoClient {
    public static void main(String[] args) {
        String host = "localhost";  // Mac 입장에서 localhost -> Docker 포트 매핑
        int port = 2500;

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(
                    new InputStreamReader(System.in));
        ) {
            System.out.println("[연결됨] " + host + ":" + port);

            String line;
            while (true) {
                System.out.print("나 (exit 종료): ");
                line = userInput.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) {
                    System.out.println("[종료]");
                    break;
                }
                out.println(line);
                String resp = in.readLine();
                if (resp == null) break;
                System.out.println("서버: " + resp);
            }
        } catch (IOException e) {
            System.err.println("[오류] " + e.getMessage());
        }
    }
}

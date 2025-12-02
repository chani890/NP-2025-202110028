# udp_chat_client_oop.py
import socket
import threading

class UdpChatClient:
    def __init__(self, host="localhost", port=2600, bufsize=1024, nickname="Guest"):
        self.server_addr = (host, port)
        self.bufsize = bufsize
        self.nickname = nickname
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def recv_loop(self):
        while True:
            data, _ = self.sock.recvfrom(self.bufsize)
            print("\n[MSG]", data.decode("utf-8", errors="replace"))
            print("> ", end="", flush=True)

    def start(self):
        # 서버에 입장 메시지
        self.sock.sendto(f"{self.nickname} 님 입장".encode("utf-8"), self.server_addr)

        t = threading.Thread(target=self.recv_loop, daemon=True)
        t.start()

        while True:
            msg = input("> ")
            if msg.lower() == "exit":
                self.sock.sendto(f"{self.nickname} 님 퇴장".encode("utf-8"), self.server_addr)
                break
            self.sock.sendto(f"{self.nickname}: {msg}".encode("utf-8"), self.server_addr)

if __name__ == "__main__":
    nick = input("닉네임: ")
    client = UdpChatClient(nickname=nick)
    client.start()

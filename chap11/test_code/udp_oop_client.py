import socket

class UDPClient:
    def __init__(self, host: str = "localhost", port: int = 2600, bufsize: int = 1024):
        self.host = host
        self.port = port
        self.BUFSIZE = bufsize
        self.server_addr = (self.host, self.port)
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # 선택: 너무 오래 안 기다리게 타임아웃
        self.sock.settimeout(5.0)

    def run(self):
        while True:
            msg = input("Message (exit to quit): ")
            if msg.lower() == "exit":
                break
            self.sock.sendto(msg.encode(), self.server_addr)
            data, _ = self.sock.recvfrom(self.BUFSIZE)
            print("Echo:", data.decode("utf-8", errors="replace"))

if __name__ == "__main__":
    UDPClient().run()

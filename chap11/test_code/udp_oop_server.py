import socket

class UDPServer:
    def __init__(self, host: str = "", port: int = 2600, bufsize: int = 1024):
        self.host = host      # '' = 모든 인터페이스
        self.port = port
        self.BUFSIZE = bufsize
        self.addr = (self.host, self.port)
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(self.addr)

    def run(self):
        print(f"[UDP_SERVER] listening on *:{self.port}")
        while True:
            data, client_addr = self.sock.recvfrom(self.BUFSIZE)
            msg = data.decode("utf-8", errors="replace")
            print(f"[UDP_SERVER] from {client_addr} : {msg}")
            # ✅ 꼭 응답 보내기 (에코)
            self.sock.sendto(data, client_addr)

if __name__ == "__main__":
    UDPServer().run()

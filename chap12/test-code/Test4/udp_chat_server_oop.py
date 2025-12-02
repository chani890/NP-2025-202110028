# udp_chat_server_oop.py
import socket
from typing import Set, Tuple

Addr = Tuple[str, int]

class UdpChatServer:
    def __init__(self, host: str = "", port: int = 2600, bufsize: int = 1024):
        self.host = host
        self.port = port
        self.bufsize = bufsize
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.clients: Set[Addr] = set()

    def start(self):
        self.sock.bind((self.host, self.port))
        print(f"[SERVER] UDP 객체지향 채팅 서버 시작 (*:{self.port})")
        self.run()

    def register_client(self, addr: Addr):
        if addr not in self.clients:
            self.clients.add(addr)
            print(f"[JOIN] {addr} 접속, 현재 인원: {len(self.clients)}")

    def broadcast(self, msg: str, sender: Addr):
        data = msg.encode("utf-8")
        for c in self.clients:
            if c != sender:
                self.sock.sendto(data, c)

    def run(self):
        while True:
            data, addr = self.sock.recvfrom(self.bufsize)
            msg = data.decode("utf-8", errors="replace")
            self.register_client(addr)
            print(f"[RECV] {addr} : {msg}")
            self.broadcast(f"{addr}: {msg}", addr)

if __name__ == "__main__":
    server = UdpChatServer()
    server.start()

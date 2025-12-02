from __future__ import annotations

import socket
import threading


class ChatbotTCPServer:
    def __init__(self, host: str, port: int, handler_factory) -> None:
        self.host = host
        self.port = port
        self.handler_factory = handler_factory

    def start(self) -> None:
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind((self.host, self.port))
        server.listen()
        print(f" TCP 여행 플래너 서버 실행 중: {self.host}:{self.port}")
        try:
            while True:
                conn, addr = server.accept()
                thread = threading.Thread(
                    target=self._handle_client,
                    args=(conn, addr),
                    daemon=True,
                )
                thread.start()
        finally:
            server.close()

    def _handle_client(self, conn, addr):
        handler = self.handler_factory()
        handler.handle(conn, addr)


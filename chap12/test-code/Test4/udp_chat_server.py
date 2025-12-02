# udp_chat_server.py
import socket

HOST = ""          # 모든 인터페이스
PORT = 2600        # 네가 쓰던 UDP 포트 그대로 써도 됨
BUF = 1024

# 접속 클라이언트 주소 목록 (set으로 중복 방지)
clients = set()

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.bind((HOST, PORT))
        print(f"[SERVER] UDP 채팅 서버 시작 (*:{PORT})")

        while True:
            data, addr = s.recvfrom(BUF)
            msg = data.decode("utf-8", errors="replace")

            # 새 클라이언트면 리스트에 추가
            if addr not in clients:
                clients.add(addr)
                print(f"[JOIN] {addr} 접속, 현재 인원: {len(clients)}")

            print(f"[RECV] {addr} : {msg}")

            # broadcast (보낸 애 포함/제외는 취향)
            for c in clients:
                if c != addr:  # 보낸 애 제외하고 싶으면 이 줄 유지, 포함하려면 if 지워
                    s.sendto(f"{addr}: {msg}".encode("utf-8"), c)

if __name__ == "__main__":
    main()

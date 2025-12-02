# udp_chat_client.py
import socket
import threading

SERVER_HOST = "localhost"   # Mac에서 실행하면 localhost -> 도커 서버로 포워딩
SERVER_PORT = 2600
BUF = 1024

def recv_loop(sock):
    while True:
        data, _ = sock.recvfrom(BUF)
        print("\n[MSG]", data.decode("utf-8", errors="replace"))
        print("> ", end="", flush=True)

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        # 서버에게 첫 인사 보내서 자신의 주소 등록
        nickname = input("닉네임 입력: ")
        s.sendto(f"{nickname} 님 입장".encode("utf-8"), (SERVER_HOST, SERVER_PORT))

        # 수신 스레드 시작
        t = threading.Thread(target=recv_loop, args=(s,), daemon=True)
        t.start()

        while True:
            msg = input("> ")
            if msg.lower() == "exit":
                s.sendto(f"{nickname} 님 퇴장".encode("utf-8"), (SERVER_HOST, SERVER_PORT))
                break
            s.sendto(f"{nickname}: {msg}".encode("utf-8"), (SERVER_HOST, SERVER_PORT))

if __name__ == "__main__":
    main()

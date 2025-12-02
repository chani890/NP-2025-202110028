# client_chat.py
from socket import *

HOST = 'localhost'  # 혹은 서버 IP
PORT = 2500
BUFSIZE = 1024

def main():
    with socket(AF_INET, SOCK_STREAM) as sock:
        try:
            sock.connect((HOST, PORT))
            print(f"[서버 연결됨] {HOST}:{PORT}")

            while True:
                msg = input("나: ")
                if msg.lower() == "exit":
                    print("[채팅 종료]")
                    break

                # 서버로 메시지 전송
                sock.send(msg.encode('utf-8'))

                # 서버에서 broadcast된 메시지 수신
                data = sock.recv(BUFSIZE)
                if not data:
                    print("[서버 연결 종료]")
                    break

                print("서버/다른 이용자:", data.decode('utf-8').strip())

        except Exception as e:
            print("[오류 발생]", e)

if __name__ == "__main__":
    main()

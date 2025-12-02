# threading_server.py
import socket
import threading

HOST = ""         # '' 또는 '0.0.0.0' -> 모든 인터페이스에서 접속 허용
PORT = 2500
BUFSIZE = 1024

# 서버와 연결된 클라이언트 소켓들을 저장할 리스트
connections = []
lock = threading.Lock()  # 리스트 동시접근 보호용

def handler(csock, addr):
    """각 클라이언트마다 실행되는 스레드 함수"""
    global connections
    print(f"[입장] {addr} 접속")

    while True:
        try:
            data = csock.recv(BUFSIZE)
        except ConnectionResetError:
            # 클라이언트가 강제로 끊었을 때
            break

        if not data:
            # 빈 데이터면 연결 종료로 판단
            break

        msg = data.decode('utf-8', errors='replace')
        print(f"[{addr}] {msg}")

        # ---- 모든 클라이언트에게 broadcast ----
        with lock:
            for conn in connections:
                try:
                    # 보낸 사람에게도 다시 보내고 싶으면 조건 제거하면 됨
                    conn.sendall(data)
                except:
                    # 전송 중 에러 나면 무시 (간단 처리)
                    pass

    # 여기까지 왔으면 이 클라이언트는 나간 것
    with lock:
        if csock in connections:
            connections.remove(csock)
    csock.close()
    print(f"[퇴장] {addr} 연결 종료")


def main():
    global connections

    # TCP 소켓 생성
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # 서버 재시작 시 'Address already in use' 방지
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((HOST, PORT))
    sock.listen()  # 기본 큐 사이즈 사용

    print(f"[서버 시작] 포트 {PORT} 에서 대기 중...")

    while True:
        csock, addr = sock.accept()
        print(f"[연결 수락] {addr}")

        # 새 클라이언트를 목록에 추가
        with lock:
            connections.append(csock)
            print(f"[현재 접속자 수] {len(connections)}")

        # 서브 스레드 생성 및 시작
        cThread = threading.Thread(target=handler, args=(csock, addr), daemon=True)
        cThread.start()


if __name__ == "__main__":
    main()

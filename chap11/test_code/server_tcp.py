# server_tcp.py  (TCP Echo Server)
import socket

HOST = "0.0.0.0"     # 도커 외부(맥) 접속 허용
PORT = 2500          # 도커 -p로 매핑한 포트
BUFSIZE = 1024

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen(1)
    print(f"[TCP-SERVER] listening on {HOST}:{PORT}")
    conn, addr = s.accept()
    print("[TCP-SERVER] connected:", addr)
    with conn:
        while True:
            data = conn.recv(BUFSIZE)
            if not data:
                break
            print("[TCP-SERVER] received:", data.decode())
            conn.sendall(data)
# server_udp.py
import socket

HOST = ""           # 모든 인터페이스 허용
PORT = 2600         # Docker run 시 -p 2600:2600 으로 매핑되어 있음
BUF  = 1024

# UDP 소켓 생성
with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
    s.bind((HOST, PORT))
    print(f"[UDP-SERVER] listening on *:{PORT}")
    while True:
        data, addr = s.recvfrom(BUF)
        print(f"[UDP-SERVER] from {addr} : {data.decode('utf-8', errors='replace')}")
        s.sendto(data, addr)  # 그대로 에코
# server_udp.py
import socket

HOST = ""      
PORT = 2600
BUF = 1024

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
    s.bind((HOST, PORT))
    print(f"[UDP-SERVER] listening on *:{PORT}")

    while True:
        data, addr = s.recvfrom(BUF)
        msg = data.decode('utf-8', errors='replace')
        print(f"[UDP-SERVER] from {addr}: {msg}")

        # 메시지 끝에 \n 추가 (Java 읽기 쉽게)
        response = (msg + "\n").encode('utf-8')
        s.sendto(response, addr)

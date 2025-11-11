# client_udp.py
import socket

HOST = "localhost"  # Docker 포트 매핑으로 컨테이너 2600에 도달
PORT = 2600
BUF  = 1024

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
    while True:
        msg = input("Message (exit to quit): ")
        if msg.lower() == "exit":
            break
        s.sendto(msg.encode(), (HOST, PORT))
        data, _ = s.recvfrom(BUF)
        print("Echo:", data.decode('utf-8', errors='replace'))

# client_tcp.py  (TCP Echo Client)
import socket

HOST = "localhost"   # -p 매핑 덕분에 맥에서 localhost로 접속
PORT = 2500
BUFSIZE = 1024

with socket.create_connection((HOST, PORT)) as sock:
    print(f"[TCP-CLIENT] connected to {HOST}:{PORT}")
    while True:
        msg = input("Message (exit to quit): ")
        if msg.lower() == "exit":
            break
        sock.sendall(msg.encode())
        data = sock.recv(BUFSIZE)
        print("Echo:", data.decode())

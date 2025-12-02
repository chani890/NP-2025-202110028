import socket

HOST = "localhost"
PORT = 2500
BUF = 1024

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    print("[클라이언트 연결됨]")

    while True:
        msg = input("나: ")
        if msg.lower() == "exit":
            break
        s.sendall(msg.encode())
        data = s.recv(BUF)
        print("서버:", data.decode())

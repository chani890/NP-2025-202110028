import socket

port = 2500
BUFFSIZE = 1024
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(("localhost", port))

while True:
    msg = input("Message to send: ")
    s.send(msg.encode())
    data = s.recv(BUFFSIZE)
    print("Received message:", data.decode())

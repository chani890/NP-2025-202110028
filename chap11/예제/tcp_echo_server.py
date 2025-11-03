import socket
port = 2500
BUFSIZE = 1024

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(('0.0.0.0', port))
sock.listen(1)
print("Echo server ready...")

conn, addr = sock.accept()
print("Connected by", addr)
while True:
    data = conn.recv(BUFSIZE)
    if not data:
        break
    print("Received:", data.decode())
    conn.send(data)
conn.close()

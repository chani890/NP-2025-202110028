import socket, time

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind(('0.0.0.0', 2500))   # 모든 IP에서 접속 허용
s.listen(5)
print("Time server started...")

while True:
    client, addr = s.accept()
    print("Connection from", addr)
    client.send(time.asctime().encode())
    client.close()

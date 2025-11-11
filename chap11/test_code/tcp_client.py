import socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(("localhost", 2500))
while True:
    msg = input("Message to send: ")
    if msg == "quit":
        break
    sock.send(msg.encode())
    data = sock.recv(1024)
    print("Echoed:", data.decode())
sock.close()

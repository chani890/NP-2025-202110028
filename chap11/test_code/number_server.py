import socket

table = {'1':'one', '2':'two', '3':'three', '4':'four',
         '5':'five', '6':'six', '7':'seven', '8':'eight',
         '9':'nine', '10':'ten'}

s = socket.socket()
s.bind(('0.0.0.0', 2550))
s.listen(1)
print("Number server running...")

client, addr = s.accept()
print("Connection from", addr)
while True:
    data = client.recv(1024).decode()
    if not data:
        break
    resp = table.get(data, "Try again")
    client.send(resp.encode())
client.close()

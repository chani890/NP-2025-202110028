from socket import *
import struct

BUFFER = 1024
group_addr = "224.0.0.255"
port = 5005

r_sock = socket(AF_INET, SOCK_DGRAM)
r_sock.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
r_sock.bind(("", port))

mreq = struct.pack("4sl", inet_aton(group_addr), INADDR_ANY)
r_sock.setsockopt(IPPROTO_IP, IP_ADD_MEMBERSHIP, mreq)

print(f"[Receiver] 멀티캐스트 그룹 {group_addr}:{port} 에 가입 완료")
print("[Receiver] 수신 대기 중... (Ctrl+C로 종료)")

while True:
    rmsg, addr = r_sock.recvfrom(BUFFER)
    print(f"[Receiver] {addr} 로부터 수신: {rmsg.decode()}")
    r_sock.sendto("ACK".encode(), addr)
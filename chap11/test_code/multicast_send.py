from socket import *
import struct

group_addr = ("224.0.0.255", 5005)
BUFFER = 1024

s_sock = socket(AF_INET, SOCK_DGRAM)
s_sock.settimeout(0.5)
TTL = struct.pack('@i', 5)  # TTL 증가 (더 많은 홉을 통과)
s_sock.setsockopt(IPPROTO_IP, IP_MULTICAST_TTL, TTL)
s_sock.setsockopt(IPPROTO_IP, IP_MULTICAST_LOOP, True)  # 루프백 활성화 (로컬 테스트용)

print(f"[Sender] 멀티캐스트 그룹 {group_addr[0]}:{group_addr[1]} 로 송신 시작")
while True:
    rmsg = input("Your message (exit to quit): ")
    if rmsg.lower() == "exit":
        print("종료합니다.")
        break
    s_sock.sendto(rmsg.encode(), group_addr)
    print(f"[Sender] Sent: {rmsg}")
    while True:
        try:
            response, addr = s_sock.recvfrom(BUFFER)
        except timeout:
            break
        else:
            print(f"[Sender] {addr} 로부터 응답: {response.decode()}")
s_sock.close()
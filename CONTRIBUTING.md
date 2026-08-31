# Adding a new lab

This repo favors a small number of labs that are fully real (genuine
root-level RCE or equivalent proof, no simulated output) over a large number
of shallow ones. If you want breadth instead — reproducing dozens of CVEs
quickly — [vulhub/vulhub](https://github.com/vulhub/vulhub) already does that
well; `vulhub-fetch/` in this repo can pull a single lab out of it on demand.
Add a lab here when you want it hand-built, understood end-to-end, and tied
back into the knowledge base.

## Directory layout

```
labs/<lab-name>/
├── Dockerfile              # single-container labs (e.g. shellshock, heartbleed)
└── exploit.sh / *.py       # the actual exploit, if it's more than one curl command
```

or, for labs needing separate attacker/victim containers:

```
labs/<lab-name>/
├── docker-compose.yml
├── victim/
│   └── Dockerfile (+ source, if the victim app is hand-written)
└── attacker/
    └── Dockerfile (+ exploit source)
```

`labs/deser-classics/` shows the pattern for a group of related labs that
share one `docker-compose.yml` and an attacker container (`cc-gadget/`,
`cc-victim/`, `fastjson-victim/`, `shiro-victim/` all plug into it).

## Dockerfile conventions

- **Fetch dependencies at build time, don't vendor them.** `curl`/`wget` the
  jar, the source tarball, whatever — inside a `RUN` step. Nothing binary
  gets committed to the repo (see `.gitignore`); this is what keeps the whole
  repo under 1MB despite six labs.
- **Pin to the actual vulnerable version, and say why in a comment** if the
  choice wasn't obvious. E.g. `fastjson-victim/Dockerfile` pins `1.2.24`
  specifically because that's the version the classic AutoType RCE technique
  targets — a newer patched version wouldn't be exploitable, an unrelated
  older one wouldn't be historically accurate.
- **Base image + package snapshot timestamps need to be internally
  consistent.** If you pin an old Debian snapshot for the vulnerable
  component, make sure it's not older than what the base Docker Hub image
  already has baked in, or `apt-get` will hit version-skew failures trying to
  downgrade. Prefer bumping the snapshot forward over trying to force a
  downgrade.
- **JDK version matters for Java labs.** `trustURLCodebase` defaults to
  `false` in JDK ≥ 8u191 (breaks classic JNDI/RMI-codebase exploits), and the
  Commons Collections 1 gadget chain was patched in the JDK's
  `AnnotationInvocationHandler` after 8u71 (use CC6 instead, or pin the JDK
  below that patch level and say so).

## RUNBOOK.md section

Add a numbered section following the existing format:

```markdown
## N. <Lab Name> (CVE-XXXX-XXXXX, if there is one)

**代码位置**：`/root/labs/<lab-name>/`

\`\`\`bash
docker run -d --name <lab>-victim -p <port>:<port> <image-name>
\`\`\`

攻击：

\`\`\`bash
<the actual exploit command(s)>
\`\`\`

（brief note on any non-obvious step — a quirk you hit and fixed, why a
particular payload shape is needed, etc.)

关闭：`docker rm -f <lab>-victim [<lab>-attacker ...]`
```

Also add the new container names to the bulk cleanup command near the bottom
of RUNBOOK.md.

## Knowledge base card

If the lab corresponds to a well-known CVE or vulnerability class, consider
adding a card for it in `knowledge-base/offsec-kb.html`'s `DATA` array
(domain `"经典漏洞案例"` for named CVEs, or the relevant vulnerability-type
domain otherwise). Two things every existing case-study card does that a new
one should too:

- **State the exam relevance honestly** in the `EXAM_SCOPE` lookup (`core` /
  `maybe` / `none` — see the comment above that constant for what each means)
  and add the card's `id` to the right `ROADMAP` phase's `cards` array. Both
  are checked structurally (every non-cert card must appear in `ROADMAP`
  exactly once) — the page will silently misbehave if you skip this.
- **Point back to the lab** in the card's `intro`, the way `case-log4shell`
  does: *"本知识库同时提供了可以真实复现的 Docker 靶场(见 RUNBOOK.md)"*.

## Before opening a PR

- [ ] The exploit actually produces real proof (a root shell, `id` output,
      leaked memory, etc.) — not a canned/simulated response.
- [ ] `docker-compose up` / `docker run` works from a clean pull (no
      leftover local image cache masking a broken Dockerfile).
- [ ] No binaries, jars, or `.tar.gz` files committed — check
      `.gitignore` covers what your build produces.
- [ ] RUNBOOK.md has a section, and the teardown command list is updated.
- [ ] If you added a knowledge base card, the browser console has no errors
      after loading the page, and the `ROADMAP` coverage is intact (every
      card id appears in exactly one phase — easiest to check by pasting the
      invariant check into the browser console; ask if you want the snippet).

## Scope

Everything here is for authorized testing, CTF practice, and certification
study against machines you control. Don't add anything whose only purpose is
attacking systems you don't have permission to touch.

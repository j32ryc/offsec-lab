#!/usr/bin/env python3
"""
Fetch a single vulnerability lab from vulhub/vulhub (github.com/vulhub/vulhub)
without cloning the entire upstream repository -- it's a few hundred CVEs and
you almost always only want one of them.

Usage:
  python3 fetch.py --list                 list every available lab (path = "<product>/<CVE-or-name>")
  python3 fetch.py --search log4j         filter that list by keyword
  python3 fetch.py log4j/CVE-2021-44228   download that lab's files into ./downloaded/<path>/
  python3 fetch.py --refresh              force re-fetch of the lab listing (it's cached after first run)

Talks to GitHub's public REST API directly -- no git clone, no extra
dependencies. Unauthenticated requests are capped at 60/hour, which is fine
for fetching a lab or two in a sitting. If you want more headroom, copy
.env.example to .env and drop in a GitHub personal access token (needs no
scopes -- it's just for the higher authenticated rate limit).
"""
import json
import os
import sys
import urllib.request
import urllib.error

REPO = "vulhub/vulhub"
API = "https://api.github.com"
HERE = os.path.dirname(os.path.abspath(__file__))
CACHE_PATH = os.path.join(HERE, ".tree_cache.json")
OUT_DIR = os.path.join(HERE, "downloaded")
ENV_PATH = os.path.join(HERE, ".env")


def load_token():
    tok = os.environ.get("GITHUB_TOKEN")
    if tok:
        return tok
    if os.path.exists(ENV_PATH):
        with open(ENV_PATH, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("GITHUB_TOKEN="):
                    v = line.split("=", 1)[1].strip()
                    if v and v != "your-token-here":
                        return v
    return None


def _headers(raw=False):
    h = {"User-Agent": "offsec-lab-vulhub-fetch", "Accept": "application/vnd.github+json"}
    if raw:
        h["Accept"] = "application/vnd.github.raw"
    tok = load_token()
    if tok:
        h["Authorization"] = f"Bearer {tok}"
    return h


def api_get(path, raw=False):
    req = urllib.request.Request(f"{API}{path}", headers=_headers(raw))
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read()
            return data if raw else json.loads(data)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="ignore")
        hint = ""
        if e.code == 403 and "rate limit" in detail.lower():
            hint = "\n    -> hit the unauthenticated rate limit (60/hr); add a GITHUB_TOKEN to .env for 5000/hr."
        raise RuntimeError(f"GitHub API error {e.code} for {path}: {detail[:300]}{hint}")


def default_branch():
    return api_get(f"/repos/{REPO}")["default_branch"]


def fetch_tree(force=False):
    if not force and os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            return json.load(f)
    branch = default_branch()
    print(f"[+] fetching vulhub/vulhub file listing (branch: {branch})...", file=sys.stderr)
    tree = api_get(f"/repos/{REPO}/git/trees/{branch}?recursive=1")
    if tree.get("truncated"):
        print("[!] warning: GitHub truncated this listing (repo is large) -- "
              "some labs may be missing below. Known paths still work directly.", file=sys.stderr)
    entries = tree["tree"]
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(entries, f)
    return entries


def list_labs(entries):
    return sorted({
        os.path.dirname(e["path"]).replace("\\", "/")
        for e in entries
        if e["type"] == "blob" and os.path.basename(e["path"]) == "docker-compose.yml"
    })


def download_lab(path, entries, branch):
    prefix = path.rstrip("/") + "/"
    matches = [e for e in entries if e["type"] == "blob" and (e["path"] == path or e["path"].startswith(prefix))]
    if not matches:
        print(f"[!] no files found under '{path}' in {REPO} -- check the exact path with "
              f"--list or --search first.", file=sys.stderr)
        sys.exit(1)
    for e in matches:
        content = api_get(f"/repos/{REPO}/contents/{e['path']}?ref={branch}", raw=True)
        out_path = os.path.join(OUT_DIR, e["path"])
        os.makedirs(os.path.dirname(out_path), exist_ok=True)
        with open(out_path, "wb") as out:
            out.write(content)
        print(f"  + {e['path']} ({len(content)} bytes)")
    dest = os.path.normpath(os.path.join(OUT_DIR, path))
    print(f"\n[+] done -> {dest}")
    print(f'    cd "{dest}" && docker-compose up -d')


def main():
    args = sys.argv[1:]
    if not args or args[0] in ("-h", "--help"):
        print(__doc__)
        return
    if args[0] == "--refresh":
        fetch_tree(force=True)
        print("[+] listing cache refreshed", file=sys.stderr)
        return

    entries = fetch_tree()

    if args[0] == "--list":
        for lab in list_labs(entries):
            print(lab)
        return
    if args[0] == "--search":
        if len(args) < 2:
            print("usage: fetch.py --search <keyword>", file=sys.stderr)
            sys.exit(1)
        kw = args[1].lower()
        hits = [lab for lab in list_labs(entries) if kw in lab.lower()]
        if not hits:
            print(f"(no matches for '{args[1]}')", file=sys.stderr)
        for lab in hits:
            print(lab)
        return

    path = args[0].strip("/")
    branch = default_branch()
    download_lab(path, entries, branch)


if __name__ == "__main__":
    main()

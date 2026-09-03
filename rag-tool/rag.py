#!/usr/bin/env python3
"""
Local RAG assistant over the OffSec knowledge base.

Retrieval is deliberately simple (weighted keyword/substring scoring, no
embeddings or vector DB) -- the corpus is ~100 cards, so exhaustive scoring
over all of them is both fast and, at this scale, about as accurate as an
embedding-based retriever would be. Generation calls DeepSeek's
OpenAI-compatible chat completions API.

Setup:
  1. node extract_corpus.js        (regenerates corpus.json from the HTML)
  2. cp .env.example .env && edit in your DEEPSEEK_API_KEY
  3. python3 rag.py
"""
import json
import os
import re
import sys
import urllib.request
import urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
CORPUS_PATH = os.path.join(HERE, "corpus.json")
ENV_PATH = os.path.join(HERE, ".env")
TOP_K = 4
DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
DEEPSEEK_MODEL = "deepseek-chat"


def load_env():
    key = os.environ.get("DEEPSEEK_API_KEY")
    if key:
        return key
    if os.path.exists(ENV_PATH):
        with open(ENV_PATH, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("DEEPSEEK_API_KEY="):
                    key = line.split("=", 1)[1].strip()
                    if key and key != "your-key-here":
                        return key
    return None


def load_corpus():
    if not os.path.exists(CORPUS_PATH):
        print(f"[!] {CORPUS_PATH} not found -- run: node extract_corpus.js", file=sys.stderr)
        sys.exit(1)
    with open(CORPUS_PATH, encoding="utf-8") as f:
        return json.load(f)


TOKEN_RE = re.compile(r"[a-zA-Z0-9]+|[一-鿿]")


def tokenize(text):
    return TOKEN_RE.findall(text.lower())


def score_card(query_tokens, query_lower, card):
    def field_score(text, weight):
        text_l = (text or "").lower()
        s = 0
        if query_lower and query_lower in text_l:
            s += weight * 3
        for t in query_tokens:
            if t in text_l:
                s += weight
        return s

    total = 0
    total += field_score(f"{card['title']} {card['en']}", 5)
    total += field_score(" ".join(card.get("tags", [])), 4)
    total += field_score(card.get("domain", ""), 3)
    total += field_score(card.get("summary", ""), 2)
    body = f"{card.get('intro','')} {card.get('variants','')} {card.get('detect','')} {card.get('defend','')}"
    total += field_score(body, 1)
    return total


def retrieve(query, corpus, k=TOP_K):
    q_lower = query.lower()
    q_tokens = tokenize(query)
    scored = [(score_card(q_tokens, q_lower, c), c) for c in corpus]
    scored = [x for x in scored if x[0] > 0]
    scored.sort(key=lambda x: -x[0])
    return [c for _, c in scored[:k]]


def build_context(cards):
    parts = []
    for c in cards:
        parts.append(
            f"### {c['title']} ({c['en']}) [{c['domain']}]\n"
            f"概述: {c['summary']}\n"
            f"详情: {c['intro']}\n"
            f"类型: {c['variants']}\n"
            f"检测: {c['detect']}\n"
            f"防御: {c['defend']}\n"
        )
    return "\n".join(parts)


def call_deepseek(api_key, question, context, history):
    system_msg = (
        "你是一个 OSCP/OSCP+/OSAI 安全知识库的问答助手。只根据下面提供的知识库片段回答问题，"
        "如果知识库片段里没有足够信息，就明确说'知识库中没有覆盖这个内容'，不要编造。"
        "回答末尾用一行列出引用了哪些卡片标题。\n\n"
        f"=== 知识库检索结果 ===\n{context}\n=== 结束 ==="
    )
    messages = [{"role": "system", "content": system_msg}] + history + [
        {"role": "user", "content": question}
    ]
    body = json.dumps({
        "model": DEEPSEEK_MODEL,
        "messages": messages,
        "stream": False,
        "temperature": 0.3,
    }).encode("utf-8")

    req = urllib.request.Request(
        DEEPSEEK_URL,
        data=body,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data["choices"][0]["message"]["content"]
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"DeepSeek API error {e.code}: {detail}")


def main():
    api_key = load_env()
    if not api_key:
        print("[!] No DEEPSEEK_API_KEY found. Copy .env.example to .env and fill in your key,")
        print("    or export DEEPSEEK_API_KEY=... in your shell.")
        sys.exit(1)

    corpus = load_corpus()
    print(f"[+] loaded {len(corpus)} cards from corpus.json")
    print("[+] ask anything about the knowledge base. type 'exit' to quit.\n")

    history = []
    while True:
        try:
            q = input("你 > ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not q:
            continue
        if q.lower() in ("exit", "quit", "q"):
            break

        hits = retrieve(q, corpus)
        if not hits:
            print("助手 > 知识库里没找到相关内容，换个问法试试？\n")
            continue

        print(f"  [检索到 {len(hits)} 张卡片: {', '.join(h['title'] for h in hits)}]")
        context = build_context(hits)
        try:
            answer = call_deepseek(api_key, q, context, history)
        except RuntimeError as e:
            print(f"助手 > [错误] {e}\n")
            continue

        print(f"助手 > {answer}\n")
        history.append({"role": "user", "content": q})
        history.append({"role": "assistant", "content": answer})
        history = history[-6:]  # keep last 3 turns


if __name__ == "__main__":
    main()

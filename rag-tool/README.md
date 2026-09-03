# RAG tool

Local, terminal-based Q&A over the knowledge base's ~100 cards. Retrieval is
plain weighted keyword scoring (see `rag.py`) — deliberately not
embeddings/a vector DB, since exhaustively scoring a corpus this size is both
fast and about as accurate as one would be at this scale. Generation calls
DeepSeek's chat completions API (OpenAI-compatible).

## Setup

```bash
node extract_corpus.js        # regenerate corpus.json from the HTML (needed once, and
                               # again any time offsec-kb.html's cards change)
cp .env.example .env
# edit .env, replace your-key-here with your real DeepSeek key
python3 rag.py
```

`.env` is gitignored at the repo root — your key never gets committed.

## What it does

- Retrieves the top 4 most relevant cards for your question (title/tags
  weighted higher than body text)
- Sends them as grounding context to DeepSeek, with an explicit
  instruction not to answer beyond what the retrieved cards actually say
- Prints which cards it retrieved for each question, so you can see
  what it's grounding on
- If retrieval comes back empty or thin, that's a real signal the
  knowledge base doesn't cover that topic yet — not a bug

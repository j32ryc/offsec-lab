# vulhub-fetch

Pull a single vulnerability lab out of [vulhub/vulhub](https://github.com/vulhub/vulhub)
(a community-maintained collection of a few hundred docker-compose CVE
reproductions) without cloning their entire repository.

## Usage

```bash
python3 fetch.py --list                 # every available lab, as "<product>/<CVE-or-name>"
python3 fetch.py --search log4j         # filter that list
python3 fetch.py log4j/CVE-2021-44228   # download it into ./downloaded/<path>/
```

The listing is cached locally after the first run (`.tree_cache.json`); use
`--refresh` to force a re-fetch if vulhub has added new labs since.

Once downloaded:

```bash
cd downloaded/log4j/CVE-2021-44228
docker-compose up -d
```

## Rate limits

This talks to GitHub's public REST API directly — no `git clone`, no
dependencies beyond the Python standard library. Unauthenticated requests are
capped at 60/hour, which is enough for fetching a lab or two in a sitting. If
you're pulling several, copy `.env.example` to `.env` and drop in a GitHub
[personal access token](https://github.com/settings/tokens) (no scopes
needed — it's read-only public data) for the 5000/hour authenticated limit.

## Why not just `git clone` vulhub?

Nothing wrong with that either — this just avoids pulling down every other
CVE's files when you only want one. Same idea as this repo's own labs, just
borrowing from a much larger community-maintained set instead of every
environment being hand-built.

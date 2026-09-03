// Extracts the DATA array (all knowledge-base cards) out of offsec-kb.html
// into a clean corpus.json for the RAG tool to retrieve from.
const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'knowledge-base', 'offsec-kb.html');
const html = fs.readFileSync(htmlPath, 'utf8');

const startMarker = 'const DATA = [';
const markerIdx = html.indexOf(startMarker);
// Guard on the raw indexOf result: adding the prefix length first would turn a
// "not found" (-1) into a positive offset, so the check never fired and the
// walker below would extract garbage from the top of the file instead.
if (markerIdx < 0) throw new Error('could not find DATA array in offsec-kb.html');
const start = markerIdx + 'const DATA = '.length;

// walk forward from the opening [ to find the matching closing ] (brace-depth aware,
// since payload code blocks contain arbitrary characters including brackets/quotes)
let depth = 0, i = start, inStr = null, esc = false;
for (; i < html.length; i++) {
  const c = html[i];
  if (inStr) {
    if (esc) { esc = false; }
    else if (c === '\\') { esc = true; }
    else if (c === inStr) { inStr = null; }
    continue;
  }
  if (c === '"' || c === "'" || c === '`') { inStr = c; continue; }
  if (c === '[') depth++;
  else if (c === ']') { depth--; if (depth === 0) { i++; break; } }
}
const arrSrc = html.slice(start, i);

// eslint-disable-next-line no-eval
const DATA = eval(arrSrc);

const corpus = DATA.map(d => ({
  id: d.id,
  domain: d.domain,
  title: d.title,
  en: d.en,
  summary: d.summary,
  tags: d.tags || [],
  intro: (d.intro || '').replace(/<[^>]+>/g, ''),
  variants: (d.variants || []).map(v => `${v.name}: ${v.desc}`).join(' | '),
  detect: (d.detect || '').replace(/<[^>]+>/g, ''),
  defend: (d.defend || []).join(' ').replace(/<[^>]+>/g, ''),
}));

fs.writeFileSync(path.join(__dirname, 'corpus.json'), JSON.stringify(corpus, null, 2));
console.log(`extracted ${corpus.length} cards -> corpus.json`);

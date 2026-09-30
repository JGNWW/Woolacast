// Zet de vijf schermen naast elkaar in één HTML-bestand (voor schermafdrukken en de criticus).
import fs from 'node:fs';
import path from 'node:path';
import { screens, CSS } from './screens.mjs';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&display=swap">`;
const html = `<!doctype html><html><head><meta charset="utf-8">${FONTS}<style>${base}${CSS}
.board{display:flex;gap:40px;padding:40px;background:#DDD5C8;width:max-content}
.board figure{margin:0}.board figcaption{font:600 14px 'Instrument Sans',sans-serif;margin-bottom:10px}
.ph{border-radius:28px;box-shadow:0 10px 30px rgba(0,0,0,.18)}</style></head><body><div class="board">
${Object.entries(screens).map(([k, s]) => `<figure id="${k}"><figcaption>${s.title}</figcaption>${s.body}</figure>`).join('\n')}
</div></body></html>`;
fs.writeFileSync(path.join(dir, 'preview.html'), html);
console.log('preview.html', html.length);

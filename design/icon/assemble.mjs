import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, 'base.css'), 'utf8');
const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&amp;family=Instrument+Sans:wght@400;500;600;700&amp;display=swap">`;

// <!--@name--> pulls in parts/name.svg
const expand = (s) => s.replace(/<!--@([\w-]+)-->/g, (_, n) =>
  fs.readFileSync(path.join(dir, 'parts', n + '.svg'), 'utf8').trim());

for (const f of fs.readdirSync(path.join(dir, 'parts')).filter((x) => x.endsWith('.body.html'))) {
  const name = f.replace('.body.html', '');
  const raw = fs.readFileSync(path.join(dir, 'parts', f), 'utf8');
  let cfg = {}, bodyStr = raw;
  const m = raw.match(/^<!--dc\s+([\s\S]*?)-->\n/);
  if (m) { cfg = JSON.parse(m[1]); bodyStr = raw.slice(m[0].length); }

  const w = cfg.w ?? 560, h = cfg.h ?? 620;
  const props = Object.assign({ $preview: { width: w, height: h } }, cfg.props || {});
  const css = base + (cfg.css ? '\n' + fs.readFileSync(path.join(dir, 'parts', cfg.css), 'utf8') : '');
  const propsAttr = JSON.stringify(props).replace(/&/g, '&amp;').replace(/'/g, '&#39;');

  fs.writeFileSync(path.join(dir, name + '.dc.html'), `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
${FONTS}
<style>
${css}</style>
</helmet>
${expand(bodyStr).trimEnd()}
</x-dc>
<script data-dc-script data-props='${propsAttr}'>
class Component extends DCLogic {}
</script>
</body>
</html>
`);
  console.log('built', name + '.dc.html');
}

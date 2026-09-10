import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, 'base.css'), 'utf8');
const dark = fs.readFileSync(path.join(dir, 'dark.css'), 'utf8');

const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&amp;family=Instrument+Sans:wght@400;500;600;700&amp;display=swap">`;

const parts = fs.readdirSync(path.join(dir, 'parts')).filter(f => f.endsWith('.body.html'));
for (const f of parts) {
  const name = f.replace('.body.html', '');
  const raw = fs.readFileSync(path.join(dir, 'parts', f), 'utf8');
  // optional front-matter line:  <!--dc {"props":..., "logic":"file.js", "dark":true, "w":390,"h":844} -->
  let cfg = {};
  let bodyStr = raw;
  const m = raw.match(/^<!--dc\s+([\s\S]*?)-->\n/);
  if (m) { cfg = JSON.parse(m[1]); bodyStr = raw.slice(m[0].length); }

  const w = cfg.w ?? 390, h = cfg.h ?? 844;
  const props = Object.assign({ $preview: { width: w, height: h } }, cfg.props || {});
  const logic = cfg.logic
    ? fs.readFileSync(path.join(dir, 'parts', cfg.logic), 'utf8')
    : 'class Component extends DCLogic {}';

  const css = base + (cfg.dark ? '\n' + dark : '') + (cfg.css ? '\n' + fs.readFileSync(path.join(dir, 'parts', cfg.css), 'utf8') : '');
  const propsAttr = JSON.stringify(props).replace(/&/g, '&amp;').replace(/'/g, '&#39;');

  const out = `<!doctype html>
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
${bodyStr.trimEnd()}
</x-dc>
<script data-dc-script data-props='${propsAttr}'>
${logic.trimEnd()}
</script>
</body>
</html>
`;
  fs.writeFileSync(path.join(dir, name + '.dc.html'), out);
  console.log('built', name + '.dc.html', `(${out.length} bytes)`);
}

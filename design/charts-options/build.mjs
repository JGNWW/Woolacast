// Vijf varianten van het Hitlijsten-scherm, gebouwd op dezelfde base.css als de mockup.
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&amp;family=Instrument+Sans:wght@400;500;600;700&amp;display=swap">`;

const shows = [
  ['De Deadline', 'Bureau Kruit', 'a1', 1],
  ['Koud Spoor', 'Podium Audio', 'a7', -1],
  ['Nachtdienst', 'VRIJDAG Media', 'a2', 4],
  ['Tafel voor Twee', 'Roos &amp; Van Dijk', 'a3', 0],
  ['Vandaag in Zeven', 'Dagblad Noord', 'a9', 2],
  ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 'new'],
  ['Lange Adem', 'Studio Hemel', 'a5', -3],
  ['Ondergronds', 'Kelderwerk', 'a4', 5],
  ['Zwart op Wit', 'Concept Media', 'a8', -2],
  ['Kade 12', 'Kade Media', 'a10', 0]
];

const UP = `<svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>`;
const DOWN = `<svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"></path></svg>`;
const mv = (m) => {
  if (m === 'new') return `<div class="mv new">NIEUW</div>`;
  if (m > 0) return `<div class="mv" style="color:#0E8A4E">${UP}${m}</div>`;
  if (m < 0) return `<div class="mv" style="color:#C03A24">${DOWN}${-m}</div>`;
  return `<div class="mv flat">–</div>`;
};
const pill = (m) => {
  if (m === 'new') return `<span class="pill" style="background:#FBE3D4;color:#5A2110;letter-spacing:.06em">NIEUW</span>`;
  if (m > 0) return `<span class="pill" style="background:#DCF0E4;color:#0B6E3E"><svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>${m}</span>`;
  if (m < 0) return `<span class="pill" style="background:#F7DED8;color:#8E2C1B"><svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"></path></svg>${-m}</span>`;
  return `<span class="pill" style="background:var(--surf2);color:var(--ink3)">–</span>`;
};

const row = (s, i, opts = {}) => `
      <div class="lrow" style="${opts.rowStyle || ''}">
        <div class="rank ${i < 3 ? 'top' : ''}" style="${opts.rankStyle || ''}">${i + 1}</div>
        <div class="art ${s[2]}" style="${opts.artStyle || ''}"></div>
        <div class="meta">
          <div class="t1">${s[0]}</div>
          <div class="t2">${s[1]}</div>
        </div>
        ${opts.pill ? pill(s[3]) : mv(s[3])}
      </div>`;
const rows = (from, to, opts) => shows.slice(from, to).map((s, i) => row(s, from + i, opts)).join('');

const ICON = {
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20.5 20.5L16.6 16.6"></path></svg>`,
  bell: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"></path><path d="M13.7 20a2 2 0 01-3.4 0"></path></svg>`,
  filter: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M4 7h16"></path><path d="M7 12h10"></path><path d="M10 17h4"></path></svg>`,
  chev: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:#988C7A"><path d="M6 9l6 6 6-6"></path></svg>`,
  chevS: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>`,
  pause: `<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="5" width="3.6" height="14" rx="1.2"></rect><rect x="13.4" y="5" width="3.6" height="14" rx="1.2"></rect></svg>`,
  fwd: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M5 12h14"></path><path d="M14 7l5 5-5 5"></path></svg>`,
  navCharts: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 20v-7"></path><path d="M12 20V4"></path><path d="M19 20v-11"></path></svg>`,
  navDisc: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"></circle><path d="M15 9l-2 4.2L9 15l2-4.2z"></path></svg>`,
  navLib: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5v14"></path><path d="M9.5 5v14"></path><path d="M14.5 6.2l5 12.8"></path></svg>`
};
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

const appbar = (actions) => `
  <div class="appbar">
    <div class="mark">Woolacast</div>
    <div class="row" style="gap:0">${actions.map(a => `<div class="ibtn">${ICON[a]}</div>`).join('')}</div>
  </div>`;

const chrome = `
  <div class="mini">
    <div class="art a1"></div>
    <div class="meta">
      <div class="t1" style="color:#F6EFE5">Live vanuit Paradiso</div>
      <div class="t2" style="color:#B3A695">De Deadline · nog 18 min</div>
    </div>
    <div class="ibtn" style="color:#F6EFE5">${ICON.pause}</div>
    <div class="ibtn" style="color:#F6EFE5">${ICON.fwd}</div>
  </div>
  <div class="nav">
    <div class="navi on">${ICON.navCharts}Hitlijsten</div>
    <div class="navi">${ICON.navDisc}Ontdek</div>
    <div class="navi">${ICON.navLib}Bibliotheek</div>
  </div>
  <div class="gest"><i></i></div>`;

const tabs = (extra = '') => `
    <div class="pad tabs" style="${extra}">
      <div class="tab on">Podcasts</div>
      <div class="tab">Afleveringen</div>
    </div>
    <div class="hr"></div>`;

const srcChips = `
    <div class="pad srcrow" style="margin-bottom:14px">
      <div class="src on"><span class="dot">A</span>Apple Podcasts</div>
      <div class="src"><span class="dot">S</span>Spotify</div>
    </div>`;

const ctx = `
    <div class="pad" style="margin-bottom:2px">
      <div class="ctx">
        ${FLAG}
        <span class="lbl" style="white-space:nowrap">Nederland</span>
        <span class="sep">·</span>
        <span class="lbl" style="color:#6E6458;white-space:nowrap">Alle categorieën</span>
        ${ICON.chev}
        <span style="flex:1 1 auto"></span>
        <span class="note" style="padding-right:10px;white-space:nowrap">Bijgewerkt 06:00</span>
      </div>
    </div>`;

const screens = {};

/* ---- Origineel: het artboard zoals getekend, statisch ---- */
screens.Main = { title: 'Origineel — zoals getekend', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Hitlijsten</h1></div>
    ${srcChips}
    ${ctx}
    ${tabs()}
    <div class="pad list">${rows(0, 7)}</div>
  </div>
  ${chrome}
</div>` };

/* ---- A: Segment — bron als schakelaar, land en categorie als twee knoppen ---- */
screens.OptieA = { title: 'A · Schakelaar', css: `
.seg{display:flex;height:44px;padding:4px;border-radius:13px;background:var(--surf2);border:1px solid var(--line2)}
.seg div{flex:1 1 0;display:flex;align-items:center;justify-content:center;gap:8px;border-radius:10px;font-size:13.5px;font-weight:600;color:var(--ink2)}
.seg div.on{background:var(--surf);color:var(--ink);box-shadow:0 1px 3px rgba(33,29,23,.14)}
.pbtn2{display:flex;align-items:center;gap:8px;height:40px;padding:0 12px 0 12px;border-radius:12px;background:var(--surf);border:1px solid var(--line);font-size:13.5px;font-weight:600;flex:1 1 0;min-width:0}
.pbtn2 span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Hitlijsten</h1></div>
    <div class="pad" style="padding-bottom:10px">
      <div class="seg">
        <div class="on"><span class="dot" style="width:20px;height:20px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:800;background:#C4542B;color:#FBF6EE">A</span>Apple Podcasts</div>
        <div><span class="dot" style="width:20px;height:20px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:800;background:#988C7A;color:#FBF6EE">S</span>Spotify</div>
      </div>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:4px">
      <div class="pbtn2">${FLAG}<span>Nederland</span><span style="flex:1 1 auto"></span>${ICON.chevS}</div>
      <div class="pbtn2"><span>Alle categorieën</span><span style="flex:1 1 auto"></span>${ICON.chevS}</div>
    </div>
    ${tabs()}
    <div class="pad list">${rows(0, 7)}</div>
  </div>
  ${chrome}
</div>` };

/* ---- B: Kop met context — de filters worden de ondertitel ---- */
screens.OptieB = { title: 'B · Kop met context', css: `
.sub{display:flex;align-items:center;gap:8px;font-size:13.5px;color:var(--ink2);font-weight:500;flex-wrap:wrap}
.sub .sdot{width:20px;height:20px;border-radius:6px;font-size:10px}
.sub .chg{color:var(--pri);font-weight:700}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'filter'])}
  <div class="body">
    <div class="pad" style="padding-top:2px">
      <div class="eyebrow">Top 200 · Bijgewerkt 06:00</div>
      <h1 class="h1 dsp" style="margin-top:6px">Nederland</h1>
      <div class="sub" style="margin-top:10px;margin-bottom:12px">
        <span class="sdot" style="background:#C4542B">A</span><span>Apple Podcasts</span>
        <span class="sep" style="color:var(--ink3)">·</span><span>Alle categorieën</span>
        <span class="sep" style="color:var(--ink3)">·</span><span class="chg">Wijzig</span>
      </div>
    </div>
    ${tabs()}
    <div class="pad list">${rows(0, 7, { artStyle: 'width:56px;height:56px;border-radius:12px', pill: true })}</div>
  </div>
  ${chrome}
</div>` };

/* ---- C: Podium — de top drie groot, daarna de lijst ---- */
screens.OptieC = { title: 'C · Podium', css: `
.pod{display:flex;gap:10px}
.pcard{flex:1 1 0;min-width:0;position:relative}
.pcard .art{width:100%;height:110px;border-radius:14px}
.pcard .badge{position:absolute;top:-8px;left:-6px;width:28px;height:28px;border-radius:14px;background:var(--pri);color:var(--priInk);
  display:flex;align-items:center;justify-content:center;font-size:14px;font-weight:800;box-shadow:0 0 0 3px var(--bg);font-family:'Bricolage Grotesque',sans-serif}
.pcard .t1{margin-top:8px;font-size:13.5px;white-space:normal;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;line-height:1.25}
.pcard .t2{margin-top:2px;font-size:11.5px}
.pcard .pill{margin-top:6px;display:inline-flex}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Hitlijsten</h1></div>
    ${srcChips}
    ${ctx}
    <div class="pad pod" style="padding-top:20px;padding-bottom:16px">
      ${shows.slice(0, 3).map((s, i) => `
      <div class="pcard">
        <div class="art ${s[2]}"></div><div class="badge">${i + 1}</div>
        <div class="t1">${s[0]}</div><div class="t2">${s[1]}</div>${pill(s[3])}
      </div>`).join('')}
    </div>
    ${tabs()}
    <div class="pad list">${rows(3, 8)}</div>
  </div>
  ${chrome}
</div>` };

/* ---- D: Donker paneel — bron, land en categorie in één inktkaart ---- */
screens.OptieD = { title: 'D · Donker paneel', css: `
.panel{border-radius:18px;background:var(--ink);color:#F6EFE5;padding:14px 14px 12px;display:flex;flex-direction:column;gap:12px}
.panel .src{background:rgba(246,239,229,.08);border-color:transparent;color:#F6EFE5;height:40px}
.panel .src .dot{background:rgba(246,239,229,.25)}
.panel .src.on{background:var(--pri);color:#FFFDFA}
.panel .src.on .dot{background:#5A2110}
.panel .ctx{background:rgba(246,239,229,.08);border-color:transparent;color:#F6EFE5;height:42px}
.panel .ctx .sep,.panel .note{color:#B3A695}
.brank{font-family:'Bricolage Grotesque',sans-serif;font-size:26px;letter-spacing:-.04em;width:34px}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Hitlijsten</h1></div>
    <div class="pad" style="padding-bottom:6px">
      <div class="panel">
        <div class="srcrow">
          <div class="src on"><span class="dot">A</span>Apple Podcasts</div>
          <div class="src"><span class="dot">S</span>Spotify</div>
        </div>
        <div class="ctx">
          ${FLAG}<span class="lbl" style="white-space:nowrap">Nederland</span><span class="sep">·</span><span class="lbl" style="white-space:nowrap">Alle categorieën</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#B3A695" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>
          <span style="flex:1 1 auto"></span><span class="note" style="padding-right:8px">06:00</span>
        </div>
      </div>
    </div>
    ${tabs()}
    <div class="pad list">${rows(0, 7, { rankStyle: '', artStyle: 'box-shadow:none' }).replace(/class="rank/g, 'class="rank brank')}</div>
  </div>
  ${chrome}
</div>` };

/* ---- E: Compact — kleinere kop en rijen, meer lijst in beeld ---- */
screens.OptieE = { title: 'E · Compact', css: `
.mini-chip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap}
.mini-chip.on{background:var(--ink);border-color:var(--ink);color:#FBF6EE}
.mini-chip .dot{width:18px;height:18px;border-radius:5px;font-size:9.5px}
.lrow.c{height:60px}
.lrow.c .art{width:44px;height:44px;border-radius:9px}
.lrow.c .rank{font-size:16px;width:22px}
.lrow.c .t1{font-size:14px}.lrow.c .t2{font-size:12px}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-top:0;padding-bottom:12px">
      <h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1>
      <span class="note">Bijgewerkt 06:00</span>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="mini-chip on"><span class="dot" style="background:var(--pri);color:#FBF6EE;display:flex;align-items:center;justify-content:center;font-weight:800">A</span>Apple ${ICON.chevS}</div>
      <div class="mini-chip">${FLAG}NL ${ICON.chevS}</div>
      <div class="mini-chip">Categorie ${ICON.chevS}</div>
    </div>
    ${tabs('height:40px')}
    <div class="pad list">${rows(0, 10).replace(/class="lrow"/g, 'class="lrow c"')}</div>
  </div>
  ${chrome}
</div>` };

for (const [name, s] of Object.entries(screens)) {
  const css = base + (s.css ? '\n' + s.css : '');
  const props = JSON.stringify({ $preview: { width: 390, height: 844 } });
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
${s.body.trim()}
</x-dc>
<script data-dc-script data-props='${props}'>
class Component extends DCLogic {}
</script>
</body>
</html>
`;
  fs.writeFileSync(path.join(dir, name + '.dc.html'), out);
  console.log('built', name + '.dc.html', out.length);
}

const order = ['Main', 'OptieA', 'OptieB', 'OptieC', 'OptieD', 'OptieE'];
const canvas = {
  artboards: order.map((n, i) => ({ file: n + '.dc.html', x: i * 470, y: 0, w: 390, h: 844, title: screens[n].title })),
  annotations: [
    { id: 'n-orig', x: 0, y: -170, w: 390, text: 'Links het origineel zoals getekend. Daarnaast vijf richtingen voor hetzelfde scherm — dezelfde tokens, lettertypen en rijen, alleen de kop en de filters verschillen.' },
    { id: 'n-a', x: 470, y: -110, w: 390, text: 'A · Schakelaar: bron als één schakelaar, land en categorie als twee losse knoppen. Rustiger; werkt alleen zolang er twee bronnen zijn.' },
    { id: 'n-b', x: 940, y: -110, w: 390, text: 'B · Kop met context: het land wordt de titel, bron en categorie de ondertitel; filters via het filtericoon. Meest editorial; instellen kost één tik extra.' },
    { id: 'n-c', x: 1410, y: -110, w: 390, text: 'C · Podium: de top drie groot, daarna de lijst. Spannend bovenin; je ziet minder rijen in één keer.' },
    { id: 'n-d', x: 1880, y: -110, w: 390, text: 'D · Donker paneel: alle filters in één inktkaart, grote cijfers in de lijst. Duidelijk één blok "instellen"; zwaardere kop.' },
    { id: 'n-e', x: 2350, y: -110, w: 390, text: 'E · Compact: kleinere kop, drie kleine chips, rijen van 60 px — tien posities in beeld. Meest lijst, minst lucht.' }
  ],
  launch: { view: 'canvas' }
};
fs.writeFileSync(path.join(dir, 'canvas.json'), JSON.stringify(canvas, null, 2));
console.log('canvas.json ok');

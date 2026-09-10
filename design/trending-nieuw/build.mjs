// Mockups voor "Trending" en "Nieuw", op de gekozen variant E van het Hitlijsten-scherm.
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&amp;family=Instrument+Sans:wght@400;500;600;700&amp;display=swap">`;

const ICON = {
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20.5 20.5L16.6 16.6"></path></svg>`,
  bell: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"></path><path d="M13.7 20a2 2 0 01-3.4 0"></path></svg>`,
  chevS: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>`,
  pause: `<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="5" width="3.6" height="14" rx="1.2"></rect><rect x="13.4" y="5" width="3.6" height="14" rx="1.2"></rect></svg>`,
  fwd: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M5 12h14"></path><path d="M14 7l5 5-5 5"></path></svg>`,
  navCharts: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 20v-7"></path><path d="M12 20V4"></path><path d="M19 20v-11"></path></svg>`,
  navDisc: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"></circle><path d="M15 9l-2 4.2L9 15l2-4.2z"></path></svg>`,
  navLib: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5v14"></path><path d="M9.5 5v14"></path><path d="M14.5 6.2l5 12.8"></path></svg>`,
  // Trending: een pijl die omhoog uitbreekt; Nieuw: een fonkel.
  trend: (c = 'currentColor', s = 14) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 17l6-6 4 4 6-7"></path><path d="M15 8h5v5"></path></svg>`,
  spark: (c = 'currentColor', s = 14) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="${c}"><path d="M12 3l2.2 5.8L20 11l-5.8 2.2L12 19l-2.2-5.8L4 11l5.8-2.2z"></path></svg>`
};
const UP = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>`;
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

const appbar = (actions) => `
  <div class="appbar">
    <div class="mark">Woolacast</div>
    <div class="row" style="gap:0">${actions.map(a => `<div class="ibtn">${ICON[a]}</div>`).join('')}</div>
  </div>`;

const chrome = (active) => `
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
    <div class="navi ${active === 'charts' ? 'on' : ''}">${ICON.navCharts}Hitlijsten</div>
    <div class="navi ${active === 'discover' ? 'on' : ''}">${ICON.navDisc}Ontdek</div>
    <div class="navi">${ICON.navLib}Bibliotheek</div>
  </div>
  <div class="gest"><i></i></div>`;

// Kop van variant E, zonder letters voor de bronnen.
const headE = (source) => `
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px">
      <h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1>
      <span class="note">Bijgewerkt 06:00</span>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="mini-chip on">${source} ${ICON.chevS}</div>
      <div class="mini-chip">${FLAG}NL ${ICON.chevS}</div>
      <div class="mini-chip">Categorie ${ICON.chevS}</div>
    </div>`;

const tabs4 = (on) => `
    <div class="pad tabs" style="height:40px;gap:16px">
      ${['Podcasts', 'Afleveringen', 'Trending', 'Nieuw'].map(t => `<div class="tab ${t === on ? 'on' : ''}" style="font-size:14px;height:40px">${t}</div>`).join('')}
    </div>
    <div class="hr"></div>`;

const CSS_E = `
.mini-chip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap}
.mini-chip.on{background:var(--ink);border-color:var(--ink);color:#FBF6EE}
.lrow.c{height:60px}
.lrow.c .art{width:44px;height:44px;border-radius:9px}
.lrow.c .rank{font-size:16px;width:22px}
.lrow.c .t1{font-size:14px}.lrow.c .t2{font-size:12px}
.tnote{display:flex;align-items:center;gap:6px;height:30px;font-size:11.5px;color:var(--ink3)}
.pill.trend{background:#FBE3D4;color:#5A2110}
.pill.new{background:#DCF0E4;color:#0B6E3E;letter-spacing:.04em}
`;

const screens = {};

/* ---- 1. Trending als vierde tabblad (Spotify) ---- */
const trending = [
  ['Ondergronds', 'Kelderwerk', 'a4', 28, 3],
  ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 41, 6],
  ['Kade 12', 'Kade Media', 'a10', 19, 7],
  ['Zwart op Wit', 'Concept Media', 'a8', 12, 9],
  ['Nachtdienst', 'VRIJDAG Media', 'a2', 5, 1],
  ['Lange Adem', 'Studio Hemel', 'a5', 30, 22],
  ['Vandaag in Zeven', 'Dagblad Noord', 'a9', 14, 11],
  ['Tafel voor Twee', 'Roos &amp; Van Dijk', 'a3', 9, 4],
  ['Koud Spoor', 'Podium Audio', 'a7', 6, 5]
];
screens.Main = { title: 'Trending — vierde tabblad', css: CSS_E, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    ${headE('Spotify')}
    ${tabs4('Trending')}
    <div class="pad tnote">${ICON.trend('#988C7A', 13)}Snelste stijgers in Nederland · Spotify · dagelijks</div>
    <div class="pad list">
      ${trending.map((s, i) => `
      <div class="lrow c">
        <div class="rank ${i < 3 ? 'top' : ''}">${i + 1}</div>
        <div class="art ${s[2]}"></div>
        <div class="meta">
          <div class="t1">${s[0]}</div>
          <div class="t2">${s[1]} · nu #${s[4]} in Top 200</div>
        </div>
        <span class="pill trend">${UP}${s[3]}</span>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 2. Nieuw als tabblad: wat sinds gisteren binnenkwam ---- */
const fresh = [
  ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 6, 'gisteren'],
  ['Kade 12', 'Kade Media', 'a10', 31, 'gisteren'],
  ['Zwart op Wit', 'Concept Media', 'a8', 58, 'gisteren'],
  ['De Slechtste Podcast', 'Studio Hemel', 'a5', 74, 'gisteren'],
  ['Halve Zolen', 'Podium Audio', 'a7', 92, '2 dagen'],
  ['Doorzagen', 'Kelderwerk', 'a4', 116, '2 dagen'],
  ['Kort Lontje', 'Roos &amp; Van Dijk', 'a3', 140, '3 dagen'],
  ['Zaterdagavond Thuis', 'Concept Media', 'a8', 171, '3 dagen']
];
screens.Nieuw = { title: 'Nieuw — nieuwe binnenkomers', css: CSS_E + `
.day{font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;color:var(--ink3);padding:12px 0 4px}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    ${headE('Apple Podcasts')}
    ${tabs4('Nieuw')}
    <div class="pad tnote">${ICON.spark('#988C7A', 13)}Nieuw in de Top 200 sinds gisteren · 8 podcasts</div>
    <div class="pad list">
      <div class="day">Gisteren</div>
      ${fresh.filter(s => s[4] === 'gisteren').map(s => `
      <div class="lrow c">
        <div class="art ${s[2]}"></div>
        <div class="meta">
          <div class="t1">${s[0]}</div>
          <div class="t2">${s[1]}</div>
        </div>
        <span class="pill new">NIEUW OP #${s[3]}</span>
      </div>`).join('')}
      <div class="day">Eerder deze week</div>
      ${fresh.filter(s => s[4] !== 'gisteren').map(s => `
      <div class="lrow c">
        <div class="art ${s[2]}"></div>
        <div class="meta">
          <div class="t1">${s[0]}</div>
          <div class="t2">${s[1]} · ${s[4]} geleden</div>
        </div>
        <span class="pill" style="background:var(--surf2);color:var(--ink2)">#${s[3]}</span>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 3. Op Ontdek: twee secties in plaats van tabbladen ---- */
screens.Ontdek = { title: 'Alternatief — secties op Ontdek', css: `
.search{display:flex;align-items:center;gap:10px;height:50px;padding:0 15px;border-radius:14px;background:var(--surf);border:1px solid var(--line);color:var(--ink3);font-size:14.5px}
.hscroll{display:flex;gap:12px;overflow:hidden}
.mcard{width:124px;flex:none}
.mcard .art{width:124px;height:124px;border-radius:14px}
.mrank{display:flex;align-items:center;gap:5px;margin-top:9px;font-size:11.5px;font-weight:700;color:#5A2110}
.mtitle{font-size:13.5px;font-weight:600;letter-spacing:-.008em;margin-top:4px;line-height:1.25;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.msub{font-size:11.5px;color:var(--ink3);margin-top:2px}
.nrow{display:flex;align-items:center;gap:12px;height:58px;border-bottom:1px solid var(--line2)}
.nrow .art{width:42px;height:42px;border-radius:9px}
.pill.new{background:#DCF0E4;color:#0B6E3E;letter-spacing:.04em}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar">
    <div class="mark">Woolacast</div>
    <div class="ibtn">${ICON.bell}</div>
  </div>
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:12px"><h1 class="h1 dsp">Ontdek</h1></div>
    <div class="pad" style="padding-bottom:14px"><div class="search">${ICON.search}Zoek podcasts en afleveringen</div></div>

    <div class="pad sect">
      <div class="h2 dsp row" style="gap:8px">${ICON.trend('#C4542B', 20)}Trending</div>
      <div style="font-size:12.5px;font-weight:600;color:var(--pri)">Spotify · NL</div>
    </div>
    <div class="pad hscroll" style="padding-bottom:16px">
      ${trending.slice(0, 3).map((s, i) => `
      <div class="mcard">
        <div class="art ${s[2]}"></div>
        <div class="mrank">${ICON.trend('#5A2110', 12)}#${i + 1} trending · ${UP}${s[3]}</div>
        <div class="mtitle">${s[0]}</div>
        <div class="msub">${s[1]}</div>
      </div>`).join('')}
    </div>

    <div class="pad sect">
      <div class="h2 dsp row" style="gap:8px">${ICON.spark('#C4542B', 18)}Nieuw binnen</div>
      <div style="font-size:12.5px;font-weight:600;color:var(--pri)">Alles</div>
    </div>
    <div class="pad">
      ${fresh.slice(0, 4).map(s => `
      <div class="nrow">
        <div class="art ${s[2]}"></div>
        <div class="meta">
          <div class="t1">${s[0]}</div>
          <div class="t2">${s[1]} · Apple NL</div>
        </div>
        <span class="pill new">NIEUW OP #${s[3]}</span>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('discover')}
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

const order = ['Main', 'Nieuw', 'Ontdek'];
fs.writeFileSync(path.join(dir, 'canvas.json'), JSON.stringify({
  artboards: order.map((n, i) => ({ file: n + '.dc.html', x: i * 470, y: 0, w: 390, h: 844, title: screens[n].title })),
  annotations: [
    { id: 'n-trend', x: 0, y: -150, w: 390, text: 'Trending als vierde tabblad. Bron: Spotify publiceert een echte trending-lijst (26 landen). Apple niet — bij Apple toont het tabblad de snelste stijgers uit onze eigen dagelijkse metingen, met dezelfde opmaak.' },
    { id: 'n-new', x: 470, y: -150, w: 390, text: 'Nieuw als tabblad: alles wat sinds gisteren de Top 200 binnenkwam, gegroepeerd per dag, met de plek waarop het binnenkwam. Werkt voor Apple én Spotify, omdat het uit onze eigen metingen komt.' },
    { id: 'n-disc', x: 940, y: -150, w: 390, text: 'Alternatief: geen extra tabbladen, maar twee secties op Ontdek — Trending als kaarten, Nieuw binnen als lijstje. Hitlijsten blijft dan zoals hij is.' }
  ],
  launch: { view: 'canvas' }
}, null, 2));
console.log('canvas.json ok');

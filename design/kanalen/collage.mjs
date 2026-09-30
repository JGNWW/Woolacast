// Mockups: de makerpagina zonder rond logo, met een collage van zijn toppodcasts
// als kop, in de stijl van Beeldgloed. Bouwt collage.html; alle namen verzonnen.
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');

const I = {
  back: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"/></svg>`,
  more: `<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5.5" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="12" cy="18.5" r="1.8"/></svg>`,
  plus: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>`,
  share: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 15V4"/><path d="M8 8l4-4 4 4"/><path d="M5 14v4.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V14"/></svg>`,
  chevD: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 9l6 6 6-6"/></svg>`,
  chevR: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg>`,
  up: `<svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"/></svg>`,
  down: `<svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"/></svg>`,
  info: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5"/><path d="M12 7.8v.1"/></svg>`,
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20.5 20.5L16.6 16.6"/></svg>`,
  bell: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"/><path d="M13.7 20a2 2 0 01-3.4 0"/></svg>`
};
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

// De shows van Kade Media, op plek in de lijst. De hoezen komen uit base.css (a1–a10).
const shows = [
  ['Kade 12', 'a10', 'Nieuwe afl. vandaag · True crime', 1],
  ['Nachtdienst', 'a2', 'Nieuwe afl. gisteren · Maatschappij', 3],
  ['Tafel voor Twee', 'a3', 'Nieuwe afl. gisteren · Eten', 5],
  ['Lange Adem', 'a5', 'Nieuwe afl. zo · Sport', 9],
  ['Halve Zolen', 'a9', 'Nieuwe afl. ma · Comedy', 13],
  ['Koud Spoor', 'a7', 'Nieuwe afl. 21 sep · True crime', 22],
  ['Ondergronds', 'a4', 'Nieuwe afl. 19 sep · True crime', null],
  ['Zwart op Wit', 'a8', 'Nieuwe afl. 17 sep · Comedy', null],
  ['Het Vijfde Kwartier', 'a6', 'Nieuwe afl. 12 sep · Sport', null]
];
const art = shows.map(s => s[1]);

/* ---- drie collages ---- */
const collageGrid = () => `
  <div class="col-grid">${art.slice(0, 4).map(a => `<div class="art ${a}"></div>`).join('')}</div>`;
const collageMosaic = () => `
  <div class="col-mosaic">
    <div class="art ${art[0]} big"></div>
    ${art.slice(1, 6).map(a => `<div class="art ${a}"></div>`).join('')}
  </div>`;
const collageWall = () => `
  <div class="col-wall">${[0, 1, 2, 3].map(r => `
    <div class="wall-row" style="margin-left:${-40 - r * 52}px">${[...art, ...art].slice(r * 2, r * 2 + 6).map(a => `<div class="art ${a}"></div>`).join('')}</div>`).join('')}
  </div>`;

const glass = (icon) => `<div class="glass">${icon}</div>`;

const rows = (dark) => shows.slice(0, 5).map(s => `
      <div class="lrow mrow">
        <div class="art ${s[1]}"></div>
        <div class="meta"><div class="t1">${s[0]}</div><div class="t2">${s[2]}</div></div>
        <span class="pill place">#${s[3]}</span>
        <span class="chev">${I.chevR}</span>
      </div>`).join('');

/** De makerpagina met een collage als kop; [dark] = Beeldgloed donker. */
const makerPage = (collage, dark, note = '') => `
<div class="ph col ${dark ? 'dark' : ''}">
  <div class="hero">${collage}<div class="hero-fade"></div></div>
  <div class="glowbed"></div>
  <div class="topbar">${glass(I.back)}${glass(I.more)}</div>
  <div class="content">
    <div class="pad" style="padding-top:286px">
      <div class="eyebrow-g">Maker · 9 podcasts</div>
      <div class="title-g dsp">Kade Media</div>
      <div class="sub-g">5 in de Top 200 van Apple ${FLAG} NL</div>
      <div class="row" style="gap:10px;margin-top:18px">
        <div class="pillbtn">${I.plus}Volg maker</div>
        <div class="circbtn">${I.share}</div>
      </div>
    </div>
    <div class="pad row" style="gap:8px;margin-top:20px">
      <div class="fchip on">Populair</div><div class="fchip">Recent</div><div class="fchip">A–Z</div>
    </div>
    <div class="pad note-g">Plek in <b>Apple</b> · ${FLAG} <b>NL</b> · Top 200 <span class="chev">${I.chevD}</span></div>
    <div class="pad list">${rows(dark)}</div>
  </div>
  ${note ? `<div class="stamp">${note}</div>` : ''}
</div>`;

/** Mini-collage als teken voor een maker in een lijst: vier hoezen in één vierkant. */
const tile = (arts, size) => `<div class="tile" style="width:${size}px;height:${size}px">${arts.slice(0, 4).map(a => `<div class="art ${a}"></div>`).join('')}</div>`;

const makersList = [
  ['Kade Media', ['a10', 'a2', 'a3', 'a5'], '5 podcasts · beste #1', 'up', 1],
  ['Dagblad Noord', ['a9', 'a4', 'a6', 'a1'], '3 podcasts · beste #2', 'down', 1],
  ['Radio Oost', ['a6', 'a4', 'a8', 'a3'], '3 podcasts · beste #4', 'flat'],
  ['Podium Audio', ['a7', 'a1'], '2 podcasts · beste #8', 'flat'],
  ['Studio Hemel', ['a5', 'a8'], '2 podcasts · beste #10', 'new']
];
const mv = (d, n) => d === 'up' ? `<div class="mv" style="color:var(--up)">${I.up}${n}</div>`
  : d === 'down' ? `<div class="mv" style="color:var(--down)">${I.down}${n}</div>`
  : d === 'new' ? `<div class="mv"><span class="newtag">NIEUW</span></div>` : `<div class="mv flat">–</div>`;

const chartsPage = () => `
<div class="ph col">
  <div class="chartglow"></div>
  <div class="sa"></div>
  <div class="appbar"><div class="mark">Toadcast</div><div class="row" style="gap:0"><div class="ibtn">${I.search}</div><div class="ibtn">${I.bell}</div></div></div>
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px">
      <h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1><span class="note" style="color:var(--ink3)">Bijgewerkt 10:20</span>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="schip dark">Apple ${I.chevD}</div><div class="schip">${FLAG}NL ${I.chevD}</div><div class="schip">Categorie ${I.chevD}</div>
    </div>
    <div class="pad tabs" style="height:40px">${['Podcasts', 'Afleveringen', 'Trending', 'Nieuw'].map((t, i) => `<div class="tab ${i ? '' : 'on'}" style="height:40px">${t}</div>`).join('')}</div>
    <div class="hr"></div>
    <div class="pad row" style="justify-content:space-between;height:48px">
      <span class="row" style="gap:6px;font-size:11.5px;color:var(--ink2)">2+ podcasts · sinds gisteren <span style="color:var(--ink3);display:flex">${I.info}</span></span>
      <div class="schip">Per maker ${I.chevD}</div>
    </div>
    <div class="pad list">
      ${makersList.map((m, i) => `
      <div class="lrow" style="height:60px">
        <div class="rank ${i < 3 ? 'top' : ''}" style="font-size:16px;width:22px">${i + 1}</div>
        ${tile(m[1], 44)}
        <div class="meta"><div class="t1" style="font-size:14px">${m[0]}</div><div class="t2" style="font-size:12.5px">${m[2]}</div></div>
        ${mv(m[3], m[4])}
      </div>`).join('')}
    </div>
  </div>
</div>`;

export const CSS = `
.ph{position:relative;overflow:hidden}
.ph.dark{--bg:#0E0F11;--surf:#16171A;--surf2:#1E1F23;--surf3:#28292E;--line:#3A3C42;--line2:rgba(237,238,240,.08);
  --ink:#EDEEF0;--ink2:#A0A3AA;--ink3:#A0A3AA;--pri:#F0895B;--priC:#34363C;--priOn:#F0895B;--up:#5BCB8F;--down:#FF7A93;
  background:var(--bg);color:var(--ink)}
/* Gloed uit de hoes van de nummer 1 (Kade 12, paars): Beeldgloed-rollen, licht L .93 en donker L .33. */
.ph{--glow:#EEE6F6;--soft:var(--ink2);--outline:rgba(33,29,23,.45)}
.ph.dark{--glow:#2F2744;--soft:rgba(237,238,240,.78);--outline:rgba(237,238,240,.5)}
.hero{position:absolute;left:0;top:0;width:390px;height:380px;overflow:hidden}
.hero .art{border-radius:0;box-shadow:none}
.hero-fade{position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,0) 38%,color-mix(in srgb,var(--glow) 55%,transparent) 66%,var(--glow) 100%)}
.ph:not(.dark) .hero-fade{background:linear-gradient(180deg,rgba(0,0,0,0) 34%,color-mix(in srgb,var(--glow) 70%,transparent) 64%,var(--glow) 100%)}
.glowbed{position:absolute;left:0;right:0;top:380px;height:420px;background:linear-gradient(180deg,var(--glow) 0%,var(--bg) 100%)}
.content{position:absolute;inset:0}
.topbar{position:absolute;left:12px;right:12px;top:30px;display:flex;justify-content:space-between;z-index:2}
.glass{width:44px;height:44px;border-radius:22px;background:rgba(14,15,17,.55);backdrop-filter:blur(10px);color:#EDEEF0;display:flex;align-items:center;justify-content:center}
.eyebrow-g{font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:var(--soft)}
.title-g{font-size:32px;font-weight:700;letter-spacing:-.02em;line-height:1.1;margin-top:6px}
.sub-g{font-size:14px;color:var(--soft);margin-top:8px;display:flex;align-items:center;gap:6px}
.pillbtn{display:flex;align-items:center;gap:8px;height:44px;padding:0 18px 0 16px;border-radius:22px;border:1.5px solid var(--outline);font-size:14px;font-weight:600}
.circbtn{width:44px;height:44px;border-radius:22px;border:1.5px solid var(--outline);display:flex;align-items:center;justify-content:center}
.fchip{height:36px;padding:0 14px;border-radius:10px;display:flex;align-items:center;font-size:13px;font-weight:600;color:var(--ink2);background:var(--surf);border:1px solid var(--line)}
.fchip.on{background:var(--priC);border-color:var(--priC);color:var(--priOn)}
.note-g{display:flex;align-items:center;gap:5px;height:44px;font-size:12.5px;color:var(--ink2)}
.note-g b{color:var(--ink);font-weight:600}
.chev{display:flex;color:var(--ink3)}
.mrow{height:72px}
.mrow .t1{font-size:14.5px}.mrow .t2{font-size:12.5px}
.place{background:var(--surf2);color:var(--ink);height:19px;font-size:10.5px}
/* 1. Raster: de vier hoogste podcasts, 2×2, zonder voegen, zoals een afspeellijst-omslag. */
.col-grid{display:grid;grid-template-columns:1fr 1fr;width:390px;height:390px}
.col-grid .art{width:195px;height:195px}
/* 2. Mozaïek: de nummer 1 groot, 2 t/m 6 eromheen: de ranglijst in één beeld. */
.col-mosaic{display:grid;grid-template-columns:repeat(3,130px);grid-template-rows:repeat(3,130px);width:390px;height:390px}
.col-mosaic .art{width:130px;height:130px}
.col-mosaic .big{grid-column:1/3;grid-row:1/3;width:260px;height:260px}
/* 3. Muur: rijen hoezen onder een hoek, voor een maker met veel podcasts. */
.col-wall{position:absolute;left:-30px;top:-50px;transform:rotate(-10deg);display:flex;flex-direction:column;gap:10px}
.wall-row{display:flex;gap:10px}
.wall-row .art{width:128px;height:128px;border-radius:14px!important}
.stamp{position:absolute;right:12px;bottom:12px;font-size:11px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;padding:5px 9px;border-radius:7px;background:rgba(14,15,17,.7);color:#EDEEF0}
/* Mini-collage in lijsten: vier hoezen in een vierkant met de radius van een hoes. */
.tile{display:grid;grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr;gap:1.5px;border-radius:9px;overflow:hidden;flex:none;box-shadow:0 1px 3px rgba(33,29,23,.14)}
.tile .art{width:auto;height:auto;border-radius:0;box-shadow:none}
.tile .art:only-child{grid-column:1/3;grid-row:1/3}
.schip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;white-space:nowrap}
.schip.dark{background:#211D17;border-color:#211D17;color:#F6EFE5}
.newtag{font-size:10px;font-weight:700;letter-spacing:.06em;padding:3px 5px;border-radius:4px;background:var(--surf3)}
.mv{width:46px}
.chartglow{position:absolute;left:0;right:0;top:0;height:560px;background:linear-gradient(180deg,#EEE6F6 0%,#EEE6F6 30%,var(--bg) 100%)}
.ph>.sa,.ph>.appbar,.ph>.body{position:relative}
`;

/** Een tegel met twee hoezen: die krijgen elk een halve kolom, over de volle hoogte. */
const fixTiles = (html) => html.replace(/<div class="tile" style="([^"]+)">((?:<div class="art [^"]+"><\/div>){2})<\/div>/g,
  (m, style, inner) => `<div class="tile" style="${style};grid-template-rows:1fr">${inner.replace(/class="art /g, 'style="grid-row:1/3" class="art ')}</div>`);

export const PHONES = [
  ['raster', 'A · Raster: de vier hoogste podcasts', makerPage(collageGrid(), true)],
  ['mozaiek', 'B · Mozaïek: #1 groot, #2–#6 eromheen', makerPage(collageMosaic(), true)],
  ['muur', 'C · Muur: rijen hoezen onder een hoek', makerPage(collageWall(), true)],
  ['mozaiek-licht', 'B in het lichte thema', makerPage(collageMosaic(), false)],
  ['lijst', 'In lijsten: vier hoezen in plaats van een rond logo', fixTiles(chartsPage())]
];

const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&display=swap">`;
const board = `<!doctype html><html><head><meta charset="utf-8">${FONTS}<style>${base}${CSS}
.board{display:flex;gap:40px;padding:40px;background:#DDD5C8;width:max-content}
.board figure{margin:0}.board figcaption{font:600 14px 'Instrument Sans',sans-serif;margin-bottom:10px}
.ph{border-radius:28px;box-shadow:0 10px 30px rgba(0,0,0,.18)}</style></head><body><div class="board">
${PHONES.map(([id, title, html]) => `<figure id="${id}"><figcaption>${title}</figcaption>${html}</figure>`).join('\n')}
</div></body></html>`;
fs.writeFileSync(path.join(dir, 'collage-preview.html'), board);
console.log('collage-preview.html', board.length);

/* ---- de pagina om te publiceren ---- */
const phoneCss = (base + CSS).replace(':root{', '.ph{').replace('body{margin:0}', '')
  .replace(/a\{color:var\(--pri\);text-decoration:none\}\s*a:hover\{color:var\(--priOn\)\}/, '');
const shot = (id, caption) => {
  const [, , html] = PHONES.find(p => p[0] === id);
  return `<figure class="shot"><div class="fit"><div class="scale">${html}</div></div><figcaption>${caption}</figcaption></figure>`;
};
const page = `<title>Makerpagina met collage</title>
${FONTS}
<style>
:root{--p-bg:#F3EEE6;--p-card:#FFFDFA;--p-ink:#211D17;--p-ink2:#5F564B;--p-line:#E0D6C6;--p-accent:#B24A22;
  --f-display:'Bricolage Grotesque','Instrument Sans',ui-sans-serif,system-ui,sans-serif;--f-body:'Instrument Sans',ui-sans-serif,system-ui,sans-serif}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--p-bg:#0E0F11;--p-card:#16171A;--p-ink:#EDEEF0;--p-ink2:#A0A3AA;--p-line:#2C2E33;--p-accent:#F0895B;color-scheme:dark}}
:root[data-theme="dark"]{--p-bg:#0E0F11;--p-card:#16171A;--p-ink:#EDEEF0;--p-ink2:#A0A3AA;--p-line:#2C2E33;--p-accent:#F0895B;color-scheme:dark}
body{background:var(--p-bg);color:var(--p-ink);font-family:var(--f-body);font-size:16px;line-height:1.55;margin:0}
.page{max-width:1180px;margin:0 auto;padding-inline:20px;padding-block:44px 72px}
h1,h2{font-family:var(--f-display);letter-spacing:-.02em;text-wrap:balance;margin:0}
h1{font-size:clamp(32px,5.5vw,52px);line-height:1.04;font-weight:800}
h2{font-size:clamp(22px,3vw,28px);font-weight:700;margin-bottom:8px}
p{margin:0}
.eyebrow-p{font-size:12px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:var(--p-accent);margin-bottom:12px}
.intro{display:grid;gap:14px;max-width:68ch;margin-bottom:36px}
.intro p,.sub{color:var(--p-ink2)}
.intro strong{color:var(--p-ink)}
.shots{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:28px;align-items:start}
.shot{margin:0;min-width:0}
.shot figcaption{font-size:13.5px;color:var(--p-ink2);margin-top:10px}
.shot figcaption b{color:var(--p-ink)}
.fit{width:100%;aspect-ratio:390/844;overflow:hidden;border-radius:24px;box-shadow:0 1px 0 var(--p-line),0 14px 34px rgba(33,29,23,.18);background:#0E0F11}
.scale{width:390px;height:844px;transform-origin:0 0}
.block{margin-top:52px;max-width:72ch}
.block ul{padding-left:20px;display:grid;gap:8px;color:var(--p-ink2)}
.block li b{color:var(--p-ink)}
${phoneCss}
.ph{border-radius:0}
</style>
<div class="page">
  <header class="intro">
    <p class="eyebrow-p">Toadcast · makers</p>
    <h1>De makerpagina zonder rond logo</h1>
    <p>Voorstel: de ronde logo's verdwijnen overal. Een maker krijgt het gezicht van zijn eigen podcasts. Op de makerpagina staat bovenaan een collage van zijn toppodcasts over de volle breedte, die wegloopt in de gloed, net als de hoes op de podcastpagina. In lijsten wordt het ronde logo een vierkantje met vier hoezen.</p>
    <p><strong>Advies: B, het mozaïek.</strong> De nummer 1 staat groot en #2 tot en met #6 staan eromheen. Zo is de kop meteen de ranglijst van de maker, en de gloed komt uit de grootste hoes, zoals bij Beeldgloed. Het werkt voor elke maker, ook zonder Apple-kanaal, en de app hoeft er niets extra's voor op te halen.</p>
  </header>
  <div class="shots">
    ${shot('mozaiek', '<b>B · Mozaïek</b>: #1 groot, #2–#6 eromheen. De gloed komt uit de hoes van #1.')}
    ${shot('raster', '<b>A · Raster</b>: de vier hoogste, 2×2, zoals een afspeellijst. Rustig, maar zonder rangorde.')}
    ${shot('muur', '<b>C · Muur</b>: rijen hoezen onder een hoek. Het meest sfeervol, maar de maker verdwijnt erin.')}
    ${shot('mozaiek-licht', '<b>B in het lichte thema</b>: dezelfde gloed, in de lichte Beeldgloed-tint.')}
    ${shot('lijst', '<b>In lijsten</b>: vier hoezen in één vierkant in plaats van een rond logo. De drie hoesjes rechts in de rij vallen dan weg.')}
  </div>
  <section class="block">
    <h2>Wat er verandert</h2>
    <ul>
      <li><b>Geen logo's meer.</b> Het ronde logo, het monogram en de kanaalkleur gaan eruit. Apple-kanalen blijven wel nodig: voor de volledige lijst shows en voor meldingen over nieuwe podcasts.</li>
      <li><b>Kop als bij een podcast.</b> Collage over de volle breedte, glazen knoppen, en daaronder eyebrow, naam, "Volg maker" en delen.</li>
      <li><b>Gloed uit de nummer 1.</b> Dezelfde Beeldgloed-rollen als op de podcastpagina, afgeleid van de hoes die het grootst in beeld staat.</li>
      <li><b>Mini-collage in lijsten.</b> Vier hoezen in een vierkant met de radius van een hoes. Het blijft herkenbaar als maker, omdat het nooit één hoes is. Makers met twee podcasts krijgen twee halve.</li>
      <li><b>Minder dan zes podcasts.</b> Het mozaïek valt terug op minder vakken: bij 2–5 podcasts het raster, bij 1 podcast de hoes zelf, zoals op de podcastpagina.</li>
    </ul>
    <p class="sub" style="margin-top:14px">Alle makers, podcasts en hoezen zijn verzonnen.</p>
  </section>
</div>
<script>
(function(){function fit(){document.querySelectorAll('.fit').forEach(function(f){var s=f.querySelector('.scale');if(s)s.style.transform='scale('+(f.clientWidth/390)+')';});}
fit();window.addEventListener('resize',fit);if(window.ResizeObserver)new ResizeObserver(fit).observe(document.body);})();
</script>
`;
fs.writeFileSync(path.join(dir, 'collage.html'), page);
console.log('collage.html', page.length);

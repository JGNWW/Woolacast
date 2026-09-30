// Vijf telefoonschermen voor de kanalen-concepten. Bouwt op design/base.css,
// in dezelfde stijl als de mockups van Trending en Nieuw.
// Alle kanalen, podcasts en plekken hieronder zijn verzonnen.

export const ICON = {
  back: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"></path></svg>`,
  share: `<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v11"></path><path d="M8 8l4-4 4 4"></path><path d="M6 13v6h12v-6"></path></svg>`,
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20.5 20.5L16.6 16.6"></path></svg>`,
  bell: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"></path><path d="M13.7 20a2 2 0 01-3.4 0"></path></svg>`,
  chevS: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>`,
  chevR: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"></path></svg>`,
  play: `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.5v13l10.5-6.5z"></path></svg>`,
  pause: `<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="5" width="3.6" height="14" rx="1.2"></rect><rect x="13.4" y="5" width="3.6" height="14" rx="1.2"></rect></svg>`,
  fwd: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M5 12h14"></path><path d="M14 7l5 5-5 5"></path></svg>`,
  check: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"></path></svg>`,
  plus: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>`,
  quote: `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M4 18v-5.5C4 8.4 6.2 6 10 5.5v3C8.2 9 7.3 10.3 7.2 12H10v6zm10 0v-5.5c0-4.1 2.2-6.5 6-7v3c-1.8.5-2.7 1.8-2.8 3.5H20v6z"></path></svg>`,
  navCharts: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 20v-7"></path><path d="M12 20V4"></path><path d="M19 20v-11"></path></svg>`,
  navDisc: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"></circle><path d="M15 9l-2 4.2L9 15l2-4.2z"></path></svg>`,
  navLib: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5v14"></path><path d="M9.5 5v14"></path><path d="M14.5 6.2l5 12.8"></path></svg>`
};
const UP = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>`;
const DOWN = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"></path></svg>`;
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

// Verzonnen kanalen: naam, kleur, beeldmerk.
export const CH = {
  kade: { name: 'Kade Media', color: '#14504E', mark: 'KADE' },
  noord: { name: 'Dagblad Noord', color: '#2B4C7E', mark: 'DN' },
  hemel: { name: 'Studio Hemel', color: '#7E3149', mark: 'SH' },
  podium: { name: 'Podium Audio', color: '#1E1B16', mark: 'PA' },
  concept: { name: 'Concept Media', color: '#A2532F', mark: 'CM' },
  kelder: { name: 'Kelderwerk', color: '#332F63', mark: 'KW' }
};

const logo = (c, size = 44, r = 12) =>
  `<div class="logo" style="width:${size}px;height:${size}px;border-radius:${r}px;background:${c.color};font-size:${Math.round(size * (c.mark.length > 2 ? .22 : .34))}px">${c.mark}</div>`;

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
    <div class="navi ${active === 'library' ? 'on' : ''}">${ICON.navLib}Bibliotheek</div>
  </div>
  <div class="gest"><i></i></div>`;

const titlebar = (right = ['share']) => `
  <div class="appbar" style="padding-left:6px">
    <div class="ibtn">${ICON.back}</div>
    <div class="row" style="gap:0">${right.map(a => `<div class="ibtn">${ICON[a]}</div>`).join('')}</div>
  </div>`;

const appbar = (actions) => `
  <div class="appbar">
    <div class="mark">Toadcast</div>
    <div class="row" style="gap:0">${actions.map(a => `<div class="ibtn">${ICON[a]}</div>`).join('')}</div>
  </div>`;

// De kanaalkop: één component, gedeeld door de kanaalpagina, de bronvergelijking en de tipgever.
const channelHead = (c, kind, stat, following = false) => `
    <div class="chead" style="--ch:${c.color}">
      <div class="pad row" style="gap:14px;align-items:center">
        ${logo(c, 64, 16)}
        <div class="meta" style="gap:3px">
          <div class="eyebrow">${kind}</div>
          <h1 class="h1 dsp" style="font-size:26px">${c.name}</h1>
          <div class="t2">${stat}</div>
        </div>
      </div>
      <div class="pad row" style="gap:8px;margin-top:14px">
        ${following
          ? `<div class="btn btn-t" style="height:40px;padding:0 16px">${ICON.check}Volgt</div>`
          : `<div class="btn btn-p" style="height:40px;padding:0 16px">${ICON.plus}Volg kanaal</div>`}
        <div class="btn btn-o" style="height:40px;padding:0 14px">${ICON.play}Nieuwste</div>
      </div>
    </div>`;

const segs = (items, on) => `
    <div class="pad segs">${items.map(t => `<div class="seg ${t === on ? 'on' : ''}">${t}</div>`).join('')}</div>`;

export const CSS = `
.logo{display:flex;align-items:center;justify-content:center;flex:none;color:#FFFDFA;font-weight:800;letter-spacing:.04em;
  font-family:'Bricolage Grotesque','Instrument Sans',sans-serif;box-shadow:0 1px 3px rgba(33,29,23,.16)}
.chead{padding:6px 0 16px;background:linear-gradient(180deg,color-mix(in srgb,var(--ch) 13%,var(--bg)) 0%,var(--bg) 100%)}
.segs{display:flex;gap:6px;padding-top:12px;padding-bottom:4px}
.seg{height:36px;padding:0 13px;border-radius:10px;display:flex;align-items:center;font-size:13px;font-weight:600;
  color:var(--ink2);background:var(--surf);border:1px solid var(--line);white-space:nowrap}
.seg.on{background:var(--ink);border-color:var(--ink);color:#FBF6EE}
.tnote{display:flex;align-items:center;gap:6px;min-height:30px;font-size:11.5px;color:var(--ink3)}
.lrow.c{height:60px}
.lrow.c .art{width:44px;height:44px;border-radius:9px}
.lrow.c .t1{font-size:14px}.lrow.c .t2{font-size:12px}
.place{display:flex;align-items:center;gap:5px;height:24px;padding:0 8px 0 5px;border-radius:7px;background:var(--surf2);
  font-size:12px;font-weight:700;font-variant-numeric:tabular-nums;color:var(--ink);flex:none}
.place.none{background:transparent;color:var(--ink3);font-weight:600;padding:0 2px}
.mini-chip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap}
.mini-chip.on{background:var(--ink);border-color:var(--ink);color:#FBF6EE}
.share{height:4px;border-radius:2px;background:var(--surf3);overflow:hidden;margin-top:5px}
.share i{display:block;height:4px;background:var(--pri);border-radius:2px}
.covers{display:flex;flex:none}
.covers .art{width:24px;height:24px;border-radius:6px;margin-left:-6px;box-shadow:0 0 0 2px var(--bg)}
.grid{display:grid;grid-template-columns:1fr 54px 54px 54px;align-items:center;column-gap:4px}
.grid .h{font-size:10.5px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:var(--ink3);text-align:center;line-height:1.2}
.cell{height:30px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;
  font-variant-numeric:tabular-nums;background:var(--surf2);color:var(--ink)}
.cell.hi{background:var(--priC);color:var(--priOn)}
.cell.no{background:transparent;color:var(--ink3);font-weight:500}
.srccard{flex:1 1 0;min-width:0;padding:10px 11px;border-radius:14px;background:var(--surf);border:1px solid var(--line2)}
.srccard .n{font-size:22px;font-weight:800;letter-spacing:-.03em;font-variant-numeric:tabular-nums;line-height:1.1}
.srccard .l{font-size:11px;color:var(--ink2);font-weight:600}
.day{font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;color:var(--ink3);padding:12px 0 2px}
.chrow{display:flex;gap:14px;overflow:hidden;padding-top:4px;padding-bottom:12px}
.chitem{display:flex;flex-direction:column;align-items:center;gap:6px;width:60px;flex:none;font-size:11px;font-weight:600;color:var(--ink2);text-align:center;line-height:1.2}
.chitem .dot{width:8px;height:8px;border-radius:4px;background:var(--pri);position:absolute;top:-2px;right:-2px;box-shadow:0 0 0 2px var(--bg)}
.chlabel{display:flex;align-items:center;gap:5px;font-size:11.5px;color:var(--ink2);font-weight:600}
.chlabel i{width:10px;height:10px;border-radius:3px;display:block;flex:none}
.alert{display:flex;gap:12px;align-items:center;padding:12px;border-radius:14px;background:var(--priC);color:var(--priOn)}
.tip{padding:12px 0;border-bottom:1px solid var(--line2);display:flex;gap:12px}
.tip .q{font-size:12.5px;color:var(--ink2);line-height:1.4;margin-top:4px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.tip .w{font-size:11.5px;color:var(--ink3);margin-top:6px;display:flex;gap:8px;align-items:center}
`;

export const screens = {};

/* ---- 1. De kanaalpagina (Apple-kanaal + YouTube-sortering) ---- */
const kadeShows = [
  ['Kade 12', 'a10', 4, 'A', 'Nieuwe afl. vandaag · True crime'],
  ['Nachtdienst', 'a2', 19, 'A', 'Nieuwe afl. gisteren · Samenleving'],
  ['Lange Adem', 'a5', 37, 'A', 'Nieuwe afl. 3 dagen geleden · Sport'],
  ['Tafel voor Twee', 'a3', 88, 'A', 'Nieuwe afl. gisteren · Eten'],
  ['Koud Spoor', 'a7', 140, 'A', 'Nieuwe afl. vorige week · True crime'],
  ['Zaterdagavond Thuis', 'a8', 171, 'A', 'Nieuwe afl. 2 dagen geleden · Comedy']
];
screens.Kanaal = { title: '1 · De kanaalpagina', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar(['share'])}
  <div class="body">
    ${channelHead(CH.kade, 'Kanaal', '31 podcasts · 6 in de Top 200')}
    ${segs(['Populair', 'Recent', 'Nieuw', 'A–Z'], 'Populair')}
    <div class="pad tnote">Hoogste plek in de hitlijst · Apple · ${FLAG} NL · alle categorieën</div>
    <div class="pad list">
      ${kadeShows.map(s => `
      <div class="lrow c">
        <div class="art ${s[1]}"></div>
        <div class="meta"><div class="t1">${s[0]}</div><div class="t2">${s[4]}</div></div>
        <span class="place">#${s[2]}</span>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 2. Makers als hitlijst ---- */
const makers = [
  [CH.kade, 24, 2, 'up', 2, ['a10', 'a2', 'a5']],
  [CH.noord, 11, 6, 'flat', 0, ['a9', 'a4', 'a6']],
  [CH.hemel, 9, 1, 'up', 1, ['a5', 'a3', 'a8']],
  [CH.concept, 7, 12, 'down', 1, ['a8', 'a1', 'a7']],
  [CH.podium, 5, 23, 'flat', 0, ['a7', 'a2', 'a10']],
  [CH.kelder, 4, 9, 'up', 3, ['a4', 'a6', 'a3']]
];
screens.Makers = { title: '2 · Makers als hitlijst', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px">
      <h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1>
      <span class="note">Bijgewerkt 06:00</span>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="mini-chip on">Apple Podcasts ${ICON.chevS}</div>
      <div class="mini-chip">${FLAG}NL ${ICON.chevS}</div>
      <div class="mini-chip">Categorie ${ICON.chevS}</div>
    </div>
    <div class="pad tabs" style="height:40px;gap:14px">
      ${['Podcasts', 'Afleveringen', 'Trending', 'Nieuw', 'Makers'].map(t => `<div class="tab ${t === 'Makers' ? 'on' : ''}" style="font-size:13.5px;height:40px">${t}</div>`).join('')}
    </div>
    <div class="hr"></div>
    <div class="pad tnote">Wie de meeste podcasts in de Top 200 heeft · Apple · NL</div>
    <div class="pad list">
      ${makers.map((m, i) => `
      <div class="lrow" style="height:66px">
        <div class="rank ${i < 3 ? 'top' : ''}" style="font-size:16px;width:22px">${i + 1}</div>
        ${logo(m[0], 44, 12)}
        <div class="meta">
          <div class="t1">${m[0].name}</div>
          <div class="t2">${m[1]} in de lijst · hoogste #${m[2]}</div>
          <div class="share"><i style="width:${m[1] / 24 * 100}%"></i></div>
        </div>
        <div class="covers">${m[5].map(a => `<div class="art ${a}"></div>`).join('')}</div>
        <div class="mv ${m[3] === 'flat' ? 'flat' : ''}" style="width:30px;color:${m[3] === 'up' ? 'var(--up)' : m[3] === 'down' ? 'var(--down)' : ''}">${m[3] === 'up' ? UP + m[4] : m[3] === 'down' ? DOWN + m[4] : '='}</div>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 3. Eén kanaal per bron ---- */
const perSource = [
  ['Kade 12', 'a10', 4, 11, 2],
  ['Nachtdienst', 'a2', 19, 7, null],
  ['Lange Adem', 'a5', 37, null, 58],
  ['Tafel voor Twee', 'a3', 88, 42, null],
  ['Koud Spoor', 'a7', 140, null, null],
  ['Halve Zolen', 'a9', null, 96, null]
];
const cell = (v) => v == null ? `<div class="cell no">—</div>` : `<div class="cell ${v <= 10 ? 'hi' : ''}">${v}</div>`;
screens.PerBron = { title: '3 · Eén kanaal per bron', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar(['share'])}
  <div class="body">
    ${channelHead(CH.kade, 'Kanaal', '31 podcasts', true)}
    ${segs(['Podcasts', 'Per bron', 'Afleveringen'], 'Per bron')}
    <div class="pad row" style="gap:8px;padding-top:10px;padding-bottom:12px">
      <div class="srccard"><div class="n">6</div><div class="l">Apple · NL</div></div>
      <div class="srccard"><div class="n">4</div><div class="l">Spotify · NL</div></div>
      <div class="srccard"><div class="n">2</div><div class="l">Apple · BE</div></div>
    </div>
    <div class="pad">
      <div class="grid" style="padding-bottom:6px">
        <div class="h" style="text-align:left">Plek in de Top 200</div><div class="h">Apple<br>NL</div><div class="h">Spotify<br>NL</div><div class="h">Apple<br>BE</div>
      </div>
      ${perSource.map(s => `
      <div class="grid" style="height:52px;border-top:1px solid var(--line2)">
        <div class="row" style="gap:10px;min-width:0"><div class="art ${s[1]}" style="width:36px;height:36px;border-radius:8px"></div><div class="t1" style="font-size:13.5px">${s[0]}</div></div>
        ${cell(s[2])}${cell(s[3])}${cell(s[4])}
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 4. Kanalen volgen: nieuw van je kanalen ---- */
const followed = [[CH.kade, true], [CH.noord, true], [CH.hemel, false], [CH.kelder, false]];
const feed = [
  ['Vandaag', [
    ['De tip die alles veranderde', 'Kade 12', CH.kade, 'a10', '48 min'],
    ['Het kabinet en de klok', 'Vandaag in Zeven', CH.noord, 'a9', '22 min']
  ]],
  ['Gisteren', [
    ['Zeven uur in de nacht', 'Nachtdienst', CH.kade, 'a2', '41 min'],
    ['Wat eten we op zondag', 'Tafel voor Twee', CH.kade, 'a3', '35 min']
  ]]
];
screens.Volgen = { title: '4 · Kanalen volgen', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search'])}
  <div class="body">
    <div class="pad" style="padding-bottom:10px"><h1 class="h1 dsp" style="font-size:26px">Bibliotheek</h1></div>
    <div class="pad tabs" style="height:40px;gap:18px">
      ${['Podcasts', 'Kanalen', 'Wachtrij', 'Meldingen'].map(t => `<div class="tab ${t === 'Kanalen' ? 'on' : ''}" style="font-size:14px;height:40px">${t}</div>`).join('')}
    </div>
    <div class="hr"></div>
    <div class="pad chrow" style="padding-top:14px">
      ${followed.map(([c, dot]) => `<div class="chitem"><div style="position:relative">${logo(c, 52, 14)}${dot ? '<i class="dot"></i>' : ''}</div>${c.name}</div>`).join('')}
    </div>
    <div class="pad"><div class="alert">
      ${logo(CH.noord, 40, 11)}
      <div class="meta"><div class="t1" style="font-size:13.5px">Nieuwe podcast van Dagblad Noord</div><div class="t2" style="color:var(--priOn)">Kort Lontje · 3 afleveringen</div></div>
      <div class="btn btn-p" style="height:36px;padding:0 12px;font-size:13px">Bekijk</div>
    </div></div>
    <div class="pad">
      ${feed.map(([day, eps]) => `
      <div class="day">${day}</div>
      ${eps.map(e => `
      <div class="erow" style="padding:11px 0">
        <div class="eart ${e[3]}" style="width:48px;height:48px;border-radius:10px"></div>
        <div class="ebody">
          <div class="etitle" style="font-size:14px">${e[0]}</div>
          <div class="emeta" style="margin-top:5px"><span class="chlabel"><i style="background:${e[2].color}"></i>${e[2].name}</span>· ${e[1]} · ${e[4]}</div>
        </div>
        <div class="pbtn" style="margin-top:2px">${ICON.play}</div>
      </div>`).join('')}`).join('')}
    </div>
  </div>
  ${chrome('library')}
</div>` };

/* ---- 5. Een tipgever als kanaal ---- */
const tips = [
  ['Halve Zolen', 'a9', '“Twee broers die elke week een schoenmaker bellen, en het wordt steeds beter.”', '2 dagen geleden', 2],
  ['Lange Adem', 'a5', '“De beste sportpodcast van dit najaar gaat over verliezen.”', 'vorige week', 0],
  ['Ondergronds', 'a4', '“Rustig, precies en nergens sensatie: zo hoort true crime te klinken.”', '12 sep', 4]
];
screens.Tipgever = { title: '5 · De tipgever als kanaal', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar(['share'])}
  <div class="body">
    ${channelHead(CH.noord, 'Krant · tipt en maakt', '23 tips sinds juni · 11 eigen podcasts')}
    ${segs(['Tipt', 'Maakt'], 'Tipt')}
    <div class="pad tnote">Nieuwste tips eerst · uit de podcastrubriek</div>
    <div class="pad">
      ${tips.map(t => `
      <div class="tip">
        <div class="art ${t[1]}" style="width:52px;height:52px;border-radius:11px"></div>
        <div class="ebody">
          <div class="t1">${t[0]}</div>
          <div class="q">${t[2]}</div>
          <div class="w">${t[3]}${t[4] ? ` · ook getipt door ${t[4]} anderen` : ''}</div>
        </div>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('discover')}
</div>` };

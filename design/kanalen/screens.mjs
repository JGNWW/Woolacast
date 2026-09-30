// Telefoonschermen voor de makers-concepten ("kanalen"). Bouwt op design/base.css
// en volgt de componenten van de app: FilterChipBox, SmallChip, UnderlineTabs,
// WoolButton, AlertCard, TipRow en de rijen van MakerScreen.
// Alle makers, podcasts en plekken hieronder zijn verzonnen.

export const ICON = {
  back: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"></path></svg>`,
  share: `<svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v11"></path><path d="M8 8l4-4 4 4"></path><path d="M6 13v6h12v-6"></path></svg>`,
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20.5 20.5L16.6 16.6"></path></svg>`,
  bell: (c = 'currentColor', s = 22) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"></path><path d="M13.7 20a2 2 0 01-3.4 0"></path></svg>`,
  chevS: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>`,
  chevR: (s = 18) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"></path></svg>`,
  play: `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.5v13l10.5-6.5z"></path></svg>`,
  pause: `<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="5" width="3.6" height="14" rx="1.2"></rect><rect x="13.4" y="5" width="3.6" height="14" rx="1.2"></rect></svg>`,
  fwd: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M5 12h14"></path><path d="M14 7l5 5-5 5"></path></svg>`,
  plus: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>`,
  info: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"></circle><path d="M12 11v5"></path><path d="M12 7.6v.2"></path></svg>`,
  navCharts: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 20v-7"></path><path d="M12 20V4"></path><path d="M19 20v-11"></path></svg>`,
  navDisc: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"></circle><path d="M15 9l-2 4.2L9 15l2-4.2z"></path></svg>`,
  navLib: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5v14"></path><path d="M9.5 5v14"></path><path d="M14.5 6.2l5 12.8"></path></svg>`
};
const UP = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>`;
const DOWN = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"></path></svg>`;
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

// Verzonnen makers. `channel` = er is een Apple-kanaal (logo en kleur uit de verzamelaar);
// zonder kanaal is er alleen een naam, en dus een neutraal monogram.
export const M = {
  kade: { name: 'Kade Media', color: '#14504E', mark: 'KADE', channel: true },
  noord: { name: 'Dagblad Noord', color: '#2B4C7E', mark: 'DN', channel: true },
  oost: { name: 'Radio Oost', mark: 'RO', channel: false },
  hemel: { name: 'Studio Hemel', color: '#7E3149', mark: 'SH', channel: true },
  concept: { name: 'Concept Media', color: '#A2532F', mark: 'CM', channel: true },
  podium: { name: 'Podium Audio', color: '#1E1B16', mark: 'PA', channel: true },
  kelder: { name: 'Kelderwerk', color: '#332F63', mark: 'KW', channel: true },
  zuid: { name: 'Zuidkust Audio', mark: 'ZA', channel: false }
};

// Makerlogo: altijd rond, zodat een maker nooit op een podcasthoes lijkt.
const logo = (m, size = 44) => m.channel
  ? `<div class="logo" style="width:${size}px;height:${size}px;background:${m.color};font-size:${Math.round(size * (m.mark.length > 2 ? .22 : .34))}px">${m.mark}</div>`
  : `<div class="logo mono" style="width:${size}px;height:${size}px;font-size:${Math.round(size * .34)}px">${m.mark}</div>`;

const chrome = (active) => `
  <div class="mini">
    <div class="art a1"></div>
    <div class="meta">
      <div class="t1 on-mini">Live vanuit Paradiso</div>
      <div class="t2 on-mini2">De Deadline · nog 18 min</div>
    </div>
    <div class="ibtn on-mini">${ICON.pause}</div>
    <div class="ibtn on-mini">${ICON.fwd}</div>
  </div>
  <div class="nav">
    <div class="navi ${active === 'charts' ? 'on' : ''}">${ICON.navCharts}Hitlijsten</div>
    <div class="navi ${active === 'discover' ? 'on' : ''}">${ICON.navDisc}Ontdek</div>
    <div class="navi ${active === 'library' ? 'on' : ''}">${ICON.navLib}Bibliotheek</div>
  </div>
  <div class="gest"><i></i></div>`;

const titlebar = (right = []) => `
  <div class="appbar" style="padding-left:6px">
    <div class="ibtn">${ICON.back}</div>
    <div class="row" style="gap:0">${right.map(a => `<div class="ibtn">${ICON[a]}</div>`).join('')}</div>
  </div>`;

const appbar = (actions) => `
  <div class="appbar">
    <div class="mark">Toadcast</div>
    <div class="row" style="gap:0">${actions.map(a => `<div class="ibtn">${typeof ICON[a] === 'function' ? ICON[a]() : ICON[a]}</div>`).join('')}</div>
  </div>`;

const chips = (items, on) => `<div class="pad row" style="gap:8px">${items.map(t => `<div class="fchip ${t === on ? 'on' : ''}">${t}</div>`).join('')}</div>`;
const tabs = (items, on) => `
    <div class="pad tabs">${items.map(t => `<div class="tab ${t === on ? 'on' : ''}">${t}</div>`).join('')}</div>
    <div class="hr"></div>`;
const label = (t) => `<div class="pad lbl2">${t}</div>`;

export const CSS = `
.logo{display:flex;align-items:center;justify-content:center;flex:none;border-radius:50%;color:#FFFDFA;font-weight:800;letter-spacing:.04em;
  font-family:'Bricolage Grotesque','Instrument Sans',sans-serif;box-shadow:inset 0 0 0 1px var(--ring)}
.logo.mono{background:var(--surf3);color:var(--ink2)}
.ph{--ring:rgba(33,29,23,.10);--tnote:var(--ink2);--panel:#211D17;--onPanel:#F6EFE5;--onPanelMuted:#B3A695;--rank:var(--pri)}
.on-mini{color:var(--onPanel)}.on-mini2{color:var(--onPanelMuted)}
/* De gloed loopt tot boven in de statusbalk (fullBleed), op 8% van de kanaalkleur. */
.glow{position:absolute;left:0;right:0;top:0;height:330px;pointer-events:none;
  background:linear-gradient(180deg,color-mix(in srgb,var(--ch) 8%,var(--bg)) 0%,color-mix(in srgb,var(--ch) 8%,var(--bg)) 45%,var(--bg) 100%)}
.ph>*{position:relative}
.ph>.glow{position:absolute}
.fchip{height:36px;padding:0 14px;border-radius:10px;display:flex;align-items:center;font-size:13px;font-weight:600;
  color:var(--ink2);background:var(--surf);border:1px solid var(--line);white-space:nowrap;flex:none}
.fchip.on{background:var(--priC);border-color:var(--priC);color:var(--priOn)}
.schip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap;flex:none}
.schip.dark{background:var(--panel);border-color:var(--panel);color:var(--onPanel)}
.schip svg{color:var(--ink2)}.schip.dark svg{color:var(--onPanelMuted)}
.tnote{display:flex;align-items:center;gap:6px;min-height:36px;font-size:12.5px;color:var(--tnote)}
.tnote b{font-weight:600;color:var(--ink);display:inline-flex;align-items:center;gap:5px;white-space:nowrap}
.tnote .chev{display:flex;color:var(--ink2)}
.place{display:flex;align-items:center;height:26px;padding:0 8px;border-radius:7px;background:var(--surf2);
  font-size:12.5px;font-weight:700;font-variant-numeric:tabular-nums;color:var(--ink);flex:none}
.place.none{background:transparent;color:var(--ink2);font-weight:600}
.chev{color:var(--ink2);display:flex;flex:none}
.covers{display:flex;flex:none;padding-left:6px}
.covers .art{width:26px;height:26px;border-radius:6px;margin-left:-6px;box-shadow:0 0 0 2px var(--bg)}
.btn-o{border-color:var(--line)}
.panel{background:var(--panel);border-radius:16px;padding:15px 15px 8px}
.panel .ph-t{font-size:14px;font-weight:700;color:var(--onPanel)}
.panel .ph-s{font-size:11.5px;font-weight:600;color:var(--onPanelMuted)}
.prow{display:flex;align-items:center;gap:11px;height:52px;border-top:1px solid rgba(246,239,229,.13)}
.prow .t1{font-size:13.5px;color:var(--onPanel)}.prow .t2{font-size:11.5px;color:var(--onPanelMuted)}
.pill.new{background:rgba(240,137,91,.2);color:#F0895B;letter-spacing:.04em}
.tip{padding:13px 0;border-bottom:1px solid var(--line2);display:flex;gap:12px;align-items:flex-start}
.tip .src{display:flex;align-items:center;gap:7px;font-size:11.5px;color:var(--ink2);font-weight:700;height:auto;padding:0;border:0;background:none}
.tip .src span{font-weight:500}
.omark{width:18px;height:18px;border-radius:5px;display:flex;align-items:center;justify-content:center;font-size:8.5px;font-weight:800;color:#FFFDFA;flex:none}
.tip .hl{font-size:12.5px;color:var(--ink2);line-height:17px;margin-top:3px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.tip .rd{font-size:12px;font-weight:700;color:var(--pri);margin-top:6px;display:flex;align-items:center;gap:4px}
.linkcard{display:flex;align-items:center;gap:12px;padding:12px 12px 12px 14px;border-radius:16px;background:var(--surf);border:1px solid var(--line2)}
.slope text{font-family:'Instrument Sans',sans-serif}

/* Donker: de tokens van Beeldgloed (Color.kt), niet het oude bruin. */
.ph.dark{--bg:#0E0F11;--surf:#16171A;--surf2:#1E1F23;--surf3:#28292E;--line:#3A3C42;--line2:rgba(237,238,240,.08);
  --ink:#EDEEF0;--ink2:#A0A3AA;--ink3:#A0A3AA;--pri:#F0895B;--priInk:#2A1006;--priC:#34363C;--priOn:#F0895B;
  --up:#5BCB8F;--down:#FF7A93;--ring:rgba(237,238,240,.16);--panel:#28292E;--onPanel:#EDEEF0;--onPanelMuted:#A0A3AA;--rank:#EDEEF0;
  background:var(--bg);color:var(--ink)}
.ph.dark .mini{background:var(--surf3);box-shadow:0 6px 20px rgba(0,0,0,.45)}
.ph.dark .btn-p{color:#2A1006}
.ph.dark .art{box-shadow:0 1px 3px rgba(0,0,0,.5)}
.ph.dark .glow{background:linear-gradient(180deg,color-mix(in srgb,var(--ch) 16%,var(--bg)) 0%,color-mix(in srgb,var(--ch) 16%,var(--bg)) 40%,var(--bg) 100%)}
`;

export const screens = {};

/* ---- 1. De makerpagina ---- */
const makerHead = (m, count, following) => `
    <div class="pad row" style="gap:14px;align-items:center;padding-top:4px">
      ${logo(m, 64)}
      <div class="meta" style="gap:2px">
        <div class="eyebrow" style="color:var(--ink2)">Maker</div>
        <h1 class="h1 dsp" style="font-size:26px">${m.name}</h1>
        <div class="t2" style="color:var(--ink);font-size:12.5px">${count}</div>
      </div>
    </div>
    <div class="pad row" style="gap:8px;margin-top:14px">
      ${following
        ? `<div class="btn btn-t">Volgt</div>`
        : `<div class="btn btn-p">${ICON.plus}Volg maker</div>`}
    </div>`;

const row = (s, place) => `
      <div class="lrow">
        <div class="art ${s[1]}"></div>
        <div class="meta"><div class="t1">${s[0]}</div><div class="t2">${s[2]}</div></div>
        ${place}
        <span class="chev">${ICON.chevR()}</span>
      </div>`;

const kadeIn = [
  ['Kade 12', 'a10', 'Nieuwe afl. vandaag · True crime', 4],
  ['Nachtdienst', 'a2', 'Nieuwe afl. gisteren · Samenleving', 19],
  ['Lange Adem', 'a5', 'Nieuwe afl. 3 dagen geleden · Sport', 37],
  ['Tafel voor Twee', 'a3', 'Nieuwe afl. gisteren · Eten', 88]
];
const kadeOut = [
  ['Halve Zolen', 'a9', 'Nieuwe afl. 2 dagen geleden · Comedy']
];
screens.Maker = { title: '1 · Makerpagina (Apple-kanaal)', ch: M.kade.color, body: `
<div class="ph col" style="--ch:${M.kade.color}">
  <div class="glow"></div>
  <div class="sa"></div>
  ${titlebar(['share'])}
  <div class="body">
    ${makerHead(M.kade, '31 podcasts', false)}
    <div style="height:14px"></div>
    ${chips(['Populair', 'Recent', 'A–Z'], 'Populair')}
    <div class="pad tnote">Plek in <b>Apple · ${FLAG} NL</b> · alle categorieën <span class="chev">${ICON.chevS}</span></div>
    <div class="pad list">
      ${kadeIn.map(s => row(s, `<span class="place" aria-label="plek ${s[3]} in Apple NL">#${s[3]}</span>`)).join('')}
    </div>
    <div class="pad lbl2" style="color:var(--ink2)">Niet in de Top 200 · op nieuwste aflevering</div>
    <div class="pad list">
      ${kadeOut.map(s => row(s, `<span class="place none">—</span>`)).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

const oostShows = [
  ['Ochtendspits', 'a6', 'Nieuwe afl. vandaag · Nieuws', 9],
  ['Het Oosten Vertelt', 'a4', 'Nieuwe afl. gisteren · Geschiedenis', 64],
  ['Derby', 'a8', 'Nieuwe afl. gisteren · Sport', 131]
];
screens.MakerDonker = { title: '1 · Maker zonder kanaal, donker', dark: true, body: `
<div class="ph col dark">
  <div class="sa"></div>
  ${titlebar([])}
  <div class="body">
    ${makerHead(M.oost, '12 gevonden in de Apple-catalogus', true)}
    <div style="height:14px"></div>
    ${chips(['Populair', 'Recent', 'A–Z'], 'Populair')}
    <div class="pad tnote">Plek in <b>Spotify · ${FLAG} NL</b> · Top 200 <span class="chev">${ICON.chevS}</span></div>
    <div class="pad list">
      ${oostShows.map(s => row(s, `<span class="place" aria-label="plek ${s[3]} in Spotify NL">#${s[3]}</span>`)).join('')}
    </div>
    <div class="pad lbl2" style="color:var(--ink2)">Niet in de Top 200 · op nieuwste aflevering</div>
    <div class="pad list">
      ${row(['Oost aan Tafel', 'a3', 'Nieuwe afl. vorige week · Eten'], `<span class="place none">—</span>`)}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 2. Makers in de hitlijst ---- */
const makers = [
  [M.kade, 24, 2, 'up', 2, ['a10', 'a2', 'a5']],
  [M.noord, 11, 6, 'flat', 0, ['a9', 'a4', 'a6']],
  [M.oost, 9, 9, 'up', 1, ['a6', 'a4', 'a8']],
  [M.hemel, 9, 1, 'down', 1, ['a5', 'a3', 'a8']],
  [M.concept, 7, 12, 'down', 2, ['a8', 'a1', 'a7']],
  [M.podium, 5, 23, 'new', 0, ['a7', 'a2', 'a10']]
];
const mv = (d, n) => d === 'up' ? `<div class="mv" style="color:var(--up)">${UP}${n}</div>`
  : d === 'down' ? `<div class="mv" style="color:var(--down)">${DOWN}${n}</div>`
  : d === 'new' ? `<div class="mv new">NIEUW</div>` : `<div class="mv flat" style="color:var(--ink2)">=</div>`;
screens.Makers = { title: '2 · Makers in de hitlijst', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px">
      <h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1>
      <span class="note" style="color:var(--ink2)">Bijgewerkt 06:00</span>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="schip dark">Apple Podcasts ${ICON.chevS}</div>
      <div class="schip">${FLAG}NL ${ICON.chevS}</div>
      <div class="schip">Categorie ${ICON.chevS}</div>
    </div>
    ${tabs(['Podcasts', 'Afleveringen', 'Trending', 'Nieuw'], 'Podcasts')}
    <div style="height:12px"></div>
    ${chips(['Per show', 'Per maker'], 'Per maker')}
    <div class="pad tnote">Makers met 2+ shows · plek t.o.v. vorige week <span class="chev">${ICON.info}</span></div>
    <div class="pad list">
      ${makers.map((m, i) => `
      <div class="lrow">
        <div class="rank ${i < 3 ? 'top' : ''}">${i + 1}</div>
        ${logo(m[0], 44)}
        <div class="meta">
          <div class="t1">${m[0].name}</div>
          <div class="t2">${m[1]} shows · hoogste #${m[2]}</div>
        </div>
        <div class="covers">${m[5].map(a => `<div class="art ${a}"></div>`).join('')}</div>
        ${mv(m[3], m[4])}
      </div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 3. Makers: Apple tegenover Spotify ---- */
// [maker, plek bij Apple, plek bij Spotify]; null = niet in die lijst.
const pairs = [
  [M.kade, 1, 2], [M.noord, 2, 1], [M.oost, 3, 5], [M.hemel, 4, 7],
  [M.concept, 5, 3], [M.podium, 6, null], [M.kelder, 7, 4], [M.zuid, null, 6]
];
const SEL = M.hemel;
const slope = () => {
  const W = 350, top = 34, step = 30, n = 8; // plek 8 = "niet in de lijst"
  const y = (r) => top + ((r ?? n) - 1) * step;
  const xl = 128, xr = 222;
  const lines = pairs.map(([m, a, s]) => {
    const on = m === SEL;
    return `<line x1="${xl}" y1="${y(a)}" x2="${xr}" y2="${y(s)}" stroke="${on ? 'var(--pri)' : 'var(--ink3)'}" stroke-width="${on ? 3 : 1.5}" ${a == null || s == null ? 'stroke-dasharray="4 4"' : ''} stroke-linecap="round"></line>`;
  });
  const dots = pairs.flatMap(([m, a, s]) => {
    const on = m === SEL, f = on ? 'var(--pri)' : 'var(--ink2)';
    return [
      a != null ? `<circle cx="${xl}" cy="${y(a)}" r="${on ? 5 : 4}" fill="${f}"></circle>` : '',
      s != null ? `<circle cx="${xr}" cy="${y(s)}" r="${on ? 5 : 4}" fill="${f}"></circle>` : ''
    ];
  });
  const lab = (m, r, side) => {
    if (r == null) return '';
    const on = m === SEL;
    const x = side === 'l' ? xl - 12 : xr + 12;
    return `<text x="${x}" y="${y(r) + 4.5}" text-anchor="${side === 'l' ? 'end' : 'start'}" font-size="12.5" font-weight="${on ? 700 : 500}" fill="${on ? 'var(--ink)' : 'var(--ink2)'}"><tspan font-weight="700" fill="var(--ink)">${r}</tspan>  ${m.name}</text>`;
  };
  const labels = pairs.flatMap(([m, a, s]) => [lab(m, a, 'l'), lab(m, s, 'r')]);
  return `<svg class="slope" width="${W}" height="${y(n) + 16}" viewBox="0 0 ${W} ${y(n) + 16}" role="img" aria-label="Plek van makers bij Apple en bij Spotify">
    <text x="${xl}" y="14" text-anchor="end" font-size="11" font-weight="700" letter-spacing="1.3" fill="var(--ink2)">APPLE</text>
    <text x="${xr}" y="14" text-anchor="start" font-size="11" font-weight="700" letter-spacing="1.3" fill="var(--ink2)">SPOTIFY</text>
    <line x1="0" y1="${y(n) - step / 2}" x2="${W}" y2="${y(n) - step / 2}" stroke="var(--line)" stroke-width="1"></line>
    <text x="${xl - 12}" y="${y(n) + 4.5}" text-anchor="end" font-size="12.5" fill="var(--ink2)">Niet in de lijst</text>
    <text x="${xr + 12}" y="${y(n) + 4.5}" text-anchor="start" font-size="12.5" fill="var(--ink2)">Niet in de lijst</text>
    ${lines.join('')}${dots.join('')}${labels.join('')}
  </svg>`;
};
screens.Vergelijk = { title: '3 · Makers: Apple tegenover Spotify', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar(['share'])}
  <div class="body">
    <div class="pad" style="padding-bottom:4px">
      <h1 class="h1 dsp" style="font-size:26px">Apple tegenover Spotify</h1>
    </div>
    <div class="pad tnote" style="padding-bottom:6px">${FLAG} NL · Top 200 · makers met 2+ shows · vandaag</div>
    <div class="pad" style="padding-top:6px">${slope()}</div>
    <div class="pad tnote">Tik op een maker om zijn lijn te volgen.</div>
    <div class="pad" style="padding-top:10px">
      <div class="linkcard">
        ${logo(SEL, 44)}
        <div class="meta">
          <div class="t1">${SEL.name}</div>
          <div class="t2">Apple #4 · 9 shows</div>
          <div class="t2">Spotify #7 · 3 shows</div>
        </div>
        <span class="chev">${ICON.chevR()}</span>
      </div>
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 4. Makers volgen, in de Bibliotheek ---- */
screens.Volgen = { title: '4 · Makers volgen', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search'])}
  <div class="body">
    <div class="pad" style="padding-bottom:6px"><h1 class="h1 dsp" style="font-size:26px">Bibliotheek</h1></div>
    ${tabs(['Gevolgd', 'Makers', 'Wachtrij', 'Bewaard'], 'Makers')}
    <div class="pad" style="padding-top:14px">
      <div class="panel">
        <div class="row" style="gap:9px;padding-bottom:11px">${ICON.bell('#F0895B', 19)}<span class="ph-t">Nieuw van je makers</span><span style="flex:1"></span><span class="ph-s">Sinds gisteren</span></div>
        <div class="prow"><div class="art a3" style="width:36px;height:36px;border-radius:9px;box-shadow:none"></div><div class="meta"><div class="t1">Kort Lontje</div><div class="t2">Nieuwe podcast · Dagblad Noord</div></div><span class="pill new">NIEUW</span></div>
        <div class="prow"><div class="art a8" style="width:36px;height:36px;border-radius:9px;box-shadow:none"></div><div class="meta"><div class="t1">Doorzagen</div><div class="t2">Nieuwe podcast · Kelderwerk</div></div><span class="pill new">NIEUW</span></div>
      </div>
    </div>
    ${label('Je makers · 3')}
    <div class="pad list">
      ${[[M.kade, '2 shows met een nieuwe aflevering'], [M.noord, '1 show met een nieuwe aflevering'], [M.oost, 'Niets nieuws sinds gisteren']].map(([m, t]) => `
      <div class="lrow">${logo(m, 48)}<div class="meta"><div class="t1">${m.name}</div><div class="t2">${t}</div></div><span class="chev">${ICON.chevR()}</span></div>`).join('')}
    </div>
    ${label('Van podcasts die je volgt')}
    <div class="pad list">
      <div class="lrow">${logo(M.hemel, 48)}<div class="meta"><div class="t1">Studio Hemel</div><div class="t2">Je volgt 2 van hun 14 podcasts</div></div><div class="btn btn-o" style="padding:0 16px">Volg</div></div>
    </div>
  </div>
  ${chrome('library')}
</div>` };

/* ---- 5. Tips per medium ---- */
const tipRows = [
  ['Halve Zolen', 'a9', 'Twee broers bellen elke week een schoenmaker, en het wordt steeds beter', '27 sep'],
  ['Lange Adem', 'a5', 'De beste sportpodcast van dit najaar gaat over verliezen', '19 sep'],
  ['Ondergronds', 'a4', 'Deze true crime is rustig en precies, zonder sensatie', '12 sep']
];
screens.Tips = { title: '5 · Tips per medium', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar([])}
  <div class="body">
    <div class="pad" style="padding-bottom:12px"><h1 class="h1 dsp" style="font-size:26px">Tips van de media</h1></div>
    <div class="pad row" style="gap:8px;overflow:hidden">
      ${['Alle', 'Dagblad Noord', 'Weekblad Zuid', 'Radio 7'].map(t => `<div class="fchip ${t === 'Dagblad Noord' ? 'on' : ''}">${t}</div>`).join('')}
    </div>
    <div class="pad row" style="justify-content:space-between;padding-top:10px">
      <span class="tnote">23 tips sinds juni</span>
      <div class="schip">Nieuwste eerst ${ICON.chevS}</div>
    </div>
    <div class="pad">
      ${tipRows.map(t => `
      <div class="tip">
        <div class="art ${t[1]}" style="width:56px;height:56px;border-radius:12px"></div>
        <div class="ebody">
          <div class="src"><span class="omark" style="background:${M.noord.color}">DN</span>Dagblad Noord <span>· ${t[3]}</span></div>
          <div class="t1" style="margin-top:5px">${t[0]}</div>
          <div class="hl">${t[2]}</div>
          <div class="rd">Lees het artikel ${ICON.chevR(13)}</div>
        </div>
        <div class="pbtn">${ICON.play}</div>
      </div>`).join('')}
    </div>
    <div class="pad tnote" style="padding-top:6px">Eigen podcasts van Dagblad Noord tellen niet als tip.</div>
    <div class="pad" style="padding-top:4px">
      <div class="linkcard">
        ${logo(M.noord, 44)}
        <div class="meta"><div class="t1">Maakt ook podcasts</div><div class="t2">11 podcasts van Dagblad Noord</div></div>
        <span class="chev">${ICON.chevR()}</span>
      </div>
    </div>
  </div>
  ${chrome('discover')}
</div>` };

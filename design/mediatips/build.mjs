// Mockups voor "Mediatips": podcastsuggesties van kranten en omroepen, per land.
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
const FONTS = `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&amp;family=Instrument+Sans:wght@400;500;600;700&amp;display=swap">`;

const ICON = {
  search: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20.5 20.5L16.6 16.6"></path></svg>`,
  bell: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6"></path><path d="M13.7 20a2 2 0 01-3.4 0"></path></svg>`,
  back: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"></path></svg>`,
  more: `<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5.5" r="1.8"></circle><circle cx="12" cy="12" r="1.8"></circle><circle cx="12" cy="18.5" r="1.8"></circle></svg>`,
  chevS: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"></path></svg>`,
  chevR: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#B3A695" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"></path></svg>`,
  pause: `<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="5" width="3.6" height="14" rx="1.2"></rect><rect x="13.4" y="5" width="3.6" height="14" rx="1.2"></rect></svg>`,
  fwd: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><path d="M5 12h14"></path><path d="M14 7l5 5-5 5"></path></svg>`,
  navCharts: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 20v-7"></path><path d="M12 20V4"></path><path d="M19 20v-11"></path></svg>`,
  navDisc: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"></circle><path d="M15 9l-2 4.2L9 15l2-4.2z"></path></svg>`,
  navLib: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5v14"></path><path d="M9.5 5v14"></path><path d="M14.5 6.2l5 12.8"></path></svg>`,
  quote: (c = '#C4542B', s = 16) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="${c}"><path d="M6 6h5v6a4 4 0 01-4 4H6v-2h1a2 2 0 002-2v-1H6z"></path><path d="M13 6h5v6a4 4 0 01-4 4h-1v-2h1a2 2 0 002-2v-1h-3z"></path></svg>`,
  news: (c = 'currentColor', s = 20) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="${c}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h13v14H6a2 2 0 01-2-2z"></path><path d="M17 8h3v9a2 2 0 01-2 2"></path><path d="M7 9h6"></path><path d="M7 13h7"></path></svg>`,
  play: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.2v13.6L19 12z"></path></svg>`,
  plus: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14"></path><path d="M5 12h14"></path></svg>`,
  clock: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"></circle><path d="M12 7.5V12l3 2"></path></svg>`
};
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;

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

/* Fictieve podcasts (zoals de rest van de mockup); de media zijn echt, de tips zijn voorbeeldtekst. */
const tips = [
  { art: 'a7', title: 'Koud Spoor', pub: 'Podium Audio', outlet: 'de Volkskrant', mark: 'VK', color: '#1A1A18',
    quote: 'Een true-crimeserie die zich niet laat opjagen: vijf delen, elk een stap dichter bij een dorp dat liever zwijgt.', date: '9 sep' },
  { art: 'a2', title: 'Nachtdienst', pub: 'VRIJDAG Media', outlet: 'NRC', mark: 'NRC', color: '#B5482A',
    quote: 'Wie ’s nachts werkt hoort hier eindelijk zichzelf terug. Vier sterren.', date: '8 sep' },
  { art: 'a5', title: 'Lange Adem', pub: 'Studio Hemel', outlet: 'Trouw', mark: 'T', color: '#14504E',
    quote: 'Rustige gesprekken over volhouden, zonder ooit een zelfhulpboek te worden.', date: '6 sep' },
  { art: 'a10', title: 'Kade 12', pub: 'Kade Media', outlet: 'de Volkskrant', mark: 'VK', color: '#1A1A18',
    quote: 'De havenpodcast waar je niet om vroeg en niet meer zonder kunt.', date: '5 sep' },
  { art: 'a6', title: 'Het Vijfde Kwartier', pub: 'Sportcast NL', outlet: 'VPRO Gids', mark: 'VPRO', color: '#2B4C7E',
    quote: 'Sport als excuus voor een gesprek over alles wat ernaast gebeurt.', date: '3 sep' },
  { art: 'a9', title: 'Vandaag in Zeven', pub: 'Dagblad Noord', outlet: 'NRC', mark: 'NRC', color: '#B5482A',
    quote: 'Zeven minuten die je ochtend niet duurder maken dan nodig.', date: '1 sep' }
];
const outlets = [['Alle', true], ['de Volkskrant'], ['NRC'], ['Trouw'], ['VPRO Gids'], ['Parool']];

const outletMark = (t, size = 22) => `<span class="omark" style="background:${t.color};width:${size}px;height:${size}px;font-size:${Math.round(size * 0.36)}px">${t.mark}</span>`;

const CSS = `
.omark{display:inline-flex;align-items:center;justify-content:center;border-radius:6px;color:#FBF6EE;font-weight:800;letter-spacing:.02em;flex:none;font-family:'Bricolage Grotesque',sans-serif}
.tiprow{display:flex;gap:12px;padding:13px 0;border-bottom:1px solid var(--line2);align-items:flex-start}
.tiprow .eart{width:56px;height:56px;border-radius:12px;flex:none;box-shadow:0 1px 3px rgba(33,29,23,.14)}
.who{display:flex;align-items:center;gap:7px;font-size:11.5px;font-weight:700;color:var(--ink2);margin-bottom:5px}
.who .dt{color:var(--ink3);font-weight:500}
.q{font-size:13px;color:var(--ink2);line-height:1.4;margin-top:4px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.mini-chip{display:flex;align-items:center;gap:6px;height:36px;padding:0 10px;border-radius:10px;background:var(--surf);border:1px solid var(--line);font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap;flex:none}
.mini-chip.on{background:var(--ink);border-color:var(--ink);color:#FBF6EE}
.tcard{width:236px;flex:none;padding:13px 14px 14px;border-radius:16px;background:var(--surf);border:1px solid var(--line2);display:flex;flex-direction:column;gap:9px}
.tcard .top{display:flex;gap:11px;align-items:center}
.tcard .art{width:48px;height:48px;border-radius:11px}
.hscroll{display:flex;gap:12px;overflow:hidden}
.search{display:flex;align-items:center;gap:10px;height:50px;padding:0 15px;border-radius:14px;background:var(--surf);border:1px solid var(--line);color:var(--ink3);font-size:14.5px}
.strip{display:flex;align-items:center;gap:10px;padding:12px 12px 12px 14px;border-radius:15px;background:var(--ink);color:#F6EFE5}
.strip .pos{display:flex;align-items:baseline;gap:3px}
.strip .n{font-size:17px;font-weight:800;letter-spacing:-.03em;font-variant-numeric:tabular-nums}
.strip .k{font-size:10.5px;font-weight:600;color:#B3A695}
.vline{width:1px;height:26px;background:rgba(246,239,229,.18)}
.tipbox{border-radius:15px;background:var(--priC);padding:12px 13px;display:flex;flex-direction:column;gap:9px}
.tipbox .h{display:flex;align-items:center;gap:8px;font-size:12.5px;font-weight:700;color:var(--priOn)}
.tipbox .line{display:flex;gap:9px;align-items:flex-start;font-size:12.5px;color:var(--priOn);line-height:1.4}
.hero{display:flex;gap:14px;align-items:flex-end}
.hero .art{width:112px;height:112px;border-radius:18px;box-shadow:0 6px 18px rgba(33,29,23,.2)}
.stat{display:flex;align-items:center;gap:6px;font-size:12px;color:var(--ink2);font-weight:500}
`;

const screens = {};

/* ---- 1. Ontdek: sectie "Tips van de media" ---- */
screens.Main = { title: 'Ontdek — sectie Mediatips', css: CSS, body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar">
    <div class="mark">Woolacast</div>
    <div class="ibtn">${ICON.bell}</div>
  </div>
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:12px"><h1 class="h1 dsp">Ontdek</h1></div>
    <div class="pad" style="padding-bottom:16px"><div class="search">${ICON.search}Zoek podcasts en afleveringen</div></div>

    <div class="pad sect">
      <div class="h2 dsp row" style="gap:8px">${ICON.news('#C4542B', 20)}Tips van de media</div>
      <div style="font-size:12.5px;font-weight:600;color:var(--pri)">Alle 14</div>
    </div>
    <div class="pad row" style="gap:8px;padding-bottom:12px;overflow:hidden">
      ${outlets.slice(0, 4).map(([n, on]) => `<div class="mini-chip ${on ? 'on' : ''}">${n}</div>`).join('')}
    </div>
    <div class="pad hscroll" style="padding-bottom:20px">
      ${tips.slice(0, 3).map(t => `
      <div class="tcard">
        <div class="top">
          <div class="art ${t.art}"></div>
          <div class="meta">
            <div class="t1">${t.title}</div>
            <div class="t2">${t.pub}</div>
          </div>
        </div>
        <div class="q" style="margin-top:0">${ICON.quote('#C4542B', 13)} ${t.quote}</div>
        <div class="who" style="margin:0">${outletMark(t, 18)}${t.outlet}<span class="dt">· ${t.date}</span></div>
      </div>`).join('')}
    </div>

    <div class="pad sect">
      <div class="h2 dsp">Grootste stijgers</div>
      <div style="font-size:12.5px;font-weight:600;color:var(--pri)">Sinds gisteren</div>
    </div>
    <div class="pad hscroll">
      ${['a6', 'a4', 'a10'].map(a => `<div style="width:124px;flex:none"><div class="art ${a}" style="width:124px;height:124px;border-radius:14px"></div></div>`).join('')}
    </div>
  </div>
  ${chrome('discover')}
</div>` };

/* ---- 2. Het scherm Mediatips: per land, per medium, chronologisch ---- */
screens.Tips = { title: 'Mediatips — het scherm', css: CSS + `
.day{font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;color:var(--ink3);padding:14px 0 2px}
`, body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar" style="padding-left:8px">
    <div class="row" style="gap:4px">
      <div class="ibtn">${ICON.back}</div>
      <div style="font-size:16.5px;font-weight:700;letter-spacing:-.012em">Tips van de media</div>
    </div>
    <div class="ibtn">${ICON.more}</div>
  </div>
  <div class="body">
    <div class="pad row" style="gap:8px;padding-bottom:8px;overflow:hidden">
      <div class="mini-chip on">${FLAG}<span style="color:#FBF6EE">NL</span> ${ICON.chevS}</div>
      ${outlets.map(([n, on]) => `<div class="mini-chip ${on ? '' : ''}" style="${on ? 'background:var(--priC);border-color:#EFC7AF;color:var(--priOn)' : ''}">${n}</div>`).join('')}
    </div>
    <div class="pad row" style="gap:6px;height:30px"><span class="note">14 tips deze maand · 5 media · uit hun podcastrubrieken</span></div>

    <div class="pad">
      <div class="day">Deze week</div>
      ${tips.slice(0, 3).map(t => `
      <div class="tiprow">
        <div class="eart ${t.art}"></div>
        <div class="ebody">
          <div class="who">${outletMark(t, 20)}${t.outlet}<span class="dt">· ${t.date}</span></div>
          <div class="etitle" style="-webkit-line-clamp:1">${t.title}</div>
          <div class="q">${t.quote}</div>
        </div>
        <div class="pbtn">${ICON.play}</div>
      </div>`).join('')}
      <div class="day">Vorige week</div>
      ${tips.slice(3, 5).map(t => `
      <div class="tiprow">
        <div class="eart ${t.art}"></div>
        <div class="ebody">
          <div class="who">${outletMark(t, 20)}${t.outlet}<span class="dt">· ${t.date}</span></div>
          <div class="etitle" style="-webkit-line-clamp:1">${t.title}</div>
          <div class="q">${t.quote}</div>
        </div>
        <div class="pbtn">${ICON.play}</div>
      </div>`).join('')}
    </div>
  </div>
  ${chrome('discover')}
</div>` };

/* ---- 3. Op de podcastpagina: "Getipt door" naast de noteringen ---- */
screens.Podcast = { title: 'Podcastpagina — "Getipt door"', css: CSS, body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar" style="padding-left:8px">
    <div class="ibtn">${ICON.back}</div>
    <div class="ibtn">${ICON.more}</div>
  </div>
  <div class="body">
    <div class="pad hero" style="padding-bottom:14px">
      <div class="art a7"></div>
      <div class="ebody" style="padding-bottom:2px">
        <div class="dsp" style="font-size:23px;font-weight:700;letter-spacing:-.022em;line-height:1.1">Koud Spoor</div>
        <div class="t2" style="margin-top:5px">Podium Audio</div>
        <div class="stat" style="margin-top:9px">${ICON.clock}True crime · wekelijks · 5 afl.</div>
      </div>
    </div>
    <div class="pad row" style="gap:9px;padding-bottom:16px">
      <div class="btn btn-p" style="flex:1 1 auto">${ICON.plus}Volgen</div>
      <div class="btn btn-o" style="padding:0 16px">${ICON.play}Nieuwste afl.</div>
    </div>
    <div class="pad" style="padding-bottom:12px">
      <div class="strip">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round"><path d="M5 20v-6"></path><path d="M12 20V5"></path><path d="M19 20v-9"></path></svg>
        <div class="pos"><span class="n">2</span><span class="k">Apple</span></div>
        <span class="vline"></span>
        <div class="pos"><span class="n">5</span><span class="k">Spotify</span></div>
        <span style="flex:1 1 auto"></span>
        <span style="font-size:11.5px;font-weight:600;color:#B3A695">3 landen</span>
        ${ICON.chevR}
      </div>
    </div>
    <div class="pad" style="padding-bottom:16px">
      <div class="tipbox">
        <div class="h">${ICON.news('#5A2110', 17)}Getipt door 2 media</div>
        <div class="line">${outletMark(tips[0], 20)}<span><strong>de Volkskrant</strong> · 9 sep — “${tips[0].quote}”</span></div>
        <div class="line">${outletMark(tips[1], 20)}<span><strong>NRC</strong> · 2 sep — “Vijf delen, geen minuut te veel.”</span></div>
      </div>
    </div>
    <div class="pad tabs">
      <div class="tab on">Afleveringen</div>
      <div class="tab">Noteringen</div>
      <div class="tab">Over</div>
    </div>
    <div class="hr"></div>
    <div class="pad">
      <div class="erow">
        <div class="eart a7"></div>
        <div class="ebody">
          <div class="etitle">De laatste getuige (deel 3)</div>
          <div class="emeta" style="margin-top:6px">9 sep<span>·</span>54 min</div>
        </div>
        <div class="pbtn">${ICON.play}</div>
      </div>
    </div>
  </div>
  ${chrome('')}
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

const order = ['Main', 'Tips', 'Podcast'];
fs.writeFileSync(path.join(dir, 'canvas.json'), JSON.stringify({
  artboards: order.map((n, i) => ({ file: n + '.dc.html', x: i * 470, y: 0, w: 390, h: 844, title: screens[n].title })),
  annotations: [
    { id: 'n-ontdek', x: 0, y: -170, w: 390, text: 'Ingang op Ontdek: een sectie "Tips van de media" met chips per medium en kaarten met de tip als citaat. "Alle 14" opent het volledige scherm. De media wisselen met het land: NL toont Volkskrant, NRC, Trouw, VPRO Gids; BE De Standaard en De Morgen; DE Zeit en SZ; VK The Guardian; VS NYT en Vulture.' },
    { id: 'n-tips', x: 470, y: -170, w: 390, text: 'Het scherm zelf: land vooraan, dan een chip per medium, en de tips chronologisch per week. Elke tip is één regel wie en wanneer, de podcast, en het citaat. Afspelen start de nieuwste aflevering; tikken opent de podcastpagina.' },
    { id: 'n-detail', x: 940, y: -170, w: 390, text: 'De tip volgt de podcast: op de podcastpagina staat onder de noteringen een blok "Getipt door" met citaat en datum, zodat een tip ook opduikt als je de show via een hitlijst vindt.' },
    { id: 'n-data', x: 0, y: 900, w: 860, text: 'Hoe het gevoed wordt: het verzamelklusje leest per medium zijn podcastrubriek (RSS waar die er is, anders de rubriekpagina) en koppelt genoemde titels aan de Apple-catalogus, net als bij Spotify. Per medium is een eigen adapter nodig, en niet elke rubriek is bereikbaar — de Volkskrant-tagpagina weert bijvoorbeeld automatische lezers, hun site-RSS niet. Alle tips en podcasts hier zijn voorbeeldtekst.' }
  ],
  launch: { view: 'canvas' }
}, null, 2));
console.log('canvas.json ok');

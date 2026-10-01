// Telefoonschermen voor de tweede reeks nieuwe functies. Bouwt op design/base.css en
// de bouwstenen van ../nieuwe-functies/screens.mjs (die weer leent van ../kanalen).
// Alle podcasts, afleveringen, plekken, media en cijfers hieronder zijn verzonnen.
import {
  ICON as I0, CSS as CSS0, chrome, appbar, titlebar, tabs, label, toggle, mv, playerHead, sheet,
  UP, FLAG, R15, F30, PREV, NEXT, PAUSE
} from '../nieuwe-functies/screens.mjs';
import { ICON as K } from '../kanalen/screens.mjs';

const svg = (d, s = 22, w = 1.9) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;
export const ICON = {
  ...I0,
  mark: (s = 21, fill = false) => svg(`<path d="M7 3.8h10a1 1 0 011 1V20l-6-4-6 4V4.8a1 1 0 011-1z"${fill ? ' fill="currentColor"' : ''}></path>`, s, 1.8),
  cast: (s = 22) => svg('<path d="M3 17.5a3.5 3.5 0 013.5 3.5"></path><path d="M3 13.5A7.5 7.5 0 0110.5 21"></path><path d="M3 9.6V6a2 2 0 012-2h14a2 2 0 012 2v12a2 2 0 01-2 2h-5"></path><circle cx="3.6" cy="20.4" r=".5" fill="currentColor"></circle>', s, 1.8),
  speaker: (s = 22) => svg('<rect x="6" y="3" width="12" height="18" rx="3"></rect><circle cx="12" cy="14" r="3.2"></circle><circle cx="12" cy="7.2" r=".6" fill="currentColor"></circle>', s, 1.8),
  tv: (s = 22) => svg('<rect x="3" y="5" width="18" height="12" rx="2"></rect><path d="M8 21h8"></path>', s, 1.8),
  phone: (s = 22) => svg('<rect x="7" y="2.8" width="10" height="18.4" rx="2.4"></rect><path d="M11 17.6h2"></path>', s, 1.8),
  clock: (s = 22) => svg('<circle cx="12" cy="12" r="8.5"></circle><path d="M12 7.5V12l3 2"></path>', s, 1.9),
  hide: (s = 20) => svg('<path d="M4 4l16 16"></path><path d="M9.9 5.2A9.6 9.6 0 0112 5c5 0 8.5 4.6 9 7-.2 1-1 2.4-2.3 3.8"></path><path d="M6.4 6.6C4.6 7.9 3.4 9.7 3 12c.5 2.4 4 7 9 7a9.3 9.3 0 004.1-.9"></path>', s, 1.8),
  shake: (s = 20) => svg('<rect x="8" y="4" width="8" height="16" rx="2"></rect><path d="M4.5 8.5v7"></path><path d="M19.5 8.5v7"></path><path d="M2 10.5v3"></path><path d="M22 10.5v3"></path>', s, 1.8),
  fade: (s = 20) => svg('<path d="M3 6h3l6 0"></path><path d="M12 6c3 0 5 6 9 12"></path><path d="M3 18h18" opacity=".35"></path>', s, 1.8),
  rewind: (s = 20) => svg('<path d="M4 12a8 8 0 108-8"></path><path d="M4 4v4h4"></path>', s, 1.8),
  window: (s = 20) => svg('<path d="M19.5 14.5A8 8 0 019.5 4.5a8 8 0 1010 10z"></path><path d="M16 3v4M14 5h4"></path>', s, 1.8),
  upload: (s = 20) => svg('<path d="M12 15V4"></path><path d="M8 8l4-4 4 4"></path><path d="M5 19h14"></path>', s, 1.9),
  cloud: (s = 20) => svg('<path d="M7 18a4.5 4.5 0 01-.6-9A6 6 0 0118 9.5a4.2 4.2 0 01-.5 8.5z"></path>', s, 1.8),
  layers: (s = 20) => svg('<path d="M12 3l9 5-9 5-9-5z"></path><path d="M3 13l9 5 9-5"></path>', s, 1.8),
  note: (s = 18) => svg('<path d="M4 20l4-1 10.5-10.5a2.1 2.1 0 00-3-3L5 16z"></path>', s, 1.8)
};

export const CSS = CSS0 + `
.nhead{display:flex;align-items:center;justify-content:space-between;padding-top:14px}
.nhead h2{font-size:18px;font-weight:700;letter-spacing:-.012em;margin:0;display:flex;align-items:center;gap:8px}
.ncount{display:inline-flex;align-items:center;justify-content:center;min-width:24px;height:22px;padding:0 7px;border-radius:11px;background:var(--pri);color:var(--priInk);font-size:12px;font-weight:800;font-variant-numeric:tabular-nums}
.tlink{font-size:13px;font-weight:700;color:var(--pri);display:flex;align-items:center;gap:5px;min-height:44px}
.day{font-size:11px;font-weight:700;letter-spacing:.09em;text-transform:uppercase;color:var(--ink2);padding:12px 0 2px}
.nrow{display:flex;gap:12px;align-items:center;padding:10px 0;border-bottom:1px solid var(--line2);background:var(--bg);position:relative}
.nrow .art{width:48px;height:48px}
.nrow .ebody{flex:1 1 auto;min-width:0}
.nrow .dot{width:8px;height:8px;border-radius:4px;background:var(--pri);flex:none}
.swipe{position:relative;margin:0 -20px;overflow:hidden}
.swipe .under{position:absolute;inset:0;background:var(--surf3);display:flex;align-items:center;justify-content:flex-end;gap:8px;padding-right:22px;font-size:13px;font-weight:700;color:var(--ink)}
.swipe .nrow{transform:translateX(-112px);padding-left:20px;padding-right:20px}
.frow{display:flex;align-items:center;gap:12px;height:60px;border-bottom:1px solid var(--line2)}
.frow .art{width:44px;height:44px}
.badge{font-size:11.5px;font-weight:700;color:var(--pri)}
.lock{background:linear-gradient(160deg,#332F63,#7E3149)}
.nbtns{display:flex;gap:18px;margin-top:10px;font-size:13px;font-weight:700;color:var(--pri)}
.nstack{display:flex;flex-direction:column;gap:6px;margin-top:8px}
.nstack div{display:flex;align-items:center;gap:9px;font-size:13px}
.nstack .art{width:28px;height:28px;border-radius:7px;box-shadow:none}
.snack{position:absolute;left:12px;right:12px;bottom:28px;border-radius:14px;background:var(--panel);color:var(--onPanel);display:flex;align-items:center;gap:12px;padding:0 6px 0 16px;height:56px;font-size:13.5px;font-weight:600;box-shadow:0 10px 26px rgba(33,29,23,.3)}
.snack .act{margin-left:auto;height:44px;padding:0 12px;display:flex;align-items:center;color:#F0A87F;font-weight:700}
.bm{display:flex;gap:12px;align-items:flex-start;padding:11px 0;border-bottom:1px solid var(--line2)}
.bm .ts{width:50px;flex:none;font-size:12.5px;font-weight:700;color:var(--pri);font-variant-numeric:tabular-nums;padding-top:1px}
.bm .bt{flex:1 1 auto;min-width:0;font-size:14px;font-weight:500;line-height:1.35}
.bm .bt span{display:block;font-size:12px;color:var(--ink2);margin-top:2px;font-weight:500}
.bm .bt.empty{color:var(--ink2)}
.bar .bmk{position:absolute;top:-9px;width:10px;height:12px;margin-left:-5px;color:var(--pri);display:block}
.dev{display:flex;align-items:center;gap:14px;min-height:62px;border-bottom:1px solid var(--line2)}
.dev .ic{width:40px;height:40px;border-radius:12px;background:var(--surf2);display:flex;align-items:center;justify-content:center;flex:none}
.dev.on .ic{background:var(--priC);color:var(--priOn)}
.dev .tx{flex:1 1 auto}.dev .tx b{display:block;font-size:14.5px;font-weight:600}.dev .tx span{display:block;font-size:12.5px;color:var(--ink2);margin-top:1px}
.dev.on .tx b{color:var(--priOn)}
.castbar{display:flex;align-items:center;gap:8px;height:34px;padding:0 12px;border-radius:10px;background:var(--priC);color:var(--priOn);font-size:12.5px;font-weight:700;width:max-content;margin:0 auto}
.warnbox{border-radius:14px;background:var(--surf2);padding:12px 14px;font-size:12.5px;line-height:1.45;color:var(--ink2)}
.warnbox b{color:var(--ink);font-weight:600}
.hrow{display:flex;gap:12px;align-items:center;padding:10px 0;border-bottom:1px solid var(--line2)}
.hrow .art{width:48px;height:48px}
.hrow .ebody{flex:1 1 auto;min-width:0}
.hrow .pg{height:4px;border-radius:2px;background:var(--surf3);margin-top:7px;position:relative;overflow:hidden}
.hrow .pg i{position:absolute;left:0;top:0;bottom:0;background:var(--ink2);display:block;border-radius:2px}
.hrow .pg i.done{background:var(--up)}
.hrow .when{font-size:11.5px;color:var(--ink2);font-variant-numeric:tabular-nums;text-align:right;flex:none;width:52px}
.ptrack{border-radius:16px;background:var(--surf);border:1px solid var(--line2);padding:14px 12px 8px}
.ptrack text{font-family:'Instrument Sans',sans-serif;font-size:10px;fill:var(--ink2)}
.cause{display:flex;gap:12px;align-items:flex-start;padding:12px 0;border-bottom:1px solid var(--line2)}
.cause .k{width:28px;height:28px;border-radius:14px;background:var(--panel);color:var(--onPanel);display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:800;flex:none}
.cause .ct{flex:1 1 auto;font-size:14px;line-height:1.35}
.cause .ct span{display:block;font-size:12px;color:var(--ink2);margin-top:2px}
.near{display:flex;align-items:center;gap:6px;font-size:12px;color:var(--ink2);margin-top:3px}
.near b{color:var(--ink);font-weight:600}
.near .omark{width:16px;height:16px;font-size:7.5px;border-radius:4px}
.serial{display:inline-flex;align-items:center;gap:6px;height:26px;padding:0 9px;border-radius:8px;background:var(--surf2);font-size:12px;font-weight:700}
.bigplay{display:flex;align-items:center;gap:12px;padding:14px 16px;border-radius:16px;background:var(--pri);color:var(--priInk)}
.bigplay .pc{width:40px;height:40px;border-radius:20px;background:rgba(255,253,250,.22);display:flex;align-items:center;justify-content:center;flex:none}
.bigplay b{display:block;font-size:15.5px;font-weight:700}.bigplay span{display:block;font-size:12px;opacity:.88;margin-top:1px}
.epn{width:30px;font-family:'Bricolage Grotesque','Instrument Sans',sans-serif;font-size:18px;font-weight:800;color:var(--ink2);text-align:center;flex:none;font-variant-numeric:tabular-nums}
.epn.on{color:var(--pri)}
.srow.tall{align-items:flex-start;padding:12px 0}.srow.tall .sw,.srow.tall .valchip{margin-top:6px}
mark.hit{background:#F6D9A8;color:var(--ink);border-radius:3px;padding:0 1px}
.setgrp{font-size:11px;font-weight:700;letter-spacing:.09em;text-transform:uppercase;color:var(--ink2);padding:16px 0 4px}
.sumgrid{display:grid;grid-template-columns:1fr auto;gap:0 12px;font-size:13.5px}
.sumgrid span{padding:9px 0;border-bottom:1px solid var(--line2)}
.sumgrid span:nth-child(2n){text-align:right;font-weight:700;font-variant-numeric:tabular-nums}
`;

export const screens = {};

/* ---- 1. Nieuw bovenaan Gevolgd ---- */
screens.Inbox = { title: '1 · Bibliotheek → Gevolgd, met Nieuw bovenaan', body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar"><div class="mark">Toadcast</div><div class="row" style="gap:0"><div class="ibtn" aria-label="Geschiedenis">${ICON.clock()}</div><div class="ibtn">${K.search}</div><div class="ibtn">${ICON.more}</div></div></div>
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Bibliotheek</h1></div>
    ${tabs(['Gevolgd', 'Makers', 'Wachtrij', 'Bewaard', 'Gedownload'], 'Gevolgd')}
    <div class="pad">
      <div class="nhead"><h2>Nieuw <span class="ncount">7</span></h2><span class="tlink">${ICON.queue(18)}Alles in de wachtrij</span></div>
      <div class="row" style="gap:8px;padding:2px 0 4px"><div class="fchip on">Onbeluisterd</div><div class="fchip">Kort &lt; 30 min</div><div class="fchip">Deze week</div></div>
      <div class="day">Vandaag</div>
      <div class="nrow"><span class="dot" aria-label="nieuw"></span><div class="art a1"></div><div class="ebody"><div class="t1">Wie betaalt de dijk?</div><div class="t2">De Deadline · 38 min · 06:00</div></div><div class="pbtn" style="margin:0">${K.play}</div></div>
      <div class="swipe">
        <div class="under">${ICON.hide(18)}Verbergen</div>
        <div class="nrow"><span class="dot"></span><div class="art a7"></div><div class="ebody"><div class="t1">Deel 5: het tweede alibi</div><div class="t2">Koud Spoor · 52 min</div></div><div class="pbtn" style="margin:0">${K.play}</div></div>
      </div>
      <div class="day">Gisteren</div>
      <div class="nrow"><span class="dot"></span><div class="art a5"></div><div class="ebody"><div class="t1">Rondje Amstel in 2:59</div><div class="t2">Lange Adem · 24 min</div></div><div class="pbtn" style="margin:0">${K.play}</div></div>
      <div class="tlink" style="justify-content:center;color:var(--ink2)">Nog 4 nieuw ${K.chevS}</div>
      <div class="nhead" style="padding-top:4px"><h2>Je shows</h2><span class="note" style="color:var(--ink2);font-size:12.5px">11</span></div>
      <div class="frow"><div class="art a2"></div><div class="meta"><div class="t1">Nachtdienst</div><div class="t2">bijgewerkt di</div></div><span class="badge">1 nieuw</span></div>
    </div>
  </div>
  ${chrome('library')}
</div>` };

screens.InboxNotif = { title: '1 · Eén gebundelde melding per ronde', body: `
<div class="ph col lock">
  <div style="height:120px"></div>
  <div style="text-align:center;color:#FFFDFA"><div class="dsp" style="font-size:72px;font-weight:600;letter-spacing:-.03em;line-height:1">06:12</div><div style="font-size:15px;margin-top:6px;opacity:.9">woensdag 1 oktober</div></div>
  <div style="height:40px"></div>
  <div class="notif">
    <div class="app"><i></i>Toadcast · Nieuwe afleveringen · nu</div>
    <div style="font-size:14.5px;font-weight:700;margin-top:6px">3 nieuwe afleveringen</div>
    <div class="nstack">
      <div><span class="art a1"></span>Wie betaalt de dijk? <span style="color:var(--ink2)">· De Deadline</span></div>
      <div><span class="art a7"></span>Deel 5: het tweede alibi <span style="color:var(--ink2)">· Koud Spoor</span></div>
      <div><span class="art a5"></span>Rondje Amstel in 2:59 <span style="color:var(--ink2)">· Lange Adem</span></div>
    </div>
    <div class="nbtns"><span>Afspelen</span><span>In de wachtrij</span></div>
  </div>
  <div style="padding:14px 22px;color:#FFFDFA;font-size:12.5px;opacity:.88;line-height:1.45">Alleen voor shows waarbij je de melding aanzette. Eigen kanaal "Nieuwe afleveringen". Android vraagt pas toestemming bij je eerste schakelaar.</div>
</div>` };

/* ---- 2. Bladwijzers ---- */
screens.BookmarkSet = { title: '2 · Eén tik zet een bladwijzer', body: `
<div class="ph col">
  <div class="sa"></div>
  ${playerHead()}
  <div class="body pad" style="padding-top:6px">
    <div class="npart a1" style="width:236px;height:236px"></div>
    <div style="padding-top:20px">
      <div class="dsp" style="font-size:22px;font-weight:700;letter-spacing:-.024em;line-height:1.15">Live vanuit Paradiso</div>
      <div class="t2" style="margin-top:5px;font-size:13.5px">De Deadline · 8 sep · 71 min</div>
    </div>
    <div style="padding-top:22px">
      <div class="bar" role="img" aria-label="Voortgang 23 van 71 minuten, bladwijzers op 12 en 23 minuten"><b style="width:32.7%"></b><span class="bmk" style="left:17%">${ICON.mark(10, true)}</span><span class="bmk" style="left:32.7%">${ICON.mark(10, true)}</span><u style="left:calc(32.7% - 7px)"></u></div>
      <div class="times"><span>23:14</span><span>-47:46</span></div>
    </div>
    <div class="row" style="justify-content:space-between;padding-top:10px">
      <div class="tbtn">${R15}</div><div class="tbtn">${PREV}</div><div class="pbig">${PAUSE()}</div><div class="tbtn">${NEXT}</div><div class="tbtn">${F30}</div>
    </div>
    <div class="tools" style="padding-top:12px">
      <div class="tool">${ICON.speed()}1,2×</div><div class="tool">${ICON.moon()}Slaap</div><div class="tool on">${ICON.mark(21, true)}Bladwijzer</div><div class="tool">${ICON.list()}Hoofdst.</div><div class="tool">${ICON.queue()}Wachtrij</div>
    </div>
  </div>
  <div class="snack">${ICON.mark(18, true)}Bladwijzer op 23:14<span class="act">Notitie</span></div>
  <div class="gest"><i></i></div>
</div>` };

screens.BookmarkList = { title: '2 · Onder de hoofdstukken van de aflevering', body: `
<div class="ph col">
  <div class="sa"></div>
  ${playerHead()}
  <div class="body pad" style="padding-top:6px"><div class="npart a1" style="width:236px;height:236px"></div></div>
  ${sheet(`
    <div class="shead"><h2>Hoofdstukken</h2><span class="note" style="color:var(--ink2);font-size:12px;padding-right:8px">van de maker</span></div>
    <div class="pad">
      <div class="crow past"><span class="ts">0:00</span><span class="ct">Opening en nieuws van de week</span></div>
      <div class="crow past"><span class="ts">6:12</span><span class="ct">Gast: Sanne Kuipers over de nieuwe zaal</span></div>
      <div class="crow on"><span class="ts">24:40</span><span class="ct">Live: "Stadslicht" met band</span></div>
      <div class="shead" style="padding:16px 0 2px"><h2 style="font-size:16px;display:flex;align-items:center;gap:8px">${ICON.mark(18, true)}Jouw bladwijzers</h2><span class="note" style="color:var(--ink2);font-size:12px">2</span></div>
      <div class="bm"><span class="ts">12:05</span><div class="bt">"De zaal krijgt een tweede balkon"<span>Notitie · 8 sep</span></div><div class="ibtn" style="width:40px;height:40px">${K.share}</div></div>
      <div class="bm" style="border-bottom:0"><span class="ts">23:14</span><div class="bt empty">Geen notitie<span>Tik om een notitie te schrijven</span></div><div class="ibtn" style="width:40px;height:40px">${K.share}</div></div>
      <div class="note" style="color:var(--ink2);font-size:12px;padding:4px 0 20px">Delen stuurt de titel, de link van de maker en "vanaf 12:05". Alle bladwijzers staan ook bovenaan Bewaard.</div>
    </div>`, 300)}
</div>` };

/* ---- 3. Casten ---- */
screens.Cast = { title: '3 · Speler → casten naar een speaker', body: `
<div class="ph col">
  <div class="sa"></div>
  <div class="appbar">
    <div class="ibtn" style="margin-left:-12px">${ICON.chevD}</div>
    <div style="text-align:center"><div class="eyebrow" style="font-size:10px;color:var(--ink2)">Speelt nu uit</div><div style="font-size:12.5px;font-weight:700;margin-top:2px">De Deadline</div></div>
    <div class="row" style="gap:0"><div class="ibtn" style="color:var(--pri)">${ICON.cast()}</div><div class="ibtn" style="margin-right:-12px">${ICON.more}</div></div>
  </div>
  <div class="body pad" style="padding-top:6px">
    <div class="npart a1" style="width:200px;height:200px"></div>
    <div style="padding-top:14px"><div class="castbar">${ICON.speaker(16)}Speelt op Woonkamer</div></div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Afspelen op</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="dev"><div class="ic">${ICON.phone(20)}</div><div class="tx"><b>Deze telefoon</b><span>Galaxy S24</span></div></div>
      <div class="dev on"><div class="ic">${ICON.speaker(20)}</div><div class="tx"><b>Woonkamer</b><span>Nest Audio · speelt nu</span></div><span style="color:var(--pri)">${K.check}</span></div>
      <div class="dev"><div class="ic">${ICON.tv(20)}</div><div class="tx"><b>Tv boven</b><span>Chromecast</span></div></div>
      <div class="warnbox" style="margin-top:14px"><b>Op de speaker werken niet:</b> stiltes inkorten, stemversterking en intro overslaan. Snelheid, slaaptimer, hoofdstukken, wachtrij en voortgang wel. Stop je met casten, dan gaat de telefoon verder waar de speaker was.</div>
      <div style="height:20px"></div>
    </div>`, 352)}
</div>` };

/* ---- 4. Geschiedenis ---- */
const hist = [
  ['Vandaag', [['a1', 'Live vanuit Paradiso', 'De Deadline', '32 min geluisterd · tot 41:10', 58, false, '07:40']]],
  ['Gisteren', [
    ['a7', 'Deel 4: de buurvrouw', 'Koud Spoor', '49 min · uitgeluisterd', 100, true, '22:15'],
    ['a5', 'De marathon die niemand liep', 'Lange Adem', '18 min · tot 18:02', 42, false, '08:05']]],
  ['Maandag 29 september', [
    ['a2', 'De nachtbus naar huis', 'Nachtdienst', '44 min · uitgeluisterd', 100, true, '23:30'],
    ['a10', 'Aflevering 4: de getuige', 'Kade 12', '6 min · tot 6:20', 9, false, '17:12']]]
];
screens.History = { title: '4 · Bibliotheek → klokje → Geschiedenis', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Geschiedenis', ['search', 'more'])}
  <div class="body pad">
    <div class="tnote" style="margin-top:-4px">Deze week <b>4 u 31 min</b> geluisterd</div>
    ${hist.map(([d, rows]) => `<div class="day">${d}</div>${rows.map(r => `
      <div class="hrow"><div class="art ${r[0]}"></div><div class="ebody"><div class="t1">${r[1]}</div><div class="t2">${r[2]} · ${r[3]}</div><div class="pg"><i class="${r[5] ? 'done' : ''}" style="width:${r[4]}%"></i></div></div><div class="when">${r[6]}</div></div>`).join('')}`).join('')}
    <div class="srow" style="border-bottom:0;margin-top:6px"><div class="ic">${ICON.clock(20)}</div><div class="tx"><b>Bijhouden wat ik luister</b><span>Uitzetten of wissen geldt ook voor je Terugblik</span></div>${toggle(true)}</div>
  </div>
  ${chrome('library')}
</div>` };

/* ---- 5. Rond deze sprong ---- */
// Plek over 30 dagen (1 = boven). De sprong: van #64 naar #9 tussen dag 21 en 25.
const ranks = [71, 70, 74, 69, 72, 68, 66, 70, 67, 65, 69, 66, 64, 63, 66, 62, 65, 61, 63, 64, 62, 48, 31, 18, 9, 7, 6, 8, 7, 6];
const X0 = 30, X1 = 336, Y0 = 14, Y1 = 150, RMAX = 80;
const px = (i) => X0 + (X1 - X0) * i / (ranks.length - 1);
const py = (r) => Y0 + (Y1 - Y0) * (r - 1) / (RMAX - 1);
const line = ranks.map((r, i) => `${i ? 'L' : 'M'}${px(i).toFixed(1)} ${py(r).toFixed(1)}`).join(' ');
const events = [[20, 'A'], [21, 'B'], [23, 'C']];
const chart = `
<svg viewBox="0 0 346 176" width="100%" role="img" aria-label="Plek van Kade 12 over 30 dagen: lang rond plek 65, dan in vier dagen naar plek 9. Drie gebeurtenissen vlak voor en tijdens de sprong.">
  ${[1, 20, 40, 60, 80].map(r => `<line x1="${X0}" x2="${X1}" y1="${py(r)}" y2="${py(r)}" stroke="var(--line2)" stroke-width="1"></line><text x="${X0 - 6}" y="${py(r) + 3.5}" text-anchor="end">#${r}</text>`).join('')}
  <rect x="${px(20)}" y="${Y0}" width="${px(25) - px(20)}" height="${Y1 - Y0}" fill="var(--priC)" opacity=".7"></rect>
  <path d="${line}" fill="none" stroke="var(--pri)" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round"></path>
  <circle cx="${px(29)}" cy="${py(6)}" r="4" fill="var(--pri)"></circle>
  ${events.map(([i, k]) => `<g><line x1="${px(i)}" x2="${px(i)}" y1="${Y1}" y2="${py(ranks[i]) + 6}" stroke="var(--ink2)" stroke-width="1" stroke-dasharray="2 2"></line><circle cx="${px(i)}" cy="${Y1 + 12}" r="8" fill="var(--panel)"></circle><text x="${px(i)}" y="${Y1 + 15.5}" text-anchor="middle" style="fill:var(--onPanel);font-weight:800">${k}</text></g>`).join('')}
  <text x="${X0}" y="${Y1 + 15.5}">1 sep</text><text x="${X1}" y="${Y1 + 15.5}" text-anchor="end">30 sep</text>
</svg>`;
screens.JumpTracker = { title: '5 · Chart-tracker: rond deze sprong', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Kade 12', ['share'])}
  <div class="body pad">
    <div class="row" style="gap:8px;padding-bottom:12px"><div class="schip dark">Apple ${K.chevS}</div><div class="schip">${FLAG}NL ${K.chevS}</div><div class="schip">True crime ${K.chevS}</div></div>
    <div class="row" style="gap:10px;align-items:baseline"><div class="dsp" style="font-size:40px;font-weight:800;letter-spacing:-.03em">#6</div><div class="t2">was #64 op 21 sep</div>${mv('up', 58)}</div>
    <div class="ptrack" style="margin-top:10px">${chart}</div>
    <div class="shead" style="padding:16px 0 0"><h2 style="font-size:17px">Rond deze sprong</h2><span class="note" style="color:var(--ink2);font-size:12px">21–25 sep</span></div>
    <div class="cause"><span class="k">A</span><div class="ct">Op 20 sep getipt door Dagblad Noord<span>"Kade 12 is de spannendste reeks van het najaar" · tips van de media</span></div></div>
    <div class="cause"><span class="k">B</span><div class="ct">Op 21 sep in Apple's selectie Nieuwe programma's<span>Apple NL · redactie</span></div></div>
    <div class="cause" style="border-bottom:0"><span class="k">C</span><div class="ct">Op 23 sep de eerste aflevering van seizoen 2<span>uit de feed van de show</span></div></div>
    <div class="note" style="color:var(--ink2);font-size:11.5px;padding-top:4px">Wat in de 7 dagen vóór of tijdens de sprong gebeurde. Of het de oorzaak is, weet de app niet.</div>
  </div>
  <div class="gest"><i></i></div>
</div>` };

screens.JumpList = { title: '5 · In de lijst: één regel, de sterkste aanleiding', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px"><h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1><span class="note" style="color:var(--ink2)">Bijgewerkt 06:00</span></div>
    <div class="pad row" style="gap:8px;padding-bottom:8px"><div class="schip dark">Apple Podcasts ${K.chevS}</div><div class="schip">${FLAG}NL ${K.chevS}</div><div class="schip">True crime ${K.chevS}</div></div>
    ${tabs(['Podcasts', 'Afleveringen', 'Trending', 'Nieuw'], 'Podcasts')}
    <div class="pad list">
      <div class="lrow"><div class="rank top">5</div><div class="art a7"></div><div class="meta"><div class="t1">Koud Spoor</div><div class="t2">Kelderwerk</div></div>${mv('down', 1)}</div>
      <div class="lrow" style="height:auto;padding:10px 0"><div class="rank top">6</div><div class="art a10"></div><div class="meta"><div class="t1">Kade 12</div><div class="t2">Kade Media</div><div class="near"><span class="omark" style="background:#2B4C7E">DN</span><span>Rond de sprong: <b>getipt door Dagblad Noord</b></span></div></div>${mv('up', 58)}</div>
      <div class="lrow"><div class="rank top">7</div><div class="art a2"></div><div class="meta"><div class="t1">Nachtdienst</div><div class="t2">Kade Media</div></div>${mv('flat')}</div>
      <div class="lrow" style="height:auto;padding:10px 0"><div class="rank top">8</div><div class="art a9"></div><div class="meta"><div class="t1">Wie belde om drie uur?</div><div class="t2">Dagblad Noord Onderzoekt</div><div class="near">${ICON.star(14)}<span>Rond de sprong: <b>eerst hoog in ${`<span class="flag" style="display:inline-flex;vertical-align:-2px"><i style="background:#000"></i><i style="background:#DD0000"></i><i style="background:#FFCE00"></i></span>`} DE</b></span></div></div>${mv('up', 23)}</div>
      <div class="lrow"><div class="rank top">9</div><div class="art a8"></div><div class="meta"><div class="t1">Derby</div><div class="t2">Studio Hemel</div></div>${mv('down', 2)}</div>
    </div>
  </div>
  ${chrome('charts')}
</div>` };

/* ---- 7. Back-up ---- */
screens.Backup = { title: '7 · Back-up terugzetten: eerst de samenvatting', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Je shows en gegevens')}
  <div class="body pad">
    <div class="setgrp" style="padding-top:4px">Verhuizen</div>
    <div class="srow"><div class="ic">${ICON.down(20)}</div><div class="tx"><b>Importeer OPML</b><span>Alleen de shows, uit een andere app</span></div></div>
    <div class="srow"><div class="ic">${ICON.upload(20)}</div><div class="tx"><b>Exporteer een back-up</b><span>Alles van jou in één .toadcast-bestand</span></div></div>
    <div class="srow"><div class="ic">${ICON.rewind(20)}</div><div class="tx"><b>Back-up terugzetten</b><span>Vervangt wat er nu staat</span></div></div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Back-up terugzetten?</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="note" style="color:var(--ink2);font-size:12.5px;margin-top:-4px">toadcast-2026-09-14.toadcast · gemaakt op 14 sep op Pixel 7</div>
      <div class="sumgrid" style="margin-top:12px">
        <span>Gevolgde shows en makers</span><span>34 · 5</span>
        <span>Wachtrij en bewaard</span><span>8 · 21</span>
        <span>Voortgang en beluisterd</span><span>612</span>
        <span>Bladwijzers</span><span>17</span>
        <span>Geschiedenis</span><span>14 mnd</span>
        <span>Instellingen, ook per show</span><span>6 shows</span>
      </div>
      <div class="warnbox" style="margin-top:14px"><b>Dit vervangt alles wat er nu staat.</b> Er wordt niets samengevoegd. Hitlijsten, tips en downloads zitten niet in de back-up: die haalt de app opnieuw op.</div>
      <div class="row" style="gap:10px;margin:16px 0 20px"><div class="btn btn-o" style="flex:1">Annuleren</div><div class="btn btn-p" style="flex:1">Vervangen</div></div>
    </div>`, 300)}
</div>` };

/* ---- 8. Slaaptimer ---- */
screens.Sleep = { title: '8 · Instellingen → Slaaptimer', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Slaaptimer')}
  <div class="body pad">
    <div class="note" style="color:var(--ink2);font-size:12.5px;line-height:1.45;margin-top:-4px">De timer zelf zet je in de speler. Hier staat hoe hij zich gedraagt.</div>
    <div class="srow tall"><div class="ic">${ICON.fade()}</div><div class="tx"><b>Zacht uitfaden</b><span>De laatste 30 seconden steeds zachter. Daarna staat het volume weer op 100%.</span></div>${toggle(true)}</div>
    <div class="srow tall"><div class="ic">${ICON.shake()}</div><div class="tx"><b>Schudden voor meer tijd</b><span>In de laatste minuut of tijdens het uitfaden: de timer begint opnieuw. Een korte tril bevestigt het.</span></div>${toggle(true)}</div>
    <div class="srow tall"><div class="ic">${ICON.rewind()}</div><div class="tx"><b>Terug bij verder luisteren</b><span>Na de slaaptimer begint de speler 30 seconden eerder. Niet na "einde aflevering".</span></div><div class="valchip">30 s</div></div>
    <div class="srow tall" style="border-bottom:0"><div class="ic">${ICON.window()}</div><div class="tx"><b>Vanzelf aan 's avonds</b><span>Als je zelf op afspelen drukt tussen 22:00 en 06:00. Nooit in de auto.</span></div>${toggle(false)}</div>
    <div class="row" style="gap:8px;padding:2px 0 0 54px"><div class="valchip def">22:00</div><span style="color:var(--ink2)">tot</span><div class="valchip def">06:00</div><div class="valchip def">30 min</div></div>
  </div>
  <div class="gest"><i></i></div>
</div>` };

/* ---- 9. Serieel ---- */
const serialEps = [
  ['1', 'De vondst op de dijk', '41 min', 'done'],
  ['2', 'Wie had de sleutel?', '46 min', 'done'],
  ['3', 'Het tweede alibi', '52 min', 'on'],
  ['4', 'De buurvrouw spreekt', '49 min', ''],
  ['5', 'Terug naar de dijk', '55 min', '']
];
screens.Serial = { title: '9 · Podcastpagina van een verhaal in delen', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('', ['share', 'more'])}
  <div class="body pad">
    <div class="row" style="gap:14px"><div class="art a7" style="width:88px;height:88px;border-radius:16px"></div>
      <div><div class="eyebrow" style="color:var(--ink2)">Podcast</div><div class="dsp" style="font-size:24px;font-weight:700;letter-spacing:-.02em">Koud Spoor</div><div class="t2">Kelderwerk · #5 True crime</div></div></div>
    <div class="row" style="gap:8px;margin-top:12px"><div class="btn btn-o">${K.check}Volgt</div><span class="serial">${ICON.layers(15)}Verhaal in delen</span></div>
    <div class="bigplay" style="margin-top:14px"><div class="pc">${K.play}</div><div><b>Verder bij deel 3</b><span>Het tweede alibi · nog 31 min</span></div></div>
    <div class="tlink" style="color:var(--ink2);font-weight:600">Opnieuw beginnen bij deel 1</div>
    <div class="row" style="gap:8px;padding:2px 0 6px"><div class="fchip on">Seizoen 1</div><div class="fchip">Seizoen 2</div><div class="fchip">Trailers en bonus</div></div>
    ${serialEps.map(e => `
    <div class="erow2"><span class="epn ${e[3] === 'on' ? 'on' : ''}">${e[0]}</span><div class="ebody"><div class="t1">${e[1]}</div><div class="t2">${e[2]}${e[3] === 'done' ? ' · beluisterd' : e[3] === 'on' ? ' · nog 31 min' : ''}</div></div>${e[3] === 'done' ? `<span style="color:var(--up)">${ICON.done(20)}</span>` : `<div class="pbtn" style="margin:0">${K.play}</div>`}</div>`).join('')}
    <div class="note" style="color:var(--ink2);font-size:11.5px;padding-top:8px">Oudste eerst, omdat de maker de show als serie markeert. Om te zetten bij Instellingen voor deze show.</div>
  </div>
  ${chrome('library')}
</div>` };

/* ---- 11. Spoelknoppen en koptelefoon ---- */
const seek = (n, fwd, s = 30) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${fwd
  ? '<path d="M12.5 5.5L17 9l-4.5 3.5"></path><path d="M17 9h-6.5a5.5 5.5 0 100 11H16"></path>'
  : '<path d="M11.5 5.5L7 9l4.5 3.5"></path><path d="M7 9h6.5a5.5 5.5 0 110 11H8"></path>'}<text x="${fwd ? 8 : 9.5}" y="19.6" font-size="7.4" font-family="Instrument Sans, system-ui" font-weight="700" stroke="none" fill="currentColor">${n}</text></svg>`;
screens.Seek = { title: '11 · Instellingen → Afspelen', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Afspelen')}
  <div class="body pad">
    <div class="setgrp" style="padding-top:2px">Terugspoelen</div>
    <div class="seg"><span>5 s</span><span class="on">10 s</span><span>15 s</span><span>30 s</span></div>
    <div class="setgrp">Vooruitspoelen</div>
    <div class="seg"><span>10</span><span>15</span><span>30</span><span class="on">45</span><span>60</span></div>
    <div class="row" style="justify-content:center;gap:28px;margin-top:14px;padding:12px;border-radius:16px;background:var(--surf2)">
      <div class="col" style="align-items:center;gap:2px"><div class="tbtn" style="height:44px">${seek(10, false)}</div><b style="font-size:13px;font-variant-numeric:tabular-nums">−10 s</b></div><div class="pbig" style="width:56px;height:56px">${PAUSE(26)}</div><div class="col" style="align-items:center;gap:2px"><div class="tbtn" style="height:44px">${seek(45, true)}</div><b style="font-size:13px;font-variant-numeric:tabular-nums">+45 s</b></div>
    </div>
    <div class="note" style="color:var(--ink2);font-size:12px;padding-top:6px">Zo in de speler, de melding, op het vergrendelscherm en in de auto.</div>
    <div class="setgrp">Koptelefoon: dubbel en driedubbel tikken</div>
    <div class="col" style="gap:8px">
      <div class="opt on"><span class="rb"></span><div><b>Spoelen</b><span class="s">Dubbel: 45 s vooruit · driedubbel: 10 s terug</span></div></div>
      <div class="opt"><span class="rb"></span><div><b>Volgende in de wachtrij</b><span class="s">Dubbel: volgende · driedubbel: begin van de aflevering</span></div></div>
    </div>
    <div class="note" style="color:var(--ink2);font-size:12px;padding-top:8px">Geldt alleen voor de koptelefoon. De knoppen in de auto blijven spoelen.</div>
  </div>
  <div class="gest"><i></i></div>
</div>` };

/* ---- 13. Zoeken in één show ---- */
const hits = [
  ['14 mrt 2026', 'Sanne Kuipers over de nieuwe zaal', 'Gast: <mark>Sanne Kuipers</mark>, directeur van het poppodium, over het tweede balkon…', '62 min'],
  ['2 nov 2025', 'Wie betaalt de cultuur?', '…met wethouder Ali Demir en <mark>Sanne Kuipers</mark> van de nieuwe zaal over subsidie…', '48 min'],
  ['9 jun 2024', 'Live vanaf het dak', 'Een avond op het dak met muziek van Stadslicht. <mark>Kuipers</mark> belt in vanuit Lissabon…', '71 min']
];
screens.ShowSearch = { title: '13 · Podcastpagina → zoeken in deze show', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('De Deadline', ['share', 'more'])}
  <div class="body pad">
    <div class="tsearch" style="margin-top:2px">${K.search}<span>kuipers</span><span class="cnt">12 van 300</span><span class="ibtn" style="width:36px;height:36px">${ICON.close}</span></div>
    <div class="tnote">Gezocht in titels en shownotes van de <b>300 afleveringen in de feed</b></div>
    ${hits.map(h => `
    <div style="padding:12px 0;border-bottom:1px solid var(--line2)">
      <div class="t2" style="font-size:11.5px">${h[0]} · ${h[3]}</div>
      <div class="t1" style="font-size:14.5px;margin-top:3px;white-space:normal">${h[1].replace('Sanne Kuipers', '<mark class="hit">Sanne Kuipers</mark>')}</div>
      <div class="t2" style="font-size:12.5px;line-height:1.45;margin-top:4px;white-space:normal">${h[2].replace(/<mark>/g, '<mark class="hit">')}</div>
    </div>`).join('')}
    <div class="tlink" style="justify-content:center;color:var(--ink2)">Nog 9 ${K.chevS}</div>
    <div class="note" style="color:var(--ink2);font-size:11.5px;text-align:center">Niet wat je zoekt? <span style="color:var(--pri);font-weight:700">Zoek "kuipers" in heel Toadcast</span></div>
  </div>
  ${chrome('library')}
</div>` };

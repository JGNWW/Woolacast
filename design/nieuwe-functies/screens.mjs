// Telefoonschermen voor de tien nieuwe functies. Bouwt op design/base.css en de
// schermen van de mockup (NowPlaying, Library, ChartsEpisodes), en leent iconen en
// de gedeelde CSS van ../kanalen/screens.mjs.
// Alle podcasts, afleveringen, plekken en cijfers hieronder zijn verzonnen.
import { ICON as K, CSS as KCSS } from '../kanalen/screens.mjs';

const svg = (d, s = 22, w = 1.9) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;
export const ICON = {
  ...K,
  down: (s = 22) => svg('<path d="M12 4v10"></path><path d="M8 10l4 4 4-4"></path><path d="M5 19h14"></path>', s),
  done: (s = 20) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" fill="currentColor"></circle><path d="M7.5 12.3l3 3 6-6.3" fill="none" stroke="var(--bg)" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"></path></svg>`,
  ring: (p, s = 22) => { const c = 2 * Math.PI * 9; return `<svg width="${s}" height="${s}" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="var(--surf3)" stroke-width="2.6"></circle><circle cx="12" cy="12" r="9" fill="none" stroke="var(--pri)" stroke-width="2.6" stroke-linecap="round" stroke-dasharray="${(c * p).toFixed(1)} ${c.toFixed(1)}" transform="rotate(-90 12 12)"></circle><rect x="9.3" y="9.3" width="5.4" height="5.4" rx="1" fill="var(--ink)"></rect></svg>`; },
  wifi: (s = 16) => svg('<path d="M3 9.5a13 13 0 0118 0"></path><path d="M6.2 13a8.5 8.5 0 0111.6 0"></path><path d="M9.4 16.4a4 4 0 015.2 0"></path><circle cx="12" cy="19.4" r=".6" fill="currentColor"></circle>', s, 2),
  more: `<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5.5" r="1.8"></circle><circle cx="12" cy="12" r="1.8"></circle><circle cx="12" cy="18.5" r="1.8"></circle></svg>`,
  close: svg('<path d="M6 6l12 12M18 6L6 18"></path>', 22, 2),
  chevD: svg('<path d="M6 9l6 6 6-6"></path>', 24, 2),
  list: (s = 21) => svg('<path d="M9 6h11"></path><path d="M9 12h11"></path><path d="M9 18h11"></path><circle cx="4.5" cy="6" r=".8" fill="currentColor"></circle><circle cx="4.5" cy="12" r=".8" fill="currentColor"></circle><circle cx="4.5" cy="18" r=".8" fill="currentColor"></circle>', s),
  text: (s = 21) => svg('<path d="M5 6h14"></path><path d="M5 11h14"></path><path d="M5 16h9"></path>', s),
  speed: (s = 21) => svg('<path d="M4 17a8 8 0 1116 0"></path><path d="M12 13l4-3"></path>', s, 1.8),
  moon: (s = 21) => svg('<path d="M19.5 14.5A8 8 0 019.5 4.5a8 8 0 1010 10z"></path>', s, 1.8),
  queue: (s = 21) => svg('<path d="M4 7h11"></path><path d="M4 12h11"></path><path d="M4 17h7"></path><path d="M17 12v8"></path><path d="M20 15l-3-3-3 3"></path>', s, 1.8),
  file: (s = 22) => svg('<path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z"></path><path d="M14 3v5h5"></path>', s),
  link: (s = 18) => svg('<path d="M10 14a4 4 0 005.7 0l3-3a4 4 0 00-5.7-5.7l-1 1"></path><path d="M14 10a4 4 0 00-5.7 0l-3 3a4 4 0 005.7 5.7l1-1"></path>', s),
  warn: (s = 18) => svg('<path d="M12 4l9 16H3z"></path><path d="M12 10v4"></path><path d="M12 17.2v.2"></path>', s),
  star: (s = 18) => svg('<path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8-4.3-4.1 5.9-.8z"></path>', s),
  radio: (s = 20) => svg('<circle cx="12" cy="12" r="2.2"></circle><path d="M7.8 7.8a6 6 0 000 8.4"></path><path d="M16.2 7.8a6 6 0 010 8.4"></path><path d="M4.9 4.9a10 10 0 000 14.2"></path><path d="M19.1 4.9a10 10 0 010 14.2"></path>', s),
  scissors: (s = 20) => svg('<circle cx="6" cy="7" r="2.6"></circle><circle cx="6" cy="17" r="2.6"></circle><path d="M8.2 8.4L20 17"></path><path d="M8.2 15.6L20 7"></path>', s),
  voice: (s = 20) => svg('<path d="M4 10v4"></path><path d="M8 7v10"></path><path d="M12 4v16"></path><path d="M16 7v10"></path><path d="M20 10v4"></path>', s),
  gear: (s = 20) => svg('<circle cx="12" cy="12" r="3"></circle><path d="M12 2.8v2.4M12 18.8v2.4M4.2 7.5l2.1 1.2M17.7 15.3l2.1 1.2M4.2 16.5l2.1-1.2M17.7 8.7l2.1-1.2"></path>', s),
  mic: (s = 22) => svg('<rect x="9" y="3.5" width="6" height="11" rx="3"></rect><path d="M5.5 11a6.5 6.5 0 0013 0"></path><path d="M12 17.5V21"></path>', s),
  skipIn: (s = 18) => svg('<path d="M5 5v14"></path><path d="M9 12h10"></path><path d="M15 8l4 4-4 4"></path>', s),
  skipOut: (s = 18) => svg('<path d="M19 5v14"></path><path d="M5 12h10"></path><path d="M11 8l4 4-4 4"></path>', s),
  bellS: (s = 18) => K.bell('currentColor', s)
};

const UP = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 2l4.5 7h-9z"></path></svg>`;
const DOWN = `<svg width="9" height="9" viewBox="0 0 12 12" fill="currentColor"><path d="M6 10L1.5 3h9z"></path></svg>`;
const FLAG = `<span class="flag"><i style="background:#AE1C28"></i><i style="background:#FFFDFA"></i><i style="background:#21468B"></i></span>`;
const R15 = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M11.5 5.5L7 9l4.5 3.5"></path><path d="M7 9h6.5a5.5 5.5 0 110 11H8"></path><text x="9.5" y="19.6" font-size="7.4" font-family="Instrument Sans, system-ui" font-weight="700" stroke="none" fill="currentColor">15</text></svg>`;
const F30 = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12.5 5.5L17 9l-4.5 3.5"></path><path d="M17 9h-6.5a5.5 5.5 0 100 11H16"></path><text x="8" y="19.6" font-size="7.4" font-family="Instrument Sans, system-ui" font-weight="700" stroke="none" fill="currentColor">30</text></svg>`;
const PREV = `<svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M7 6h2.4v12H7z"></path><path d="M19 6v12l-9-6z"></path></svg>`;
const NEXT = `<svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M14.6 6H17v12h-2.4z"></path><path d="M5 6v12l9-6z"></path></svg>`;
const PAUSE = (s = 34) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="currentColor"><rect x="7" y="4.5" width="3.8" height="15" rx="1.3"></rect><rect x="13.2" y="4.5" width="3.8" height="15" rx="1.3"></rect></svg>`;

const chrome = (active) => `
  <div class="mini">
    <div class="art a1"></div>
    <div class="meta">
      <div class="t1 on-mini">Live vanuit Paradiso</div>
      <div class="t2 on-mini2">De Deadline · nog 18 min</div>
    </div>
    <div class="ibtn on-mini">${K.pause}</div>
    <div class="ibtn on-mini">${K.fwd}</div>
  </div>
  <div class="nav">
    <div class="navi ${active === 'charts' ? 'on' : ''}">${K.navCharts}Hitlijsten</div>
    <div class="navi ${active === 'discover' ? 'on' : ''}">${K.navDisc}Ontdek</div>
    <div class="navi ${active === 'library' ? 'on' : ''}">${K.navLib}Bibliotheek</div>
  </div>
  <div class="gest"><i></i></div>`;

const appbar = (actions, mark = 'Toadcast') => `
  <div class="appbar">
    <div class="mark">${mark}</div>
    <div class="row" style="gap:0">${actions.map(a => `<div class="ibtn">${typeof ICON[a] === 'function' ? ICON[a]() : ICON[a]}</div>`).join('')}</div>
  </div>`;
const titlebar = (title, right = []) => `
  <div class="appbar" style="padding-left:6px">
    <div class="row" style="gap:2px"><div class="ibtn">${K.back}</div><div style="font-size:16px;font-weight:700;letter-spacing:-.01em">${title}</div></div>
    <div class="row" style="gap:0">${right.map(a => `<div class="ibtn">${typeof ICON[a] === 'function' ? ICON[a]() : ICON[a]}</div>`).join('')}</div>
  </div>`;
const tabs = (items, on) => `
    <div class="pad tabs${items.length > 3 ? ' tight' : ''}">${items.map(t => `<div class="tab ${t === on ? 'on' : ''}">${t}</div>`).join('')}</div>
    <div class="hr"></div>`;
const label = (t, extra = '') => `<div class="pad lbl2" style="color:var(--ink2);${extra}">${t}</div>`;
const toggle = (on) => `<span class="sw ${on ? 'on' : ''}" role="switch" aria-checked="${on}"><i></i></span>`;
const mv = (d, n) => d === 'up' ? `<div class="mv" style="color:var(--up)">${UP}${n}</div>`
  : d === 'down' ? `<div class="mv" style="color:var(--down)">${DOWN}${n}</div>`
  : d === 'new' ? `<div class="mv new">NIEUW</div>` : `<div class="mv flat" style="color:var(--ink2)">=</div>`;

// Kop van de speler, gedeeld door hoofdstukken, tekst en snelheid.
const playerHead = (sub = 'De Deadline') => `
  <div class="appbar">
    <div class="ibtn" style="margin-left:-12px">${ICON.chevD}</div>
    <div style="text-align:center">
      <div class="eyebrow" style="font-size:10px;color:var(--ink2)">Speelt nu uit</div>
      <div style="font-size:12.5px;font-weight:700;margin-top:2px">${sub}</div>
    </div>
    <div class="ibtn">${ICON.more}</div>
  </div>`;
const sheet = (inner, top) => `
  <div class="scrim"></div>
  <div class="sheet" style="top:${top}px">
    <div class="grab"><i></i></div>
    ${inner}
  </div>`;

export const CSS = KCSS + `
.npart{border-radius:20px;margin:0 auto;box-shadow:0 14px 36px rgba(33,29,23,.26)}
.ctxchip{display:inline-flex;align-items:center;gap:7px;height:34px;padding:0 13px;border-radius:10px;background:var(--priC);color:var(--priOn);font-size:12.5px;font-weight:700}
.bar{height:5px;border-radius:3px;background:var(--surf3);position:relative}
.bar b{position:absolute;left:0;top:0;height:5px;border-radius:3px;background:var(--pri);display:block}
.bar u{position:absolute;top:-5px;width:15px;height:15px;border-radius:8px;background:var(--pri);border:3px solid var(--bg);display:block}
.bar s{position:absolute;top:-1px;width:3px;height:7px;background:var(--bg);display:block;text-decoration:none}
.times{display:flex;justify-content:space-between;font-size:11.5px;color:var(--ink2);font-variant-numeric:tabular-nums;font-weight:600;margin-top:9px}
.tbtn{width:56px;height:56px;border-radius:28px;display:flex;align-items:center;justify-content:center;color:var(--ink)}
.pbig{width:72px;height:72px;border-radius:36px;background:var(--pri);color:var(--priInk);display:flex;align-items:center;justify-content:center;box-shadow:0 6px 18px rgba(196,84,43,.34)}
.tools{display:flex;justify-content:space-between;align-items:center}
.tool{display:flex;flex-direction:column;align-items:center;gap:5px;width:56px;height:56px;justify-content:center;color:var(--ink2);font-size:10.5px;font-weight:600}
.tool.on{color:var(--pri)}
.chap{display:flex;align-items:center;gap:8px;height:40px;padding:0 12px 0 10px;border-radius:12px;background:var(--surf2);font-size:13px;font-weight:600}
.chap .n{font-size:11px;font-weight:700;color:var(--ink2);font-variant-numeric:tabular-nums}
.scrim{position:absolute;inset:0;background:rgba(33,29,23,.38)}
.sheet{position:absolute;left:0;right:0;bottom:0;background:var(--bg);border-radius:24px 24px 0 0;box-shadow:0 -10px 30px rgba(33,29,23,.18);display:flex;flex-direction:column}
.ph>.scrim,.ph>.sheet{position:absolute}
.grab{height:22px;display:flex;align-items:center;justify-content:center;flex:none}
.grab i{width:36px;height:4px;border-radius:2px;background:var(--line);display:block}
.shead{display:flex;align-items:center;justify-content:space-between;padding:4px 12px 8px 20px}
.shead h2{font-size:19px;font-weight:700;letter-spacing:-.014em;margin:0}
.crow{display:flex;align-items:center;gap:12px;min-height:56px;border-bottom:1px solid var(--line2)}
.crow .ts{width:46px;font-size:12.5px;font-weight:600;color:var(--ink2);font-variant-numeric:tabular-nums;flex:none}
.crow .ct{flex:1 1 auto;font-size:14.5px;font-weight:500}
.crow.on .ct{font-weight:700}
.crow.on .ts{color:var(--pri)}
.crow.past .ct{color:var(--ink2)}
.eq{display:flex;align-items:flex-end;gap:2px;height:14px;color:var(--pri)}
.eq i{width:3px;border-radius:1px;background:currentColor;display:block}
.srow{display:flex;align-items:center;gap:14px;min-height:64px;border-bottom:1px solid var(--line2)}
.srow .ic{width:40px;height:40px;border-radius:12px;background:var(--surf2);display:flex;align-items:center;justify-content:center;color:var(--ink);flex:none}
.srow .tx{flex:1 1 auto;min-width:0}
.srow .tx b{display:block;font-size:14.5px;font-weight:600}
.srow .tx span{display:block;font-size:12.5px;color:var(--ink2);margin-top:2px}
.sw{width:46px;height:28px;border-radius:14px;background:var(--surf3);border:2px solid var(--ink2);position:relative;flex:none;display:block}
.sw i{position:absolute;top:4px;left:4px;width:16px;height:16px;border-radius:8px;background:var(--ink2);display:block}
.sw.on{background:var(--pri);border-color:var(--pri)}
.sw.on i{left:auto;right:3px;top:3px;width:18px;height:18px;border-radius:9px;background:var(--priInk)}
.seg{display:flex;gap:6px}
.seg span{flex:1 1 0;height:44px;border-radius:12px;display:flex;align-items:center;justify-content:center;background:var(--surf);border:1px solid var(--line);font-size:14px;font-weight:700;font-variant-numeric:tabular-nums;color:var(--ink)}
.seg span.on{background:var(--ink);border-color:var(--ink);color:var(--bg)}
.saved{display:flex;align-items:center;gap:12px;padding:14px 16px;border-radius:16px;background:var(--surf2)}
.saved .big{font-size:22px;font-weight:800;letter-spacing:-.02em;font-variant-numeric:tabular-nums}
.tx-line{font-size:17px;line-height:1.5;color:var(--ink2);padding:6px 10px;margin:0 -10px;border-radius:10px}
.tx-line.on{color:var(--ink);background:var(--priC);font-weight:600}
.tx-line.on mark{background:transparent;color:var(--priOn);text-decoration:underline;text-decoration-thickness:2px;text-underline-offset:3px}
.tx-line mark{background:#F6D9A8;color:var(--ink);border-radius:3px;padding:0 1px}
.who{font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:var(--ink2);margin:10px 0 2px}
.tsearch{display:flex;align-items:center;gap:10px;height:44px;padding:0 8px 0 14px;border-radius:12px;background:var(--surf2);font-size:14px;color:var(--ink)}
.tsearch .cnt{margin-left:auto;font-size:12.5px;color:var(--ink2);font-weight:600;font-variant-numeric:tabular-nums}
.erow2{display:flex;gap:12px;padding:12px 0;border-bottom:1px solid var(--line2);align-items:center}
.erow2 .ebody{flex:1 1 auto;min-width:0}
.dstate{width:44px;height:44px;border-radius:22px;display:flex;align-items:center;justify-content:center;flex:none;color:var(--ink)}
.dstate.ok{color:var(--up)}
.storage{height:10px;border-radius:5px;background:var(--surf3);display:flex;overflow:hidden}
.storage i{display:block;height:100%}
.tabs.tight{gap:17px}.tabs.tight .tab{font-size:14.5px}
.wk .flag,.note .flag{display:inline-flex;vertical-align:-2px;margin:0 3px}
.tag{display:inline-flex;align-items:center;gap:4px;height:20px;padding:0 7px;border-radius:6px;font-size:10.5px;font-weight:700;background:var(--surf2);color:var(--ink);letter-spacing:.02em}
.tag.ok{background:#DCF0E4;color:#0B6E3E}
.tag.pri{background:var(--priC);color:var(--priOn)}
.wk{border-radius:18px;background:var(--panel);color:var(--onPanel);padding:16px 16px 6px}
.wk .h{font-size:17px;font-weight:700;letter-spacing:-.01em}
.wk .s{font-size:12px;color:var(--onPanelMuted);margin-top:2px}
.wrow{display:flex;align-items:center;gap:11px;height:54px;border-top:1px solid rgba(246,239,229,.13)}
.wrow .art{width:36px;height:36px;border-radius:9px;box-shadow:none}
.wrow .t1{font-size:13.5px;color:var(--onPanel)}.wrow .t2{font-size:11.5px;color:var(--onPanelMuted)}
.wrow .mv{width:auto}
.wpill{display:inline-flex;align-items:center;gap:3px;height:22px;padding:0 8px;border-radius:7px;font-size:11.5px;font-weight:700;font-variant-numeric:tabular-nums}
.wpill.up{background:rgba(76,190,126,.18);color:#7FD9A4}.wpill.down{background:rgba(255,122,147,.16);color:#FFA3B4}.wpill.new{background:rgba(240,137,91,.2);color:#F0A87F}
.notif{margin:0 10px;border-radius:22px;background:var(--surf);border:1px solid var(--line2);box-shadow:0 8px 24px rgba(33,29,23,.14);padding:12px 14px}
.notif .app{display:flex;align-items:center;gap:6px;font-size:11.5px;color:var(--ink2);font-weight:600}
.notif .app i{width:16px;height:16px;border-radius:5px;background:var(--pri);display:block}
.stat{border-radius:16px;background:var(--surf);border:1px solid var(--line2);padding:14px 16px}
.stat .big{font-size:34px;font-weight:800;letter-spacing:-.035em;line-height:1;font-variant-numeric:tabular-nums}
.stat .lb{font-size:12.5px;color:var(--ink2);margin-top:4px}
.bars{display:flex;align-items:flex-end;gap:5px;height:64px}
.bars i{flex:1 1 0;border-radius:4px 4px 1px 1px;background:var(--surf3);display:block}
.bars i.on{background:var(--pri)}
.early{border-radius:18px;background:var(--panel);color:var(--onPanel);padding:16px}
.early .t2{color:var(--onPanelMuted)}
.opml-hero{display:flex;align-items:center;gap:14px;padding:16px;border-radius:18px;background:var(--surf);border:1px solid var(--line2)}
.bigcheck{width:48px;height:48px;border-radius:24px;background:#DCF0E4;color:#0B6E3E;display:flex;align-items:center;justify-content:center;flex:none}
.sumrow{display:flex;align-items:center;gap:12px;min-height:52px;border-bottom:1px solid var(--line2);font-size:14px}
.sumrow .n{margin-left:auto;font-weight:700;font-variant-numeric:tabular-nums}
.sumrow .dot{width:10px;height:10px;border-radius:5px;flex:none}
.radio-bar{display:flex;align-items:center;gap:12px;padding:12px 12px 12px 14px;border-radius:16px;background:var(--panel);color:var(--onPanel)}
.radio-bar .t2{color:var(--onPanelMuted)}
.radio-bar .go{height:40px;padding:0 16px;border-radius:12px;background:var(--pri);color:var(--priInk);display:flex;align-items:center;gap:6px;font-size:13.5px;font-weight:700;margin-left:auto;flex:none}
.opt{display:flex;align-items:center;gap:12px;min-height:60px;padding:0 14px;border-radius:14px;border:1px solid var(--line);background:var(--surf)}
.opt.on{border:2px solid var(--pri);background:var(--priC)}
.opt .rb{width:20px;height:20px;border-radius:10px;border:2px solid var(--ink2);flex:none;position:relative}
.opt.on .rb{border-color:var(--pri)}
.opt.on .rb:after{content:"";position:absolute;inset:3px;border-radius:5px;background:var(--pri)}
.opt b{display:block;font-size:14.5px;font-weight:600}.opt span.s{display:block;font-size:12.5px;color:var(--ink2);margin-top:1px}
.qmini{display:flex;align-items:center;gap:10px;height:48px;border-bottom:1px solid var(--line2);font-size:13.5px}
.qmini .n{width:18px;font-weight:800;color:var(--ink2);text-align:right;font-variant-numeric:tabular-nums}
.qmini .art{width:32px;height:32px;border-radius:8px;box-shadow:none}
.qmini .skip{margin-left:auto;font-size:11.5px;color:var(--ink2);font-weight:600}
.stepper{display:flex;align-items:center;gap:0;border:1px solid var(--line);border-radius:12px;height:40px;background:var(--surf);flex:none}
.stepper span{width:40px;height:38px;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:600;color:var(--ink)}
.stepper b{min-width:52px;text-align:center;font-size:14px;font-variant-numeric:tabular-nums}
.valchip{height:36px;padding:0 12px;border-radius:10px;background:var(--surf2);display:flex;align-items:center;gap:6px;font-size:13px;font-weight:700;flex:none}
.valchip.def{background:transparent;border:1px solid var(--line);color:var(--ink2);font-weight:600}
.ownset{display:inline-flex;align-items:center;gap:6px;height:28px;padding:0 10px;border-radius:8px;background:var(--surf2);font-size:12px;font-weight:600;color:var(--ink)}
/* Auto: het scherm van de auto, niet de telefoon. Donker, grote doelen. */
.car{width:800px;height:480px;background:#0B0C0E;color:#EDEEF0;font-family:'Instrument Sans',ui-sans-serif,system-ui,sans-serif;display:flex;overflow:hidden;position:relative;-webkit-font-smoothing:antialiased}
.car .rail{width:88px;background:#141518;display:flex;flex-direction:column;align-items:center;justify-content:space-between;padding:18px 0;flex:none;color:#A0A3AA}
.car .rail .ri{width:56px;height:56px;border-radius:28px;display:flex;align-items:center;justify-content:center}
.car .rail .ri.on{background:#28292E;color:#EDEEF0}
.car .main{flex:1 1 auto;min-width:0;display:flex;flex-direction:column;padding:16px 22px 0}
.car .ctabs{display:flex;gap:6px;flex:none}
.car .ct{height:52px;padding:0 20px;border-radius:26px;display:flex;align-items:center;font-size:17px;font-weight:600;color:#A0A3AA}
.car .ct.on{background:#28292E;color:#EDEEF0}
.car .items{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px 14px;margin-top:14px}
.car .it{display:flex;align-items:center;gap:14px;height:84px;padding:0 14px;border-radius:18px;background:#16171A}
.car .it .art{width:60px;height:60px;border-radius:12px;box-shadow:none}
.car .it .t1{font-size:17px;font-weight:600;color:#EDEEF0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.car .it{min-width:0}
.car .it .t2{font-size:14px;color:#A0A3AA}
.car .rk{font-family:'Bricolage Grotesque','Instrument Sans',sans-serif;font-size:22px;font-weight:800;color:#F0895B;width:28px;text-align:right;flex:none}
.car .np{height:92px;display:flex;align-items:center;gap:16px;margin:auto -22px 0;padding:0 22px;background:#16171A;border-top:1px solid #2C2E33}
.car .np .art{width:60px;height:60px;border-radius:12px}
.car .cb{width:64px;height:64px;border-radius:32px;display:flex;align-items:center;justify-content:center;color:#EDEEF0;flex:none}
.car .cb.p{background:#F0895B;color:#2A1006}
.car .cb.sp{font-size:17px;font-weight:800;border:2px solid #3A3C42;font-variant-numeric:tabular-nums}
.car .prog{position:absolute;left:88px;right:0;bottom:92px;height:3px;background:#2C2E33}
.car .prog b{display:block;height:3px;width:62%;background:#F0895B}
`;

export const screens = {};

/* ---- 1. Downloaden ---- */
screens.Downloads = { title: '1 · Bibliotheek → Gedownload', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'more'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Bibliotheek</h1></div>
    ${tabs(['Gevolgd', 'Wachtrij', 'Bewaard', 'Gedownload'], 'Gedownload')}
    <div class="pad" style="padding-top:14px">
      <div class="row" style="justify-content:space-between;font-size:12.5px;margin-bottom:8px">
        <span><b>1,2 GB</b> <span style="color:var(--ink2)">van 2 GB</span></span>
        <span style="color:var(--ink2)">14 afleveringen</span>
      </div>
      <div class="storage" role="img" aria-label="1,2 GB van 2 GB gebruikt: 0,9 GB automatisch, 0,3 GB zelf gedownload"><i style="width:45%;background:var(--pri)"></i><i style="width:15%;background:var(--ink2)"></i></div>
      <div class="row" style="gap:14px;font-size:11.5px;color:var(--ink2);margin-top:8px">
        <span class="row" style="gap:5px"><i style="width:9px;height:9px;border-radius:3px;background:var(--pri);display:block"></i>Automatisch</span>
        <span class="row" style="gap:5px"><i style="width:9px;height:9px;border-radius:3px;background:var(--ink2);display:block"></i>Zelf gedownload</span>
      </div>
    </div>
    <div class="pad" style="padding-top:12px">
      <div class="linkcard">
        <span style="color:var(--ink)">${ICON.wifi(20)}</span>
        <div class="meta"><div class="t1" style="font-size:13.5px">Automatisch: nieuwste 1 per show</div><div class="t2">Alleen op wifi · 6 van 11 shows</div></div>
        <span class="chev">${K.chevR()}</span>
      </div>
    </div>
    ${label('Bezig')}
    <div class="pad">
      <div class="erow2" style="padding-top:0">
        <div class="art a7" style="width:48px;height:48px"></div>
        <div class="ebody"><div class="t1">Het mes in de polder</div><div class="t2">Koud Spoor · 62 min · 38 van 58 MB</div></div>
        <div class="dstate" aria-label="Wordt gedownload, 65 procent. Tik om te stoppen">${ICON.ring(.65)}</div>
      </div>
    </div>
    ${label('Klaar om te luisteren')}
    <div class="pad">
      <div class="erow2" style="padding-top:0">
        <div class="art a2" style="width:48px;height:48px"></div>
        <div class="ebody"><div class="t1">De nachtbus naar huis</div><div class="t2">Nachtdienst · 44 min</div><div class="row" style="gap:6px;margin-top:5px"><span class="tag">Automatisch</span></div></div>
        <div class="dstate ok" aria-label="Gedownload">${ICON.done()}</div>
      </div>
      <div class="erow2">
        <div class="art a1" style="width:48px;height:48px"></div>
        <div class="ebody"><div class="t1">Live vanuit Paradiso</div><div class="t2">De Deadline · nog 18 min</div><div class="row" style="gap:6px;margin-top:5px"><span class="tag pri">In wachtrij · blijft staan</span></div></div>
        <div class="dstate ok" aria-label="Gedownload">${ICON.done()}</div>
      </div>
    </div>
  </div>
  ${chrome('library')}
</div>` };

screens.DownloadsSheet = { title: '1 · Automatisch downloaden per show', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('', ['share', 'more'])}
  <div class="body pad">
    <div class="row" style="gap:14px"><div class="art a2" style="width:88px;height:88px;border-radius:16px"></div>
      <div><div class="eyebrow" style="color:var(--ink2)">Podcast</div><div class="dsp" style="font-size:24px;font-weight:700;letter-spacing:-.02em">Nachtdienst</div><div class="t2">Kade Media</div></div></div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Downloaden</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="srow"><div class="ic">${ICON.down(20)}</div><div class="tx"><b>Automatisch downloaden</b><span>Nieuwe afleveringen van Nachtdienst</span></div>${toggle(true)}</div>
      <div class="srow"><div class="tx"><b>Hoeveel</b><span>De nieuwste, oudere ruimt de app op</span></div><div class="stepper"><span aria-label="minder">−</span><b>1</b><span aria-label="meer">+</span></div></div>
      <div class="srow"><div class="ic">${ICON.wifi(20)}</div><div class="tx"><b>Alleen op wifi</b><span>Geldt voor alle shows · Instellingen</span></div>${toggle(true)}</div>
      <div class="srow" style="border-bottom:0"><div class="ic">${ICON.done(20)}</div><div class="tx"><b>Na beluisteren wissen</b><span>Na 24 uur. Bewaard en in de wachtrij blijft altijd staan.</span></div>${toggle(true)}</div>
      <div class="note" style="color:var(--ink2);font-size:12.5px;padding:8px 0 20px">Opslag: 1,2 van 2 GB gebruikt. De grens staat in Instellingen.</div>
    </div>`, 300)}
</div>` };

/* ---- 2. Hoofdstukken ---- */
const chapters = [
  ['0:00', 'Opening en nieuws van de week', 'past'],
  ['6:12', 'Gast: Sanne Kuipers over de nieuwe zaal', 'past'],
  ['24:40', 'Live: "Stadslicht" met band', 'on'],
  ['38:05', 'De vraag van de luisteraar', ''],
  ['52:30', 'Wat we volgende week doen', '']
];
screens.Chapters = { title: '2 · Speler met hoofdstukken', body: `
<div class="ph col">
  <div class="sa"></div>
  ${playerHead()}
  <div class="body pad" style="padding-top:6px">
    <div class="npart a1" style="width:236px;height:236px"></div>
    <div style="padding-top:20px">
      <div class="dsp" style="font-size:22px;font-weight:700;letter-spacing:-.024em;line-height:1.15">Live vanuit Paradiso</div>
      <div class="t2" style="margin-top:5px;font-size:13.5px">De Deadline · 8 sep · 71 min</div>
    </div>
    <div style="padding-top:14px"><div class="chap"><span class="n">3/5</span>Live: "Stadslicht" met band<span style="margin-left:auto" class="chev">${ICON.list(18)}</span></div></div>
    <div style="padding-top:18px">
      <div class="bar" role="img" aria-label="Voortgang 29 van 71 minuten, 5 hoofdstukken"><b style="width:41%"></b><s style="left:8.7%"></s><s style="left:34.7%"></s><s style="left:53.6%"></s><s style="left:73.9%"></s><u style="left:calc(41% - 7px)"></u></div>
      <div class="times"><span>29:08</span><span>nog 8:57 in dit hoofdstuk</span><span>-41:52</span></div>
    </div>
    <div class="row" style="justify-content:space-between;padding-top:12px">
      <div class="tbtn">${R15}</div><div class="tbtn">${PREV}</div><div class="pbig">${PAUSE()}</div><div class="tbtn">${NEXT}</div><div class="tbtn">${F30}</div>
    </div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Hoofdstukken</h2><span class="note" style="color:var(--ink2);font-size:12px;padding-right:8px">van de maker</span></div>
    <div class="pad">${chapters.map(c => `
      <div class="crow ${c[2]}"><span class="ts">${c[0]}</span><span class="ct">${c[1]}</span>${c[2] === 'on' ? `<span class="eq" aria-label="speelt nu"><i style="height:8px"></i><i style="height:14px"></i><i style="height:10px"></i></span>` : ''}</div>`).join('')}
      <div class="srow" style="border-bottom:0;min-height:58px"><div class="ic">${ICON.moon(20)}</div><div class="tx"><b>Slaaptimer: einde van dit hoofdstuk</b><span>Stopt om 38:05</span></div></div>
    </div>`, 404)}
</div>` };

/* ---- 3. Transcriptie ---- */
screens.Transcript = { title: '3 · Speler → Tekst, zoeken op "zaal"', body: `
<div class="ph col">
  <div class="sa"></div>
  ${playerHead()}
  <div class="pad row" style="gap:12px;padding-bottom:10px">
    <div class="art a1" style="width:48px;height:48px"></div>
    <div class="meta"><div class="t1">Live vanuit Paradiso</div><div class="t2">De Deadline · 29:08 / 71:00</div></div>
  </div>
  ${tabs(['Speler', 'Hoofdstukken', 'Tekst'], 'Tekst')}
  <div class="body pad" style="padding-top:12px">
    <div class="tsearch">${K.search}<span>zaal</span><span class="cnt">2 van 4</span><span class="ibtn" style="width:36px;height:36px">${ICON.chevD}</span></div>
    <div style="padding-top:10px">
      <div class="who">Sanne</div>
      <div class="tx-line">De nieuwe <mark>zaal</mark> gaat pas in maart open, dus dit is echt de laatste keer hier.</div>
      <div class="who">Joost</div>
      <div class="tx-line">En dan de band. Zijn jullie er klaar voor?</div>
      <div class="tx-line on" aria-current="true">Dit nummer schreven we in de oude <mark>zaal</mark>, op de avond dat het dak lekte.</div>
      <div class="tx-line">Het heet Stadslicht.</div>
      <div class="who">Sanne</div>
      <div class="tx-line">Ik weet nog dat we emmers neerzetten tussen het publiek.</div>
    </div>
  </div>
  <div class="pad row" style="gap:12px;height:76px;border-top:1px solid var(--line2);justify-content:space-between">
    <div class="tbtn">${R15}</div><div class="pbig" style="width:56px;height:56px">${PAUSE(26)}</div><div class="tbtn">${F30}</div>
    <div class="note" style="color:var(--ink2);font-size:11.5px;width:112px;text-align:right">Tekst van de maker · tik op een zin om te springen</div>
  </div>
  <div class="gest"><i></i></div>
</div>` };

/* ---- 4. Stilte inkorten en stemversterking ---- */
screens.Speed = { title: '4 · Snelheidsmenu met inkorten en stem', body: `
<div class="ph col">
  <div class="sa"></div>
  ${playerHead()}
  <div class="body pad" style="padding-top:6px">
    <div class="npart a1" style="width:236px;height:236px"></div>
    <div style="padding-top:20px"><div class="dsp" style="font-size:22px;font-weight:700;letter-spacing:-.024em">Live vanuit Paradiso</div></div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Afspelen</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="lbl2" style="margin-top:0;color:var(--ink2)">Snelheid</div>
      <div class="seg"><span>0,8×</span><span>1×</span><span class="on">1,2×</span><span>1,5×</span><span>2×</span></div>
      <div class="srow" style="margin-top:6px"><div class="ic">${ICON.scissors()}</div><div class="tx"><b>Stiltes inkorten</b><span>Slaat pauzes over, zonder sneller te praten</span></div>${toggle(true)}</div>
      <div class="srow"><div class="ic">${ICON.voice()}</div><div class="tx"><b>Stemversterking</b><span>Zachte stemmen harder, harde muziek zachter</span></div>${toggle(false)}</div>
      <div class="saved" style="margin-top:14px">
        <span style="color:var(--pri)">${ICON.scissors(24)}</span>
        <div><div class="big">3 u 12 min</div><div style="font-size:12.5px;color:var(--ink2)">bespaard met inkorten, gemeten sinds 2 sep</div></div>
      </div>
      <div class="note" style="color:var(--ink2);font-size:12.5px;padding:10px 0 20px">Onthoud voor De Deadline <span style="color:var(--pri);font-weight:700">· Instellingen voor deze show</span></div>
    </div>`, 318)}
</div>` };

/* ---- 5. Weekoverzicht ---- */
screens.Week = { title: '5 · Weekoverzicht op Ontdek', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Ontdek</h1></div>
    <div class="pad">
      <div class="wk">
        <div class="row" style="justify-content:space-between;align-items:flex-start">
          <div><div class="h">Jouw week in de lijsten</div><div class="s">22–28 sep · Apple ${FLAG} NL · 3 lijsten</div></div>
          <span class="chev" style="color:var(--onPanelMuted)">${K.chevR()}</span>
        </div>
        <div class="ph-s" style="font-size:10.5px;letter-spacing:.1em;text-transform:uppercase;font-weight:700;color:var(--onPanelMuted);margin:14px 0 4px">Jouw shows</div>
        <div class="wrow"><div class="art a6"></div><div class="meta"><div class="t1">Het Vijfde Kwartier</div><div class="t2">#47 → #6 · Alle categorieën</div></div><span class="wpill up">${UP}41</span></div>
        <div class="wrow"><div class="art a7"></div><div class="meta"><div class="t1">Koud Spoor</div><div class="t2">#3 → #8 · True crime</div></div><span class="wpill down">${DOWN}5</span></div>
        <div class="ph-s" style="font-size:10.5px;letter-spacing:.1em;text-transform:uppercase;font-weight:700;color:var(--onPanelMuted);margin:14px 0 4px">Grootste stijgers</div>
        <div class="wrow"><div class="art a10"></div><div class="meta"><div class="t1">Kade 12</div><div class="t2">#88 → #19 · True crime</div></div><span class="wpill up">${UP}69</span></div>
        <div class="wrow"><div class="art a3"></div><div class="meta"><div class="t1">Tafel voor Twee</div><div class="t2">Nieuw in de top 10 · #7</div></div><span class="wpill new">NIEUW</span></div>
      </div>
    </div>
    <div class="pad tnote" style="padding-top:6px">Uit de dagelijkse historie van de verzamelaar · elke maandag nieuw</div>
    <div class="pad" style="padding-top:10px"><div class="sect"><h2 class="h2 dsp">Stijgers vandaag</h2><span style="font-size:13px;font-weight:600;color:var(--pri)">Alles</span></div></div>
    <div class="pad list">
      <div class="lrow"><div class="rank top">4</div><div class="art a5"></div><div class="meta"><div class="t1">Lange Adem</div><div class="t2">Studio Hemel · Sport</div></div>${mv('up', 12)}</div>
    </div>
  </div>
  ${chrome('discover')}
</div>` };

screens.WeekNotif = { title: '5 · De melding op maandag', body: `
<div class="ph col" style="background:linear-gradient(160deg,#2B4C7E,#14504E)">
  <div style="height:120px"></div>
  <div style="text-align:center;color:#FFFDFA"><div class="dsp" style="font-size:72px;font-weight:600;letter-spacing:-.03em;line-height:1">07:30</div><div style="font-size:15px;margin-top:6px;opacity:.9">maandag 29 september</div></div>
  <div style="height:40px"></div>
  <div class="notif">
    <div class="app"><i></i>Toadcast · Weekoverzicht · nu</div>
    <div style="font-size:14.5px;font-weight:700;margin-top:6px">Het Vijfde Kwartier steeg 41 plekken</div>
    <div style="font-size:13px;color:var(--ink2);margin-top:2px;line-height:1.4">Nu #6 in Apple NL. Ook: Kade 12 +69 in True crime, Tafel voor Twee nieuw in de top 10.</div>
    <div class="row" style="gap:18px;margin-top:10px;font-size:13px;font-weight:700;color:var(--pri)"><span>Bekijk de week</span><span style="color:var(--ink2)">Meldingen instellen</span></div>
  </div>
  <div style="padding:14px 22px;color:#FFFDFA;font-size:12.5px;opacity:.85;line-height:1.45">Eén melding per week, in een eigen kanaal dat je los kunt uitzetten. De dagelijkse kaart Chart-alerts in de Bibliotheek blijft.</div>
</div>` };

/* ---- 6. Terugblik ---- */
const weeks = [3, 5, 4, 6, 8, 5, 7, 9, 6, 8, 11, 7];
screens.Recap = { title: '6 · Terugblik: september', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Terugblik', ['share'])}
  <div class="body pad">
    <div class="row" style="gap:8px;padding-bottom:12px"><div class="fchip on">September</div><div class="fchip">2026</div><div class="fchip">Alles</div></div>
    <div class="row" style="gap:10px">
      <div class="stat" style="flex:1"><div class="big dsp">31 u</div><div class="lb">geluisterd, 7 u meer dan augustus</div></div>
      <div class="stat" style="flex:1"><div class="big dsp">12</div><div class="lb">dagen op rij, je langste reeks</div></div>
    </div>
    <div class="stat" style="margin-top:10px">
      <div class="row" style="justify-content:space-between;font-size:12.5px;color:var(--ink2);margin-bottom:10px"><b style="color:var(--ink)">Uren per week</b><span>laatste 12 weken</span></div>
      <div class="bars" role="img" aria-label="Uren per week over de laatste 12 weken, hoogste 11 uur">${weeks.map((h, i) => `<i style="height:${h / 11 * 100}%" class="${i === 10 ? 'on' : ''}"></i>`).join('')}</div>
    </div>
    <div class="lbl2" style="color:var(--ink2)">Meest geluisterd</div>
    <div class="list">
      <div class="lrow" style="height:56px"><div class="rank top">1</div><div class="art a1" style="width:40px;height:40px"></div><div class="meta"><div class="t1">De Deadline</div><div class="t2">9 u 40 min · 14 afleveringen</div></div></div>
      <div class="lrow" style="height:56px"><div class="rank top">2</div><div class="art a7" style="width:40px;height:40px"></div><div class="meta"><div class="t1">Koud Spoor</div><div class="t2">6 u 05 min · 6 afleveringen</div></div></div>
    </div>
    <div class="early" style="margin-top:12px">
      <div class="row" style="gap:8px;color:#F0A87F;font-size:11px;font-weight:700;letter-spacing:.1em;text-transform:uppercase">${ICON.star(15)}Vroege luisteraar</div>
      <div style="font-size:15px;font-weight:700;margin-top:6px;line-height:1.3">Je volgde Het Vijfde Kwartier 23 dagen voordat het de top 20 haalde</div>
      <div class="t2" style="font-size:12px;margin-top:4px">Apple NL · Alle categorieën · top 20 op 26 sep</div>
    </div>
  </div>
  ${chrome('library')}
</div>` };

screens.RecapCard = { title: '6 · De deelbare kaart (op het toestel gemaakt)', body: `
<div class="ph col" style="background:#1E1B16;align-items:center;justify-content:center">
  <div style="width:320px;border-radius:26px;background:linear-gradient(165deg,#C4542B,#7E3149);color:#FFFDFA;padding:26px 24px;box-shadow:0 20px 50px rgba(0,0,0,.4)">
    <div class="mark" style="color:#FFE3D0">Toadcast · september</div>
    <div class="dsp" style="font-size:60px;font-weight:800;letter-spacing:-.04em;line-height:1;margin-top:18px">31 uur</div>
    <div style="font-size:15px;opacity:.92;margin-top:6px">geluisterd naar 9 podcasts</div>
    <div class="row" style="gap:10px;margin-top:22px"><div class="art a1" style="width:64px;height:64px;border-radius:14px"></div><div class="art a7" style="width:64px;height:64px;border-radius:14px"></div><div class="art a6" style="width:64px;height:64px;border-radius:14px"></div></div>
    <div style="margin-top:22px;padding-top:16px;border-top:1px solid rgba(255,253,250,.28)">
      <div style="font-size:11px;letter-spacing:.12em;text-transform:uppercase;font-weight:700;opacity:.85">Vroege luisteraar</div>
      <div class="dsp" style="font-size:22px;font-weight:700;margin-top:4px;line-height:1.2">23 dagen eerder dan de top 20</div>
      <div style="font-size:13px;opacity:.85;margin-top:3px">Het Vijfde Kwartier</div>
    </div>
  </div>
  <div class="row" style="gap:10px;margin-top:24px"><div class="btn btn-p">${K.share}Delen</div><div class="btn" style="background:#28292E;color:#EDEEF0">Opslaan</div></div>
</div>` };

/* ---- 7. OPML ---- */
screens.Opml = { title: '7 · Na het importeren', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('Importeren')}
  <div class="body pad">
    <div class="opml-hero">
      <div class="bigcheck">${K.check}</div>
      <div><div class="dsp" style="font-size:22px;font-weight:700;letter-spacing:-.02em">34 shows gevolgd</div><div class="t2">uit pocketcasts-export.opml · 36 feeds</div></div>
    </div>
    <div class="list" style="margin-top:14px">
      <div class="sumrow"><span class="dot" style="background:var(--up)"></span>Gekoppeld aan de catalogus<span class="n">31</span></div>
      <div class="sumrow"><span class="dot" style="background:var(--ink2)"></span>Alleen de feed, geen hitlijstplek<span class="n">3</span></div>
      <div class="sumrow"><span class="dot" style="background:var(--down)"></span>Feed niet bereikbaar<span class="n">2</span></div>
    </div>
    <div class="lbl2" style="color:var(--ink2)">Niet bereikbaar</div>
    <div class="erow2" style="padding-top:0"><div class="dstate" style="color:var(--down)">${ICON.warn(20)}</div><div class="ebody"><div class="t1">Radio Oost Nazit</div><div class="t2">radiooost.example/nazit.xml · 404</div></div><div class="btn btn-o" style="height:40px;padding:0 14px;font-size:13px">Opnieuw</div></div>
    <div class="erow2"><div class="dstate" style="color:var(--down)">${ICON.warn(20)}</div><div class="ebody"><div class="t1">Lichtkogel Live</div><div class="t2">lichtkogel.example/rss · time-out</div></div><div class="btn btn-o" style="height:40px;padding:0 14px;font-size:13px">Opnieuw</div></div>
    <div class="lbl2" style="color:var(--ink2)">Alleen de feed</div>
    <div class="erow2" style="padding-top:0"><div class="art a4" style="width:44px;height:44px"></div><div class="ebody"><div class="t1">Ondergronds</div><div class="t2">Niet in Apple's catalogus, speelt wel</div></div></div>
    <div class="row" style="gap:10px;margin-top:16px"><div class="btn btn-p" style="flex:1">Naar de Bibliotheek</div></div>
  </div>
  <div class="gest"><i></i></div>
</div>` };

screens.OpmlMenu = { title: '7 · Bibliotheek → menu', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'more'])}
  <div class="body">
    <div class="pad" style="padding-top:2px;padding-bottom:14px"><h1 class="h1 dsp">Bibliotheek</h1></div>
    ${tabs(['Gevolgd', 'Wachtrij', 'Bewaard', 'Gedownload'], 'Gevolgd')}
  </div>
  ${sheet(`
    <div class="shead"><h2>Je shows</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="srow"><div class="ic">${ICON.down(20)}</div><div class="tx"><b>Importeer uit een andere app</b><span>OPML uit Pocket Casts, AntennaPod, Overcast, Podcast Addict</span></div><span class="chev">${K.chevR()}</span></div>
      <div class="srow"><div class="ic">${ICON.link()}</div><div class="tx"><b>Voeg een feed toe</b><span>Plak het adres van een RSS-feed</span></div><span class="chev">${K.chevR()}</span></div>
      <div class="srow" style="border-bottom:0"><div class="ic">${ICON.file(20)}</div><div class="tx"><b>Exporteer als OPML</b><span>34 shows · ook je back-up</span></div><span class="chev">${K.chevR()}</span></div>
      <div class="note" style="color:var(--ink2);font-size:12.5px;padding:6px 0 22px">2 shows staan alleen op Spotify en hebben geen open feed. Die kunnen niet mee in de export.</div>
    </div>`, 470)}
</div>` };

/* ---- 10. Hitlijstradio ---- */
const eps = [
  ['a5', 'Lange Adem', 'De marathon die niemand liep', 1],
  ['a10', 'Kade 12', 'Aflevering 4: de getuige', 2],
  ['a2', 'Nachtdienst', 'De nachtbus naar huis', 3],
  ['a1', 'De Deadline', 'Live vanuit Paradiso', 4]
];
screens.Radio = { title: '10 · Hitlijsten → Afleveringen → Speel de lijst', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body">
    <div class="pad row" style="justify-content:space-between;padding-bottom:12px"><h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1><span class="note" style="color:var(--ink2)">Bijgewerkt 06:00</span></div>
    <div class="pad row" style="gap:8px;padding-bottom:8px">
      <div class="schip dark">Apple Podcasts ${K.chevS}</div><div class="schip">${FLAG}NL ${K.chevS}</div><div class="schip">True crime ${K.chevS}</div>
    </div>
    ${tabs(['Podcasts', 'Afleveringen', 'Trending', 'Nieuw'], 'Afleveringen')}
    <div class="pad" style="padding-top:12px">
      <div class="radio-bar">
        <span style="color:#F0A87F">${ICON.radio(24)}</span>
        <div><div style="font-size:14px;font-weight:700">Speel de lijst</div><div class="t2" style="font-size:11.5px">Top 10 · 1 per show · wat je hoorde valt weg</div></div>
        <div class="go">${K.play}Speel</div>
      </div>
    </div>
    <div class="pad list">${eps.map(e => `
      <div class="lrow"><div class="rank top">${e[3]}</div><div class="art ${e[0]}"></div><div class="meta"><div class="t1">${e[2]}</div><div class="t2">${e[1]}</div></div><div class="pbtn" style="margin:0">${K.play}</div></div>`).join('')}
    </div>
  </div>
  ${chrome('charts')}
</div>` };

screens.RadioSheet = { title: '10 · Wat gebeurt er met je wachtrij', body: `
<div class="ph col">
  <div class="sa"></div>
  ${appbar(['search', 'bell'])}
  <div class="body"><div class="pad"><h1 class="h1 dsp" style="font-size:26px">Hitlijsten</h1></div></div>
  ${sheet(`
    <div class="shead"><h2>Speel Top afleveringen</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="note" style="color:var(--ink2);font-size:12.5px;margin-top:-4px">Apple ${FLAG} NL · True crime · 8 afleveringen, 5 u 40 min</div>
      <div class="col" style="gap:8px;margin-top:14px">
        <div class="opt on"><span class="rb"></span><div><b>Na je wachtrij</b><span class="s">Je 3 afleveringen blijven eerst</span></div></div>
        <div class="opt"><span class="rb"></span><div><b>Wachtrij vervangen</b><span class="s">Je 3 afleveringen gaan naar Bewaard</span></div></div>
      </div>
      <div class="lbl2" style="color:var(--ink2)">Komt erbij</div>
      <div class="qmini"><span class="n">1</span><div class="art a5"></div><span>De marathon die niemand liep</span></div>
      <div class="qmini"><span class="n">2</span><div class="art a10"></div><span>Aflevering 4: de getuige</span></div>
      <div class="qmini" style="color:var(--ink2)"><span class="n">3</span><div class="art a2" style="opacity:.5"></div><span style="text-decoration:line-through">De nachtbus naar huis</span><span class="skip">al gehoord</span></div>
      <div class="qmini"><span class="n">4</span><div class="art a7"></div><span>Het mes in de polder</span><span class="skip">1 per show</span></div>
      <div class="btn btn-p" style="margin:16px 0 20px">${K.play}Speel 8 afleveringen</div>
    </div>`, 262)}
</div>` };

/* ---- 11. Android Auto ---- */
screens.Auto = { title: '11 · Android Auto: tabblad Hitlijst', wide: true, body: `
<div class="car">
  <div class="rail">
    <div class="ri">${ICON.more}</div>
    <div class="ri on">${ICON.radio(26)}</div>
    <div class="ri">${ICON.mic(26)}</div>
  </div>
  <div class="main">
    <div class="ctabs"><div class="ct">Verder luisteren</div><div class="ct">Nieuw</div><div class="ct on">Hitlijst</div><div class="ct">Gedownload</div></div>
    <div style="font-size:14px;color:#A0A3AA;margin-top:10px">Top afleveringen · Apple NL · True crime</div>
    <div class="items" style="margin-top:10px">
      <div class="it"><span class="rk">1</span><div class="art a5"></div><div class="meta"><div class="t1">De marathon die niemand liep</div><div class="t2">Lange Adem</div></div></div>
      <div class="it"><span class="rk">2</span><div class="art a10"></div><div class="meta"><div class="t1">Aflevering 4: de getuige</div><div class="t2">Kade 12</div></div></div>
      <div class="it"><span class="rk">3</span><div class="art a2"></div><div class="meta"><div class="t1">De nachtbus naar huis</div><div class="t2">Nachtdienst</div></div></div>
      <div class="it"><span class="rk">4</span><div class="art a7"></div><div class="meta"><div class="t1">Het mes in de polder</div><div class="t2">Koud Spoor</div></div></div>
      <div class="it"><span class="rk">5</span><div class="art a9"></div><div class="meta"><div class="t1">Wie belde om drie uur?</div><div class="t2">Dagblad Noord Onderzoekt</div></div></div>
      <div class="it"><span class="rk">6</span><div class="art a8"></div><div class="meta"><div class="t1">Het laatste fluitsignaal</div><div class="t2">Derby</div></div></div>
    </div>
    <div class="np">
      <div class="art a1"></div>
      <div class="meta"><div class="t1" style="font-size:17px;color:#EDEEF0">Live vanuit Paradiso</div><div class="t2" style="font-size:14px;color:#A0A3AA">De Deadline · nog 18 min</div></div>
      <div class="cb">${R15}</div><div class="cb p">${PAUSE(30)}</div><div class="cb">${F30}</div><div class="cb sp">1,2×</div>
    </div>
  </div>
  <div class="prog"><b></b></div>
</div>` };

/* ---- 12. Instellingen per show ---- */
screens.PerShow = { title: '12 · Podcastpagina → Instellingen voor deze show', body: `
<div class="ph col">
  <div class="sa"></div>
  ${titlebar('', ['share', 'more'])}
  <div class="body pad">
    <div class="row" style="gap:14px"><div class="art a1" style="width:88px;height:88px;border-radius:16px"></div>
      <div><div class="eyebrow" style="color:var(--ink2)">Podcast</div><div class="dsp" style="font-size:24px;font-weight:700;letter-spacing:-.02em">De Deadline</div><div class="t2">Dagblad Noord</div></div></div>
    <div class="row" style="gap:10px;margin-top:14px"><div class="btn btn-o">${K.check}Volgt</div><span class="ownset">${ICON.gear(15)}1,3× · intro 45 s</span></div>
  </div>
  ${sheet(`
    <div class="shead"><h2>Voor De Deadline</h2><div class="ibtn">${ICON.close}</div></div>
    <div class="pad">
      <div class="srow"><div class="ic">${ICON.speed(20)}</div><div class="tx"><b>Snelheid</b><span>Algemeen staat op 1,2×</span></div><div class="valchip">1,3×</div></div>
      <div class="srow"><div class="ic">${ICON.skipIn()}</div><div class="tx"><b>Intro overslaan</b><span>Alleen als je vanaf het begin start</span></div><div class="stepper"><span>−</span><b>45 s</b><span>+</span></div></div>
      <div class="srow"><div class="ic">${ICON.skipOut()}</div><div class="tx"><b>Outro overslaan</b><span>Telt als beluisterd, door naar de wachtrij</span></div><div class="valchip def">Uit</div></div>
      <div class="srow"><div class="ic">${ICON.scissors()}</div><div class="tx"><b>Stiltes inkorten</b><span>Zoals algemeen (aan)</span></div><div class="valchip def">Algemeen</div></div>
      <div class="srow" style="border-bottom:0"><div class="ic">${ICON.down(20)}</div><div class="tx"><b>Downloaden</b><span>Automatisch, nieuwste 1 · wifi</span></div><div class="valchip def">Algemeen</div></div>
      <div class="note" style="color:var(--ink2);font-size:12.5px;padding:2px 0 10px">Reclame in de feed maakt de intro soms langer of korter.</div>
      <div class="btn btn-o" style="margin:0 0 20px">Alles terug naar algemeen</div>
    </div>`, 208)}
</div>` };

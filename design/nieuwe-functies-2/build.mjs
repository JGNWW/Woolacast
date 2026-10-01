// Bouwt functies.html: de tweede reeks van tien goedgekeurde functies met hun schermen,
// het oordeel van de criticus en wat onderweg afviel. Dit bestand wordt als Artifact
// gepubliceerd. De werkbladstijl is die van ../kanalen en ../nieuwe-functies.
import fs from 'node:fs';
import path from 'node:path';
import { screens, CSS } from './screens.mjs';
import { VERDICTS, REJECTED } from './verdicts.mjs';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
// base.css zet zijn tokens op :root; op deze pagina horen ze alleen bij de telefoons.
const phoneCss = (base + CSS)
  .replace(':root{', '.ph{')
  .replace('body{margin:0}', '')
  .replace(/a\{color:var\(--pri\);text-decoration:none\}\s*a:hover\{color:var\(--priOn\)\}/, '');

const phone = (key, caption) => {
  const s = screens[key];
  return `
      <figure class="shot">
        <div class="fit" style="aspect-ratio:390/844" data-w="390"><div class="scale" style="width:390px;height:844px">${s.body}</div></div>
        <figcaption>${caption}</figcaption>
      </figure>`;
};

const ROUND = { 1: 'ronde 1', 2: 'ronde 2, ter vervanging van een afgekeurd idee', 3: 'ronde 3, ter vervanging van een afgekeurd idee' };
const verdict = (id, n) => {
  const v = VERDICTS[id];
  return `
        <div class="verdict">
          <p class="vhead"><span class="ok">Goedgekeurd${v.scope ? `, ${v.scope}` : ''}</span><span class="muted">idee ${n} · ${ROUND[v.round]}</span></p>
          <p class="vwhy">${v.why}</p>
          <details>
            <summary>Voorwaarden van de criticus (${v.cond.length})</summary>
            <ul>${v.cond.map(c => `<li>${c}</li>`).join('')}</ul>
          </details>
        </div>`;
};

const feature = (n, title, lede, shots, points, id) => `
  <section class="concept" id="f${n}">
    <div class="shots">${shots}</div>
    <div class="copy">
      <p class="num">Functie ${n}</p>
      <h2>${title}</h2>
      <p class="lede">${lede}</p>
      <dl>${points.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('')}</dl>
      ${verdict(id, id.slice(1))}
    </div>
  </section>`;

// Sleep: hoe het afloopt, als tijdlijn. Schaal: 0–100% van de breedte = de laatste
// 3 minuten van de timer plus een minuut erna.
const sleepLine = `
  <figure class="tl" aria-label="Tijdlijn van de slaaptimer: de laatste minuut luistert de telefoon naar schudden, de laatste 30 seconden faden uit, daarna stopt de speler. Bij verder luisteren begint hij 30 seconden eerder.">
    <div class="tl-track">
      <span class="tl-seg play" style="left:0;width:60%"></span>
      <span class="tl-seg fade" style="left:60%;width:20%"></span>
      <span class="tl-seg shake" style="left:40%;width:40%"></span>
      <span class="tl-tick" style="left:40%"></span><span class="tl-tick" style="left:60%"></span><span class="tl-tick" style="left:80%"></span>
      <span class="tl-back" style="left:60%"></span>
    </div>
    <div class="tl-axis"><span style="left:0">−2:00</span><span style="left:40%">−1:00</span><span style="left:60%">−0:30</span><span style="left:80%">stop</span></div>
    <div class="legend"><span><i style="background:color-mix(in srgb,var(--p-accent) 22%,transparent)"></i>speelt</span><span><i style="background:linear-gradient(90deg,color-mix(in srgb,var(--p-accent) 22%,transparent),transparent);border:1px solid var(--p-line)"></i>uitfaden</span><span><i style="background:var(--p-accent);opacity:.6"></i>schudden = opnieuw</span><span><i style="background:var(--p-ink);width:3px"></i>hier begint hij weer</span></div>
    <figcaption>De laatste minuut luistert de telefoon naar schudden; de laatste 30 seconden wordt het zachter. Druk je de volgende keer op afspelen, dan begint hij 30 seconden terug, bij het stuk dat je wegdommelend miste.</figcaption>
  </figure>`;

const names = ['Nieuw en meldingen', 'Casten', 'Spoelknoppen', 'Zoeken in een show', 'Geschiedenis', 'Verhaal in delen', 'Rond deze sprong', 'Slaaptimer', 'Bladwijzers', 'Back-up'];

const html = `<title>Toadcast functiereeks 2</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&display=swap">
<style>
/*PAGECSS*/
.verdict{border:1px solid var(--p-line);border-radius:14px;background:var(--p-card);padding:14px 16px;display:grid;gap:8px}
.vhead{display:flex;flex-wrap:wrap;align-items:center;gap:10px;font-size:14px}
.vwhy{font-size:14.5px;color:var(--p-ink2)}
.verdict summary{min-height:44px;display:flex;align-items:center;cursor:pointer;font-weight:600;font-size:14.5px}
.verdict ul{margin:0 0 6px;padding-left:20px;display:grid;gap:6px;font-size:14.5px;color:var(--p-ink2)}
.no{display:inline-flex;align-items:center;height:24px;padding:0 9px;border-radius:7px;background:var(--p-nobg);color:var(--p-no);font-size:12.5px;font-weight:700}
.rej{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px}
.rej article{background:var(--p-card);border:1px solid var(--p-line);border-radius:16px;padding:16px 18px;display:grid;gap:8px;align-content:start}
.rej h3{margin:0}
.rej p{color:var(--p-ink2);font-size:14.5px}
.rej p b{color:var(--p-ink)}
.score{display:flex;flex-wrap:wrap;gap:8px;margin-top:4px}
.score span{display:inline-flex;align-items:center;gap:6px;min-height:32px;padding:0 12px;border-radius:10px;background:var(--p-card);border:1px solid var(--p-line);font-size:13.5px;font-weight:600}
.score i{width:8px;height:8px;border-radius:4px;background:var(--p-ok);display:block}
.score .x i{background:var(--p-no)}
.ft td,.ft th{text-align:center;font-variant-numeric:tabular-nums}
.ft td:first-child,.ft th:first-child{text-align:left}
.ft .y{color:var(--p-ok);font-weight:700}.ft .n{color:var(--p-no)}.ft .q{color:var(--p-ink2)}
.ft .tc{background:var(--p-card)}
.ft th,.ft td{padding-left:7px;padding-right:7px}
.ft td:first-child{white-space:normal;min-width:170px}
.tl{margin:0;display:grid;gap:8px;width:300px;max-width:100%;align-self:center;align-content:start}
.tl-track{position:relative;height:44px;border-radius:10px;background:var(--p-card);border:1px solid var(--p-line);overflow:hidden}
.tl-seg{position:absolute;top:0;bottom:0;display:flex;align-items:center;justify-content:center;font-size:12px;min-width:0}
.tl-seg b{font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;padding:0 4px}
.tl-seg.play{background:color-mix(in srgb,var(--p-accent) 22%,transparent)}
.tl-seg.shake{top:auto;height:12px;bottom:0;background:var(--p-accent);opacity:.55}
.tl-seg.shake b{display:none}
.tl-seg.fade{background:linear-gradient(90deg,color-mix(in srgb,var(--p-accent) 22%,transparent),transparent)}
.tl-seg.stop{color:var(--p-ink2)}
.tl-tick{position:absolute;top:0;bottom:0;width:1px;background:var(--p-line)}
.tl-back{position:absolute;top:0;bottom:0;width:3px;margin-left:-1px;background:var(--p-ink)}
.tl-axis{position:relative;height:16px;font-size:11.5px;color:var(--p-ink2);font-variant-numeric:tabular-nums}
.tl-axis span{position:absolute;transform:translateX(-50%)}
.tl-axis span:first-child{transform:none}
.tl figcaption{font-size:13.5px;color:var(--p-ink2)}
.legend{display:flex;flex-wrap:wrap;gap:12px;font-size:12.5px;color:var(--p-ink2)}
.legend i{display:inline-block;width:12px;height:12px;border-radius:3px;vertical-align:-2px;margin-right:5px}
@media (max-width:1180px){.concept{grid-template-columns:minmax(0,1fr);gap:24px}.shots{flex-wrap:wrap;min-width:0}.shot{width:min(300px,100%)}}
${phoneCss}
.ph{border-radius:0}
</style>

<div class="page">
  <header class="intro">
    <p class="eyebrow-p">Toadcast · ontwerpvoorstel · tweede reeks</p>
    <h1>Tien nieuwe functies</h1>
    <p>Gevraagd: tien nieuwe ideeën, elk beoordeeld door een criticus die de tien meest gebruikte podcastapps en hun meestgebruikte functies heeft onderzocht. Een idee kwam pas op de lijst na goedkeuring; een afgekeurd idee werd vervangen door een nieuw idee, dat opnieuw beoordeeld werd. Dertien ideeën in drie rondes: <strong>tien goedgekeurd</strong>, drie afgekeurd.</p>
    <p>De ideeën uit de eerste reeks (downloaden, hoofdstukken, meelezen, stilte inkorten, weekoverzicht, terugblik, OPML, hitlijstradio, de auto en instellingen per show) staan hier niet opnieuw in. Deze reeks vult de rest van de tafelstakes aan (meldingen, casten, spoelknoppen, zoeken in een show, geschiedenis), repareert de kernroute van een hitlijstenapp bij verhalende shows, en geeft de chart-tracker context.</p>
    <div class="score" aria-label="Uitslag van de beoordeling">
      <span><i></i>Ronde 1: 8 van 10 goedgekeurd</span><span class="x"><i></i>2 afgekeurd</span><span><i></i>Ronde 2: 1 van 2</span><span class="x"><i></i>1 afgekeurd</span><span><i></i>Ronde 3: 1 van 1</span>
    </div>
    <nav class="jump" aria-label="Functies">
      ${names.map((t, i) => `<a href="#f${i + 1}"><b>${i + 1}</b> ${t}</a>`).join('')}
    </nav>
  </header>

  <section class="block">
    <h2>De meetlat</h2>
    <p class="sub">De criticus gebruikte zijn onderzoek naar YouTube, Spotify, Apple Podcasts, Amazon Music, iHeartRadio, Castbox, Overcast, Pocket Casts, Podcast Addict en Podbean (met AntennaPod als referentie), en zocht voor deze reeks per functie na welke app het heeft: in helppagina's, changelogs en issues. Per idee vier vragen: gebruiken of missen luisteraars het echt, kan het zonder eigen server en account, dupliceert het iets wat Toadcast al heeft, en is het de moeite waard?</p>
    <div class="tablewrap"><table class="ft">
      <thead><tr><th>Functie</th><th>YT</th><th>Spot</th><th>Apple</th><th>Amzn</th><th>iHrt</th><th>Cbox</th><th>Ovc</th><th>PC</th><th>PA</th><th>Pbn</th><th>AP</th><th class="tc">Toadcast</th></tr></thead>
      <tbody>
${[
  ['Melding bij een nieuwe aflevering', '~✓✓??✓✓✓✓✓✓✗'],
  ['Eén lijst met nieuwe afleveringen', '✓✓???✓✓✓✓?✓~'],
  ['Casten naar een speaker', '✓✓✓✓✓✓✓✓✓✓~✗'],
  ['Spoelstappen zelf instellen', '✓✗✓??✓✓✓✓✓✓✗'],
  ['Zoeken binnen één show', '~✓✓??✓✓✓~?✓✗'],
  ['Luistergeschiedenis', '✓~~??✓~✓✓✓✓✗'],
  ['Seriële volgorde (itunes:type)', '?✓✓????~~?~✗'],
  ['Slaaptimer: zacht uitfaden', '??????✓✓✓?✓✗'],
  ['Bladwijzers', '✓~~✗✗?✓~✓?✗✗'],
  ['Back-up als bestand, zonder account', '✗✗✗✗✗?~✗✓?✓~'],
  ['Uitleg bij een sprong in de hitlijst', '✗✗✗✗✗✗✗✗✗✗✗✗']
].map(([f, c]) => `        <tr><td>${f}</td>${[...c].map((x, i) => `<td class="${x === '✓' ? 'y' : x === '✗' ? 'n' : 'q'}${i === 11 ? ' tc' : ''}">${x}</td>`).join('')}</tr>`).join('\n')}
      </tbody>
    </table></div>
    <p class="legend"><span><b class="y" style="color:var(--p-ok)">✓</b> aanwezig</span><span>~ deels of betaald</span><span><b style="color:var(--p-no)">✗</b> niet aanwezig</span><span>? niet vastgesteld</span></p>
  </section>

  <div class="block">
    ${feature(1, 'Nieuw, en een melding bij een nieuwe aflevering', 'Alle nieuwe afleveringen van je gevolgde shows in één lijst bovenaan de Bibliotheek, en per show een melding als je die wilt.',
      phone('Inbox', 'Nieuw staat bovenaan Gevolgd, geen eigen tabblad') + phone('InboxNotif', 'Eén gebundelde melding per ronde'),
      [['Wat het doet', 'Nieuwste eerst, per dag gegroepeerd, met drie vaste chips: Onbeluisterd, Kort &lt; 30 min, Deze week. "Alles in de wachtrij" zet er hooguit 20 achter je wachtrij. Naar links vegen verbergt een aflevering.'],
       ['Meldingen', 'Per show aan te zetten (standaard uit). Eén melding per ronde met Afspelen en In de wachtrij. Shows die alleen op Spotify staan hebben geen feed en dus geen schakelaar.'],
       ['Hoe', 'De bestaande downloadtaak leest al elke 6 uur de feeds; die ronde doet dit erbij, met voorwaardelijk ophalen zodat een ongewijzigde feed niets kost.'],
       ['Eén getal', '"Nieuw" betekent overal hetzelfde: verschenen na het volgen, niet beluisterd en niet weggeveegd. De "2 nieuw" bij een show volgt dezelfde regel.']], 'i1')}

    ${feature(2, 'Casten naar Chromecast en speakers', 'Een cast-knop in de speler. De speaker haalt de audio zelf bij de maker; de telefoon wordt de afstandsbediening.',
      phone('Cast', 'Speler → Afspelen op'),
      [['Wat het doet', 'Kies een speaker of tv. Snelheid, spoelen, slaaptimer, hoofdstukken, wachtrij en voortgang blijven werken. Stop je met casten, dan speelt de telefoon verder waar de speaker was.'],
       ['Eerlijk over grenzen', 'Stiltes inkorten, stemversterking en intro overslaan gebeuren op de telefoon en werken op de speaker niet. Het blad zegt dat.'],
       ['Hoe', 'Media3 Cast. Een gedownloade aflevering wordt gecast vanaf het adres in de feed. De knop verschijnt alleen als er een Cast-apparaat in het netwerk is.'],
       ['Twee builds', 'De Cast-SDK vraagt Google Play-diensten. Zoals bij AntennaPod komt er een build zonder casten voor wie zonder Google werkt.']], 'i3')}

    ${feature(3, 'Spoelknoppen instellen, en de koptelefoon', 'Hoeveel terug en hoeveel vooruit kies je zelf. En dubbel tikken op de koptelefoon spoelt, in plaats van niets te doen.',
      phone('Seek', 'Instellingen → Afspelen'),
      [['Wat het doet', 'Terug 5, 10, 15 of 30 seconden; vooruit 10 tot 60. Het gekozen getal staat op de knoppen in de speler, de melding, het vergrendelscherm en in de auto.'],
       ['Koptelefoon', 'Standaard spoelt dubbel tikken vooruit en driedubbel terug, met dezelfde stappen. Als optie: volgende in de wachtrij. In de auto blijven de knoppen spoelen.'],
       ['Waarom nu', 'Nu staat het vast op −15 en +30, en doet "volgende" op een koptelefoon waarschijnlijk niets: de speler kent maar één aflevering, de wachtrij zit ernaast.']], 'i11')}

    ${feature(4, 'Zoeken in de afleveringen van één show', 'Een zoekveld op de podcastpagina. Typen filtert alle afleveringen van deze show op titel en shownotes.',
      phone('ShowSearch', 'Podcastpagina → zoeken in deze show'),
      [['Wat het doet', 'Vind "die aflevering met Sanne Kuipers" in een show met honderden afleveringen. Het woord wordt gemarkeerd; een treffer in de shownotes krijgt de zin eromheen.'],
       ['Waarin gezocht', 'Altijd erbij: "12 van de 300 afleveringen in de feed". Toont de feed maar een deel van het archief, en is daar een teken van, dan zegt de pagina dat.'],
       ['Niets gevonden', 'Een knop zoekt dezelfde term in heel Toadcast.'],
       ['Hoe', 'De feed is al geladen. De tekst wordt één keer klaargemaakt (platte tekst, kleine letters, zonder accenten), zodat "cafe" ook "café" vindt. Geen netwerk.']], 'i13')}

    ${feature(5, 'Luistergeschiedenis', 'Wat je hoorde, per dag, met hoe ver je kwam. Tik en je luistert verder waar je was.',
      phone('History', 'Bibliotheek → klokje → Geschiedenis'),
      [['Wat het doet', 'Per dag: titel, show, hoeveel je echt luisterde en tot waar. Zoeken, per item verwijderen, alles wissen, of het bijhouden uitzetten.'],
       ['Eén log', 'Dezelfde gegevens voeden de Terugblik uit de eerste reeks. Wie de geschiedenis uitzet of wist, doet dat ook voor de Terugblik, en dat staat bij de schakelaar.'],
       ['Hoe', 'Echt geluisterde seconden (spoelen telt niet), per dag per aflevering samengevoegd zodat het opslagbestand niet blijft groeien. Alles op het toestel.']], 'i4')}

    ${feature(6, 'Verhaal in delen: begin bij aflevering 1', 'Shows die de maker als serie markeert, staan oudste eerst, met een grote knop om bij het begin te beginnen of verder te gaan waar je was.',
      phone('Serial', 'Podcastpagina van een seriële show'),
      [['Wat het doet', 'Bovenaan "Begin bij deel 1" of "Verder bij deel 3". Seizoenen als chips, trailers en bonussen apart. In de wachtrij komt na een deel het volgende deel, niet de nieuwste aflevering.'],
       ['Vanuit de hitlijst', 'Afspelen van een seriële show uit een lijst begint bij deel 1 (of waar je was) in plaats van midden in het verhaal. De hitlijstradio blijft per aflevering.'],
       ['Hoe', 'De feed leest itunes:type, itunes:season, itunes:episode en itunes:episodeType. Ontbreekt het begin in de feed, dan staat er "oudste beschikbare". Per show om te zetten voor makers die de tag verkeerd zetten.']], 'i9')}

    ${feature(7, 'Rond deze sprong', 'Bij een flinke stijger zet de app erbij wat er in de dagen ervoor gebeurde: een tip in de krant, een plek in Apple\'s selectie, een nieuw seizoen. Feiten naast elkaar, zonder te zeggen dat het de oorzaak is.',
      phone('JumpTracker', 'Chart-tracker: de gebeurtenissen op de grafiek') + phone('JumpList', 'In de lijst: één regel, alleen als de meting dat rechtvaardigt'),
      [['Wat telt', 'Alleen wat hooguit 7 dagen vóór de sprong viel. Een aflevering telt alleen als het de eerste is, een nieuw seizoen, of een terugkeer na een pauze; een gewone weekaflevering zegt niets.'],
       ['Bronnen', 'De tips van de media, Apple\'s Nieuwe programma\'s, de feed van de show, en "eerst hoog in een ander land" uit de historie van de verzamelaar. Allemaal data die Toadcast al heeft.'],
       ['Eerst meten', 'Een maand stijgers in NL en de VS nalopen. Heeft minder dan een kwart een aanleiding, dan verschijnt het alleen in de chart-tracker en niet in de lijst.'],
       ['Waarom Toadcast', 'Geen van de tien grote apps toont beweging door de tijd, laat staan wat erbij hoort.']], 'i5')}

    ${feature(8, 'Een slaaptimer die meedenkt', 'De slaaptimer bestaat al. Dit maakt het stoppen zacht en het verder luisteren makkelijk.',
      phone('Sleep', 'Instellingen → Slaaptimer') + sleepLine,
      [['Uitfaden', 'De laatste 30 seconden steeds zachter in plaats van een harde stop. Standaard aan. Daarna staat het volume weer op 100%, zodat de volgende keer niet stil begint.'],
       ['Schudden', 'Wie nog wakker is, schudt de telefoon in de laatste minuut: de timer begint opnieuw, een korte tril bevestigt het. Zonder bewegingssensor werkt de timer gewoon.'],
       ['Terug bij verder luisteren', 'Na de slaaptimer begint de speler 30 seconden eerder, het stuk dat je half sliep. Niet na "einde aflevering", want daar mis je niets.'],
       ['Vanzelf aan', 'Optioneel: als je tussen 22:00 en 06:00 zelf op afspelen drukt, staat de timer meteen aan. Nooit in de auto. Het slaaptimermenu in de speler blijft kort.']], 'i8')}

    ${feature(9, 'Bladwijzers', 'Eén tik in de speler legt het moment vast. Een notitie kan later.',
      phone('BookmarkSet', 'Eén tik, zonder dialoog') + phone('BookmarkList', 'Onder de hoofdstukken van de aflevering'),
      [['Wat het doet', 'Een bladwijzer is een tijdstip met een optionele notitie. Ze staan als streepjes op de balk, onder de hoofdstukken van de aflevering, en als groep bovenaan Bewaard. Tik = afspelen vanaf dat punt.'],
       ['Delen', 'De bestaande deeltekst plus "vanaf 12:05". Geen link die naar het tijdstip springt: Toadcast heeft geen eigen webpagina, en de site van de maker kent geen tijdstip.'],
       ['Geen clips', 'Stukjes audio knippen en delen is afgekeurd: Pocket Casts haalde het in 2025 weg omdat het onderhoud niet vol te houden was.'],
       ['Blijvend', 'Bij elke bladwijzer bewaart de app de gegevens van de aflevering, zodat hij blijft werken als de aflevering uit de feed valt. Ze gaan mee in de back-up.']], 'i2')}

    ${feature(10, 'Back-up en verhuizen', 'Alles wat van jou is in één bestand: om te verhuizen naar een nieuwe telefoon, of als je geen Google-back-up gebruikt.',
      phone('Backup', 'Terugzetten, eerst met een samenvatting'),
      [['Wat erin zit', 'Gevolgde shows en makers, wachtrij, bewaard, voortgang, beluisterd, bladwijzers, geschiedenis en instellingen, ook per show. Geen hitlijsten, tips of downloads: die haalt de app opnieuw op.'],
       ['Hoe', 'Exporteren en terugzetten met de hand, via het bestandsmenu van Android, naast de OPML-import. Het bestand kan naar de telefoon, Drive, Nextcloud of waar je wilt. Geen account, geen server.'],
       ['Terugzetten', 'Eerst een samenvatting van wat er in het bestand zit. Terugzetten vervangt alles; er wordt niets samengevoegd. Het bestand heeft een versienummer.'],
       ['Naast Google', 'De automatische Google-back-up van Android staat al aan en blijft de standaard. Instellingen noemt hem. Dit bestand is de uitweg voor wie zonder Google werkt of zelf wil verhuizen.']], 'i7')}
  </div>

  <section class="block">
    <h2>Afgekeurd onderweg</h2>
    <p class="sub">Drie ideeën haalden het niet. De deelkaart en proefluisteren (ronde 1) werden vervangen door de spoelknoppen en de vergelijking van Apple en Spotify (ronde 2); die vergelijking werd vervangen door zoeken in een show (ronde 3).</p>
    <div class="rej">
      ${REJECTED.map(r => `<article><p><span class="no">Afgekeurd</span> <span class="muted" style="font-size:13px">idee ${r.n} · ronde ${r.round}</span></p><h3>${r.title}</h3><p>${r.why}</p><p><b>Opnieuw indienen kan als:</b> ${r.again}</p></article>`).join('')}
    </div>
  </section>

  <section class="block">
    <h2>Volgorde van bouwen</h2>
    <p class="sub">Voorgesteld door de criticus. Twee regels gelden voor alles: geen nieuwe tabbladen in de Bibliotheek (er staan er al vijf), en wat aan de speler zit wacht tot de speler op een echt toestel getest is.</p>
    <ol class="plan">
      <li><div><h3>Nieuw en meldingen</h3><p>Tafelstakes, en de feedronde bestaat al.</p></div></li>
      <li><div><h3>Casten, dan de spoelknoppen</h3><p>Na de test op een toestel en het bijwerken van Media3. De spoelknoppen herstellen meteen de koptelefoon.</p></div></li>
      <li><div><h3>Zoeken in een show, geschiedenis, verhaal in delen</h3><p>Klein en lokaal. De geschiedenis legt ook de data voor de Terugblik neer.</p></div></li>
      <li><div><h3>Rond deze sprong</h3><p>Eerst een maand meten hoe vaak er een aanleiding is.</p></div></li>
      <li><div><h3>Slaaptimer, bladwijzers, back-up</h3><p>Fijn voor zware luisteraars. De back-up als laatste, zodat bladwijzers en geschiedenis meegaan.</p></div></li>
    </ol>
  </section>

  <section class="block">
    <h2>Bronnen</h2>
    <p class="sub">Alle podcasts, makers, media en cijfers in de schermen zijn verzonnen. De volledige beoordelingen staan in <code>beoordelingen.md</code>, het onderzoek in <code>onderzoek-aanvulling.md</code> (met alle bronnen) en in het onderzoek van de eerste reeks.</p>
    <ul class="srcs">
      <li>Meldingen en lijsten: <a href="https://community.spotify.com/t5/FAQs/New-Episodes-are-now-in-the-Following-Feed-FAQ/ta-p/6989287">Spotify Following-feed</a>, <a href="https://support.apple.com/guide/iphone/follow-your-favorite-podcasts-iph92ddcc196/ios">Apple meldingen per show</a>, <a href="https://9to5google.com/2024/04/08/google-podcast-new-episode-notifications/">YouTube Music zonder meldingen</a></li>
      <li>Spoelen: <a href="https://support.apple.com/guide/podcasts/change-settings-pod4130f48/mac">Apple instellingen</a>, <a href="https://support.pocketcasts.com/article/skip-controls/">Pocket Casts Skip Controls</a>, <a href="https://www.podfeet.com/blog/2024/05/overcast-user-guide/">Overcast-gids</a>, <a href="https://github.com/AntennaPod/AntennaPod/issues/2551">AntennaPod #2551</a></li>
      <li>Slaaptimer: <a href="https://support.pocketcasts.com/knowledge-base/sleep-timer/">Pocket Casts</a>, <a href="https://github.com/AntennaPod/AntennaPod/issues/8760">AntennaPod #8760</a>, <a href="https://github.com/AntennaPod/AntennaPod/issues/7052">AntennaPod #7052</a></li>
      <li>Bladwijzers en clips: <a href="https://support.pocketcasts.com/knowledge-base/bookmarks/">Pocket Casts bladwijzers</a>, <a href="https://github.com/Automattic/pocket-casts-android/pull/3481">Pocket Casts haalt clips weg (PR #3481)</a></li>
      <li>Serieel en hitlijsten: <a href="https://providersupport.spotify.com/article/podcast-consumption-order">Spotify consumption order</a>, <a href="https://podcasters.apple.com/support/3146-apple-podcasts-charts">Apple Podcasts Charts</a>, <a href="https://podnews.net/article/how-the-podcast-charts-are-calculated">Podnews over de lijsten</a></li>
      <li>Casten en back-up: <a href="https://developer.android.com/media/media3/cast/create-castplayer">Media3 CastPlayer</a>, <a href="https://developer.android.com/identity/data/autobackup">Android Auto Backup</a>, <a href="https://podcastaddict.com/faq/20">Podcast Addict back-up</a></li>
    </ul>
  </section>
</div>
<script>
(function(){
  function fit(){
    document.querySelectorAll('.fit').forEach(function(f){
      var s=f.querySelector('.scale'); if(s) s.style.transform='scale('+(f.clientWidth/Number(f.dataset.w||390))+')';
    });
  }
  fit(); window.addEventListener('resize',fit);
  if (window.ResizeObserver) new ResizeObserver(fit).observe(document.body);
})();
</script>
`;
fs.writeFileSync(path.join(dir, 'functies.html'), html.replace('/*PAGECSS*/', PAGECSS()));
console.log('functies.html', html.length);

function PAGECSS() {
  // De werkbladstijl van de makers-pagina, zodat alle voorstellen er hetzelfde uitzien.
  const k = fs.readFileSync(path.join(dir, '..', 'kanalen', 'build.mjs'), 'utf8');
  const css = k.slice(k.indexOf('<style>') + 7, k.indexOf('${phoneCss}'));
  return css
    .replace(/--p-no:#8E2C1B;/, '--p-no:#8E2C1B; --p-nobg:#F6DDD6;')
    .replace(/--p-no:#FFA3B4;/g, '--p-no:#FFA3B4; --p-nobg:#3A1E24;');
}

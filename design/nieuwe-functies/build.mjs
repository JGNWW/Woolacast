// Bouwt functies.html: het onderzoek, de tien goedgekeurde functies met hun schermen,
// en het oordeel van de criticus. Dit bestand wordt als Artifact gepubliceerd.
import fs from 'node:fs';
import path from 'node:path';
import { screens, CSS } from './screens.mjs';
import { VERDICTS, REJECTED } from './verdicts.mjs';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
// base.css zet zijn tokens op :root; op deze pagina horen ze alleen bij de telefoons.
const phoneCss = (base + CSS)
  .replace(':root{', '.ph,.car{')
  .replace('body{margin:0}', '')
  .replace(/a\{color:var\(--pri\);text-decoration:none\}\s*a:hover\{color:var\(--priOn\)\}/, '');

const phone = (key, caption) => {
  const s = screens[key];
  const [w, h] = s.wide ? [800, 480] : [390, 844];
  return `
      <figure class="shot${s.wide ? ' wide' : ''}">
        <div class="fit" style="aspect-ratio:${w}/${h}" data-w="${w}"><div class="scale" style="width:${w}px;height:${h}px">${s.body}</div></div>
        <figcaption>${caption}</figcaption>
      </figure>`;
};

const verdict = (id) => {
  const v = VERDICTS[id];
  return `
        <div class="verdict">
          <p class="vhead"><span class="ok">Goedgekeurd</span><span class="muted">ronde ${v.round}${v.round > 1 ? ', ter vervanging van een afgekeurd idee' : ''}</span></p>
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
      ${verdict(id)}
    </div>
  </section>`;

const names = ['Downloaden', 'Hoofdstukken', 'Meelezen', 'Stilte inkorten', 'Weekoverzicht', 'Terugblik', 'OPML', 'Hitlijstradio', 'In de auto', 'Per show'];

const html = `<title>Tien nieuwe functies voor Toadcast</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&display=swap">
<style>
/*PAGECSS*/
.shot.wide{width:440px}
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
.score span{display:inline-flex;align-items:center;gap:6px;height:32px;padding:0 12px;border-radius:10px;background:var(--p-card);border:1px solid var(--p-line);font-size:13.5px;font-weight:600}
.score i{width:8px;height:8px;border-radius:4px;background:var(--p-ok);display:block}
.score .x i{background:var(--p-no)}
@media (max-width:1180px){.concept{grid-template-columns:minmax(0,1fr);gap:24px}.shots{flex-wrap:wrap;min-width:0}.shot,.shot.wide{width:min(300px,100%)}.shot.wide{width:min(440px,100%)}}
${phoneCss}
.ph,.car{border-radius:0}
</style>

<div class="page">
  <header class="intro">
    <p class="eyebrow-p">Toadcast · ontwerpvoorstel</p>
    <h1>Tien nieuwe functies</h1>
    <p>Gevraagd: tien ideeën voor nieuwe functies, elk beoordeeld door een criticus die de tien meest gebruikte podcastapps en hun meestgebruikte functies heeft onderzocht. Een idee kwam pas op deze lijst na zijn goedkeuring. Twaalf ideeën zijn voorgelegd; <strong>tien zijn goedgekeurd</strong>, twee afgekeurd en vervangen.</p>
    <p>De lijn door de lijst: eerst de basis die bij de tien grote apps vanzelf spreekt en in Toadcast ontbreekt (downloaden, hoofdstukken, OPML, de auto), dan wat fans een app laat kiezen (stilte inkorten, instellingen per show), en daarbovenop drie functies die alleen een hitlijstenapp kan maken: het weekoverzicht, de vroege luisteraar in de terugblik, en de hitlijstradio.</p>
    <div class="score" aria-label="Uitslag van de beoordeling">
      <span><i></i>Ronde 1: 8 van 10 goedgekeurd</span><span class="x"><i></i>2 afgekeurd</span><span><i></i>Ronde 2: 2 van 2 goedgekeurd</span>
    </div>
    <nav class="jump" aria-label="Functies">
      ${names.map((t, i) => `<a href="#f${i + 1}"><b>${i + 1}</b> ${t}</a>`).join('')}
    </nav>
  </header>

  <section class="block">
    <h2>De meetlat: de tien grootste podcastapps</h2>
    <p class="sub">Er is geen wereldwijde telling. De criticus combineerde enquêtes (Edison, Cumulus/Signal Hill), downloadcijfers van hostingbedrijven (Buzzsprout, Transistor), Podtrac en installaties in de Play Store. Stand 30 september 2026.</p>
    <div class="tablewrap"><table>
      <thead><tr><th>#</th><th>App</th><th>Aandeel</th><th>Waar fans hem om kiezen</th></tr></thead>
      <tbody>
        <tr><td>1</td><td>YouTube (+ Music)</td><td>28% van de luistertijd (VS)</td><td>Video, aanbevelingen, stilte inkorten in Music</td></tr>
        <tr><td>2</td><td>Spotify</td><td>29% van de luistertijd; 19–29% van de downloads</td><td>Alles in één app, Wrapped, transcripties</td></tr>
        <tr><td>3</td><td>Apple Podcasts</td><td>36–43% van de downloads</td><td>Standaard op iPhone, transcripties, automatische hoofdstukken</td></tr>
        <tr><td>4</td><td>Amazon Music</td><td>4% van de luistertijd</td><td>Alexa, Audible in dezelfde app</td></tr>
        <tr><td>5</td><td>iHeartRadio</td><td>4% van de luistertijd</td><td>Radio en podcasts samen</td></tr>
        <tr><td>6</td><td>Castbox</td><td>1,9% van de downloads</td><td>Zoeken in de audio, gratis</td></tr>
        <tr><td>7</td><td>Overcast</td><td>1,7–2,9%</td><td>Smart Speed, Voice Boost</td></tr>
        <tr><td>8</td><td>Pocket Casts</td><td>1,3–1,7%</td><td>Filters, instellingen per show, Playback</td></tr>
        <tr><td>9</td><td>Podcast Addict</td><td>1,1%</td><td>Alles instelbaar, statistieken</td></tr>
        <tr><td>10</td><td>Podbean</td><td>0,4–0,5%</td><td>Makers en luisteraars in één app</td></tr>
      </tbody>
    </table></div>
    <div class="facts" style="margin-top:14px">
      <div class="fact"><div class="big">10 / 10</div><p>apps kunnen downloaden en werken in de auto. Toadcast nog niet.</p></div>
      <div class="fact"><div class="big">0 / 10</div><p>apps laten zien hoe een show door de hitlijsten beweegt. Toadcast wel.</p></div>
      <div class="fact"><div class="big">~4%</div><p>van de afleveringen levert een transcriptie in de feed, ~0,9% hoofdstukken (Podcast Index).</p></div>
      <div class="fact"><div class="big">92%</div><p>luistert en kijkt niet. Wie van YouTube naar een audio-app gaat, doet dat om te luisteren.</p></div>
    </div>
    <ul class="lessons">
      <li><b>Tafelstakes:</b> downloaden, snelheid, slaaptimer, wachtrij, meldingen, auto, casten, hoofdstukken, en sinds kort transcripties.</li>
      <li><b>Onderscheidend:</b> stilte inkorten en stemversterking, filters, statistieken, OPML en eigen feeds, instellingen per show.</li>
      <li><b>Hype zonder gebruik:</b> live audio (Spotify Live en Amazon Amp gestopt), reacties via RSS, value-for-value.</li>
      <li><b>Het gat:</b> niemand verbindt de hitlijst van vandaag met die van vorige week, of met wat jij luistert.</li>
    </ul>
  </section>

  <div class="block">
    ${feature(1, 'Downloaden en automatisch klaarzetten', 'Afleveringen op het toestel, zodat luisteren ook zonder verbinding kan. Per gevolgde show zet de app de nieuwste klaar, alleen op wifi, binnen een opslaggrens.',
      phone('Downloads', 'Bibliotheek krijgt een tabblad Gedownload') + phone('DownloadsSheet', 'Per show aan te zetten'),
      [['Wat het doet', 'Een downloadknop op elke afleveringsrij en in de speler. De rij toont de status: bezig (ring), klaar (vinkje), mislukt. De speler speelt het lokale bestand als dat er is.'],
       ['Automatisch', 'Per show aan of uit, met het aantal (standaard uit, of 1). Oudere downloads ruimt de app zelf op. Beluisterd wordt na 24 uur gewist, behalve wat bewaard is of in de wachtrij staat.'],
       ['Opslag', 'Een balk die automatisch en zelf gedownload apart telt, en een grens in Instellingen.'],
       ['Techniek', 'Media3 DownloadManager met wifi- en oplaadvoorwaarden. Geen server.']], 'f1')}

    ${feature(2, 'Hoofdstukken', 'Waar de maker hoofdstukken meelevert, ziet de speler ze: het huidige hoofdstuk, streepjes in de balk, en een lijst om naartoe te springen.',
      phone('Chapters', 'Tik op het hoofdstuklabel opent de lijst'),
      [['Bronnen', 'podcast:chapters (JSON) en Podlove Simple Chapters uit de RSS, en anders ID3-hoofdstukken uit het mp3-bestand, via een range-verzoek op het begin.'],
       ['In de speler', 'Het label "3/5 Live: Stadslicht" boven de balk, streepjes op de hoofdstukgrenzen, en onder de tijden "nog 8:57 in dit hoofdstuk".'],
       ['Slaaptimer', 'Een extra keuze: "einde van dit hoofdstuk".'],
       ['Geen hoofdstukken', 'Dan is er geen spoor van: geen label, geen streepjes. Vorige en volgende blijven over de wachtrij gaan.']], 'f2')}

    ${feature(3, 'Transcriptie meelezen', 'Als de maker een transcriptie publiceert, krijgt de speler een tabblad Tekst. De zin die je hoort licht op en schuift mee; tik op een zin en de speler springt ernaartoe.',
      phone('Transcript', 'Meelezen en zoeken in de tekst'),
      [['Wat het doet', 'Meelezen met sprekersnamen, zoeken met "2 van 4" en pijlen, en op de podcastpagina een klein label bij afleveringen met tekst. Ook een toegankelijkheidsfunctie voor wie slecht hoort.'],
       ['Bronnen', 'podcast:transcript in SRT, VTT of JSON. Een HTML-transcriptie zonder tijden wordt platte tekst, zonder meelopen.'],
       ['Grens', 'De app transcribeert niet zelf; dat vraagt een server of veel rekenkracht.'],
       ['Eerst meten', 'Omdat maar ~4% van de afleveringen tekst heeft, telt de verzamelaar eerst hoeveel het er in de top 200 zijn. Daarvan hangt af wanneer dit gebouwd wordt.']], 'f3')}

    ${feature(4, 'Stilte inkorten en stemversterking', 'Twee schakelaars in het snelheidsmenu: stiltes overslaan zonder dat mensen sneller gaan praten, en zachte stemmen harder maken ten opzichte van muziek.',
      phone('Speed', 'Het snelheidsmenu, met de gemeten besparing'),
      [['Inkorten', 'Media3 slaat stille stukken over. De app telt precies hoeveel tijd dat scheelt en toont het totaal: "3 u 12 min bespaard".'],
       ['Stem', 'Dynamische compressie van Android, voorzichtig afgesteld zodat muziek niet vervormt.'],
       ['Per show', 'Onder het menu staat een link naar de instellingen voor deze show (functie 10), voor shows met veel muziek.']], 'f4')}

    ${feature(5, 'Weekoverzicht van de hitlijsten', 'Elke maandag één overzicht van wat er in jouw lijsten gebeurde: hoe jouw shows bewogen, wie het hardst steeg en wat er nieuw in de top 10 kwam. Als kaart op Ontdek en als één melding.',
      phone('Week', 'De kaart bovenaan Ontdek') + phone('WeekNotif', 'Eén melding op maandagochtend'),
      [['Welke lijsten', 'Hooguit drie, afgeleid uit de categorieën van je gevolgde shows en de lijst die je het laatst bekeek. Zelf aan te passen.'],
       ['Data', 'De dagelijkse historie van de verzamelaar, zodat er geen gaten vallen in weken dat je een lijst niet opende.'],
       ['Melding', 'Een eigen meldingskanaal, los uit te zetten. De dagkaart Chart-alerts in de Bibliotheek blijft.'],
       ['Waarom Toadcast', 'Geen van de tien grote apps laat beweging door de tijd zien.']], 'f5')}

    ${feature(6, 'Terugblik', 'Je eigen luisterstatistieken, per maand, per jaar of alles, met één cijfer dat alleen Toadcast kan geven: hoeveel dagen jij een show eerder ontdekte dan de top 20.',
      phone('Recap', 'Bibliotheek → Terugblik') + phone('RecapCard', 'Deelbare kaart, op het toestel gemaakt'),
      [['Wat erin staat', 'Uren geluisterd, de langste reeks dagen, uren per week, de meest geluisterde shows en categorieën.'],
       ['Vroege luisteraar', 'Shows die je volgde voordat ze de top 20 haalden, met het aantal dagen voorsprong. De top-20-datum komt uit de historie van de verzamelaar.'],
       ['Nu al beginnen', 'De app houdt vanaf nu geluisterde tijd per dag per show bij, en de datum waarop je een show ging volgen. Het scherm kan later komen.'],
       ['Privé', 'Alles op het toestel. De deelbare afbeelding wordt lokaal gemaakt en alleen gedeeld als jij dat doet.']], 'f6')}

    ${feature(7, 'OPML importeren en exporteren', 'Je shows meenemen uit een andere app, en ze weer meenemen naar buiten. Voor een app zonder account is dit ook de back-up.',
      phone('OpmlMenu', 'Bibliotheek → menu') + phone('Opml', 'Het overzicht na het importeren'),
      [['Importeren', 'Een OPML-bestand uit Pocket Casts, AntennaPod, Overcast of Podcast Addict, via de bestandskiezer of "delen met Toadcast".'],
       ['Na afloop', 'Hoeveel shows gevolgd zijn, welke aan de catalogus gekoppeld zijn (en dus een hitlijstplek krijgen), welke alleen een feed hebben, en welke feeds niet bereikbaar waren, met Opnieuw.'],
       ['Eigen feed', 'Volgen op alleen een feed-adres wordt mogelijk. Daarmee komt "voeg een feed toe" meteen mee.'],
       ['Exporteren', 'Alle shows met een open feed. Wat alleen op Spotify staat, meldt de app apart.']], 'f7')}

    ${feature(8, 'Hitlijstradio', 'Eén knop op Hitlijsten → Afleveringen: speel de lijst. De top van de gekozen bron, het land en de categorie achter elkaar, zonder wat je al hoorde.',
      phone('Radio', 'De knop boven de afleveringslijst') + phone('RadioSheet', 'Voor het afspelen: wat gebeurt er met je wachtrij'),
      [['Regels', 'De top 10, één aflevering per show, en wat je al hoorde valt weg.'],
       ['Je wachtrij', 'Je kiest: erachter, of vervangen (je afleveringen gaan naar Bewaard). Het blad laat zien wat er komt en wat er wegvalt.'],
       ['Spotify', 'Spotify levert geen afleveringslijst per categorie. Daar pakt de knop per show de nieuwste aflevering, en dat staat erbij.'],
       ['Geen proefmodus', 'De eerste minuten van een aflevering zijn vaak reclame. Later kan proeven via hoofdstukken of soundbites.']], 'f10')}

    ${feature(9, 'Toadcast in de auto', 'Android Auto met vier tabbladen, waaronder een hitlijst: de top afleveringen van je laatst gekozen lijst, met één tik op het scherm van de auto.',
      phone('Auto', 'Het autoscherm, tabblad Hitlijst'),
      [['Tabbladen', 'Verder luisteren (wachtrij en half beluisterd), Nieuw (gevolgde shows), Hitlijst, en Gedownload zodra functie 1 er is.'],
       ['Bediening', '−15, afspelen, +30 en snelheid. Geen toetsenbord onderweg; wel "Hey Google, speel [show]" en hervatten.'],
       ['Techniek', 'De bestaande PlaybackService wordt een MediaLibraryService. Hoezen via een eigen ContentProvider. Zonder verbinding de laatst opgehaalde lijst.'],
       ['Volgorde', 'Eerst de speler op een echt toestel testen. Android Automotive is een aparte stap met een eigen keuring.']], 'f11')}

    ${feature(10, 'Instellingen per show', 'Voor de show die je anders wilt: een eigen snelheid, intro en outro overslaan, en uitzonderingen op inkorten en downloaden. Standaard staat alles op "zoals algemeen".',
      phone('PerShow', 'Podcastpagina → Instellingen voor deze show'),
      [['Zichtbaar', 'Een label naast Volgt ("1,3× · intro 45 s"), en in de speler "voor deze show" bij de snelheid. Zo vraagt niemand zich af waarom een show ineens sneller speelt.'],
       ['Intro en outro', 'Intro overslaan alleen als je vanaf het begin start. Outro overslaan telt als beluisterd en gaat door naar de wachtrij. Reclame kan de intro per keer anders lang maken; dat staat erbij.'],
       ['Stap voor stap', 'Snelheid en overslaan kunnen nu. Inkorten en downloaden verschijnen als functie 4 en 1 er zijn. Een schakelaar voor meldingen bij nieuwe afleveringen komt pas als die meldingen bestaan.']], 'f12')}
  </div>

  <section class="block">
    <h2>Afgekeurd onderweg</h2>
    <p class="sub">Twee ideeën uit ronde 1 haalden het niet. Ze zijn vervangen door de auto (9) en instellingen per show (10), die in ronde 2 wel werden goedgekeurd.</p>
    <div class="rej">
      ${REJECTED.map(r => `<article><p><span class="no">Afgekeurd</span></p><h3>${r.title}</h3><p>${r.why}</p><p><b>Opnieuw indienen kan als:</b> ${r.again}</p></article>`).join('')}
    </div>
  </section>

  <section class="block">
    <h2>Volgorde van bouwen</h2>
    <ol class="plan">
      <li><div><h3>Nu al: bijhouden wat je luistert</h3><p>Geluisterde tijd per dag per show en een volgdatum. Klein werk, maar elke week wachten kost de Terugblik een week.</p></div></li>
      <li><div><h3>De basis: downloaden, OPML, stilte inkorten</h3><p>Wat alle grote apps hebben. OPML brengt volgen op feed-adres mee, inkorten is een paar regels Media3.</p></div></li>
      <li><div><h3>Wat Toadcast eigen maakt: weekoverzicht, hitlijstradio, terugblik</h3><p>Allemaal op de historie van de verzamelaar. Het weekoverzicht zet de achtergrondtaak neer die later ook een widget kan verversen.</p></div></li>
      <li><div><h3>Speler en auto: hoofdstukken, per show, Android Auto</h3><p>Android Auto pas als de speler op een echt toestel getest is.</p></div></li>
      <li><div><h3>Meelezen, na de meting</h3><p>Hangt af van hoeveel afleveringen in de top 200 een transcriptie leveren.</p></div></li>
    </ol>
    <p class="note-p">Alle podcasts, afleveringen, plekken en cijfers in de schermen zijn verzonnen. De cijfers over de tien apps komen uit het onderzoek (onderzoek.md, met bronnen).</p>
  </section>

  <section class="block">
    <h2>Bronnen</h2>
    <ul class="srcs">
      <li>Edison: <a href="https://ssrs.com/insights/youtube-is-the-preferred-podcast-listening-service/">YouTube is the preferred podcast listening service</a>, <a href="https://www.edisonresearch.com/the-podcast-consumer-2025/">Podcast Consumer 2025</a></li>
      <li>Cumulus / Signal Hill: <a href="https://www.westwoodone.com/blog/2025/11/17/audio-remains-the-primary-mode-of-podcast-consumption-despite-growing-video-use-92-say-they-listen-to-podcasts-according-to-cumulus-media-and-signal-hill-insights-podcast/">Podcast Download herfst 2025</a></li>
      <li>Hosting: <a href="https://www.buzzsprout.com/stats">Buzzsprout Stats</a>, <a href="https://transistor.fm/global-stats/">Transistor Global Stats</a>, Podtrac via <a href="https://podnews.net/article/biggest-podcast-app">Podnews</a></li>
      <li>Podcasting 2.0: <a href="https://stats.podcastindex.org/daily_counts.json">Podcast Index, dagelijkse tellingen</a></li>
      <li>Apps: <a href="https://github.com/Automattic/pocket-casts-android/blob/main/CHANGELOG.md">Pocket Casts changelog</a>, <a href="https://9to5mac.com/2026/04/08/overcast-launches-podcast-transcripts-in-new-app-update-for-iphone/">Overcast transcripties</a>, <a href="https://www.macrumors.com/2025/11/04/ios-26-2-podcasts-app-update/">Apple Podcasts iOS 26.2</a>, <a href="https://podcastaddict.com/faq/540">Podcast Addict statistieken</a>, <a href="https://9to5google.com/2023/12/07/google-podcasts-export/">Google Podcasts en OPML</a></li>
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
  // De werkbladstijl van de makers-pagina, zodat beide voorstellen er hetzelfde uitzien.
  const k = fs.readFileSync(path.join(dir, '..', 'kanalen', 'build.mjs'), 'utf8');
  const css = k.slice(k.indexOf('<style>') + 7, k.indexOf('${phoneCss}'));
  return css
    .replace(/--p-no:#8E2C1B;/, '--p-no:#8E2C1B; --p-nobg:#F6DDD6;')
    .replace(/--p-no:#FFA3B4;/g, '--p-no:#FFA3B4; --p-nobg:#3A1E24;');
}

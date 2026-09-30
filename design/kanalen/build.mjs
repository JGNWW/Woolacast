// Bouwt kanalen.html: onderzoek, de vijf goedgekeurde concepten met hun schermen,
// en het verloop van de beoordeling. Dit bestand wordt als Artifact gepubliceerd.
import fs from 'node:fs';
import path from 'node:path';
import { screens, CSS } from './screens.mjs';
import { VERDICTS } from './verdicts.mjs';

const dir = path.dirname(new URL(import.meta.url).pathname);
const base = fs.readFileSync(path.join(dir, '..', 'base.css'), 'utf8');
// base.css zet zijn tokens op :root; op deze pagina horen ze alleen bij de telefoons.
const phoneCss = (base + CSS)
  .replace(':root{', '.ph{')
  .replace('body{margin:0}', '')
  .replace(/a\{color:var\(--pri\);text-decoration:none\}\s*a:hover\{color:var\(--priOn\)\}/, '');

const phone = (key, caption) => `
      <figure class="shot">
        <div class="fit"><div class="scale">${screens[key].body}</div></div>
        <figcaption>${caption}</figcaption>
      </figure>`;

const verdict = (id) => {
  const v = VERDICTS[id];
  return `
        <details class="rounds">
          <summary><span class="ok">Goedgekeurd</span> in ronde ${v.rounds.length}<span class="muted"> · ${v.rounds.length - 1}× bijgeschaafd</span></summary>
          <ol>${v.rounds.map((r, i) => `<li><b class="${r.ok ? 'ok-t' : 'no-t'}">Ronde ${i + 1}: ${r.ok ? 'goedgekeurd' : 'afgekeurd'}.</b> ${r.text}</li>`).join('')}</ol>
        </details>`;
};

const concept = (n, title, lede, shots, points, id) => `
  <section class="concept" id="c${n}">
    <div class="shots">${shots}</div>
    <div class="copy">
      <p class="num">Concept ${n}</p>
      <h2>${title}</h2>
      <p class="lede">${lede}</p>
      <dl>${points.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('')}</dl>
      ${verdict(id)}
    </div>
  </section>`;

const html = `<title>Kanalen voor Toadcast</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&display=swap">
<style>
/* Werkblad van de ontwerpstudio: het papier en ember van de app, telefoons naast de tekst. */
:root{
  --p-bg:#F3EEE6; --p-card:#FFFDFA; --p-ink:#211D17; --p-ink2:#5F564B; --p-line:#E0D6C6;
  --p-accent:#B24A22; --p-ok:#0B6E3E; --p-okbg:#DCF0E4; --p-no:#8E2C1B;
  --f-display:'Bricolage Grotesque','Instrument Sans',ui-sans-serif,system-ui,sans-serif;
  --f-body:'Instrument Sans',ui-sans-serif,system-ui,sans-serif;
}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){
  --p-bg:#0E0F11; --p-card:#16171A; --p-ink:#EDEEF0; --p-ink2:#A0A3AA; --p-line:#2C2E33;
  --p-accent:#F0895B; --p-ok:#7FD9A4; --p-okbg:#1B3528; --p-no:#FFA3B4; color-scheme:dark}}
:root[data-theme="dark"]{
  --p-bg:#0E0F11; --p-card:#16171A; --p-ink:#EDEEF0; --p-ink2:#A0A3AA; --p-line:#2C2E33;
  --p-accent:#F0895B; --p-ok:#7FD9A4; --p-okbg:#1B3528; --p-no:#FFA3B4; color-scheme:dark}
body{background:var(--p-bg);color:var(--p-ink);font-family:var(--f-body);font-size:16px;line-height:1.55;margin:0}
.page{max-width:1120px;margin:0 auto;padding-inline:20px;padding-block:48px 80px}
h1,h2,h3{font-family:var(--f-display);letter-spacing:-.02em;text-wrap:balance;margin:0}
h1{font-size:clamp(34px,6vw,56px);line-height:1.02;font-weight:800}
h2{font-size:clamp(24px,3.2vw,32px);line-height:1.1;font-weight:700}
h3{font-size:19px;font-weight:700;margin-bottom:10px}
p{margin:0}
.eyebrow-p{font-size:12px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:var(--p-accent);margin-bottom:14px}
.intro{display:grid;gap:18px;max-width:68ch;margin-bottom:40px}
.intro p{color:var(--p-ink2);font-size:17px}
.intro strong{color:var(--p-ink)}
.jump{display:flex;flex-wrap:wrap;gap:8px;margin-top:6px}
.jump a{display:inline-flex;align-items:center;gap:8px;min-height:44px;padding:0 14px;border-radius:12px;background:var(--p-card);border:1px solid var(--p-line);color:var(--p-ink);font-weight:600;font-size:14px;text-decoration:none}
.jump a b{font-family:var(--f-display);color:var(--p-accent)}
.jump a:focus-visible,summary:focus-visible{outline:2px solid var(--p-accent);outline-offset:2px}
.block{margin-top:56px}
.block>h2{margin-bottom:8px}
.block>.sub{color:var(--p-ink2);max-width:68ch;margin-bottom:20px}
.tablewrap{overflow-x:auto;border:1px solid var(--p-line);border-radius:16px;background:var(--p-card)}
table{border-collapse:collapse;width:100%;min-width:640px;font-size:14.5px}
th,td{text-align:left;vertical-align:top;padding:12px 16px;border-bottom:1px solid var(--p-line)}
th{font-size:12px;letter-spacing:.1em;text-transform:uppercase;color:var(--p-ink2);font-weight:700}
tr:last-child td{border-bottom:0}
td:first-child{font-weight:700;white-space:nowrap}
.facts{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
.fact{background:var(--p-card);border:1px solid var(--p-line);border-radius:16px;padding:16px 18px}
.fact .big{font-family:var(--f-display);font-size:34px;font-weight:800;letter-spacing:-.03em;font-variant-numeric:tabular-nums;line-height:1.1}
.fact p{color:var(--p-ink2);font-size:14.5px;margin-top:4px}
.lessons{display:grid;gap:10px;margin-top:18px;max-width:72ch;padding-left:20px;color:var(--p-ink2)}
.lessons b{color:var(--p-ink)}
.concept{display:grid;grid-template-columns:auto minmax(0,1fr);gap:40px;align-items:start;padding:40px 0;border-top:1px solid var(--p-line)}
.concept:first-of-type{border-top:0}
.shots{display:flex;gap:20px}
.shot{margin:0;width:300px;max-width:100%}
.shot figcaption{font-size:13px;color:var(--p-ink2);margin-top:10px}
.fit{width:100%;aspect-ratio:390/844;overflow:hidden;border-radius:26px;box-shadow:0 1px 0 var(--p-line),0 14px 34px rgba(33,29,23,.18);background:#FAF6F0}
.scale{width:390px;height:844px;transform-origin:0 0}
.copy{min-width:0;max-width:62ch}
.num{font-size:12px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:var(--p-accent);margin-bottom:6px}
.lede{font-size:17px;color:var(--p-ink2);margin:12px 0 18px}
dl{display:grid;grid-template-columns:128px minmax(0,1fr);gap:10px 16px;margin:0 0 20px;font-size:15px}
dt{font-weight:700}
dd{margin:0;color:var(--p-ink2)}
.rounds{border:1px solid var(--p-line);border-radius:14px;background:var(--p-card);padding:0 16px}
.rounds summary{min-height:48px;display:flex;align-items:center;gap:8px;cursor:pointer;font-weight:600;font-size:14.5px}
.ok{display:inline-flex;align-items:center;height:24px;padding:0 9px;border-radius:7px;background:var(--p-okbg);color:var(--p-ok);font-size:12.5px;font-weight:700}
.muted{color:var(--p-ink2);font-weight:500}
.rounds ol{margin:0 0 14px;padding-left:20px;display:grid;gap:8px;font-size:14.5px;color:var(--p-ink2)}
.ok-t{color:var(--p-ok)}.no-t{color:var(--p-no)}
.plan{counter-reset:s;display:grid;gap:12px;max-width:72ch;padding:0;list-style:none}
.plan li{counter-increment:s;display:grid;grid-template-columns:40px minmax(0,1fr);gap:12px;align-items:start}
.plan li::before{content:counter(s);font-family:var(--f-display);font-weight:800;font-size:22px;color:var(--p-accent);line-height:1.3}
.plan li p{color:var(--p-ink2)}
.srcs{font-size:14px;color:var(--p-ink2);display:grid;gap:6px;padding-left:20px;margin:0}
.srcs a{color:var(--p-accent)}
.note-p{font-size:13.5px;color:var(--p-ink2);margin-top:14px}
@media (max-width:880px){
  .concept{grid-template-columns:1fr;gap:24px}
  .shots{flex-wrap:wrap}
  .shot{width:min(300px,100%)}
  dl{grid-template-columns:1fr;gap:2px}
  dd{margin-bottom:10px}
}
@media (prefers-reduced-motion:no-preference){.rounds summary{transition:color .15s}}
${phoneCss}
.ph{border-radius:0}
</style>

<div class="page">
  <header class="intro">
    <p class="eyebrow-p">Toadcast · ontwerpvoorstel</p>
    <h1>Kanalen: alle podcasts van één maker</h1>
    <p>De vraag was een kanaalachtige functie: podcasts per bron zien, en ze ordenen op populair of recent. In de app heet zo'n bron een <strong>maker</strong>, net als het bestaande makerscherm. "Bron" blijft het woord voor Apple en Spotify. Waar Apple een kanaal kent, levert dat het logo en de kleur. Anders is het de makersnaam.</p>
    <p>Hieronder staan vijf concepten. Een strenge ontwerpcriticus met kennis van design language en podcastapps heeft ze beoordeeld. Een concept kwam pas op deze lijst na goedkeuring; afgekeurde versies zijn bijgeschaafd en opnieuw voorgelegd. <strong>Advies:</strong> bouw 1 en 2 eerst. Samen zijn ze de kern, en ze maken iets wat geen andere podcastapp heeft: makers als hitlijst.</p>
    <nav class="jump" aria-label="Concepten">
      <a href="#c1"><b>1</b> Makerpagina</a><a href="#c2"><b>2</b> Makers in de hitlijst</a><a href="#c3"><b>3</b> Apple tegenover Spotify</a><a href="#c4"><b>4</b> Makers volgen</a><a href="#c5"><b>5</b> Tips per medium</a>
    </nav>
  </header>

  <section class="block">
    <h2>Hoe anderen het doen</h2>
    <p class="sub">Kanalen of netwerken bestaan in de grote apps, maar niemand zet ze in een hitlijst.</p>
    <div class="tablewrap"><table>
      <thead><tr><th>App</th><th>Wat er is</th><th>Ordening</th></tr></thead>
      <tbody>
        <tr><td>Apple Podcasts</td><td>Kanalen: logo, achtergrondkleur, beschrijving en eventueel een abonnement. Volg je een show uit een kanaal, dan verschijnt het kanaal in je Bibliotheek.</td><td>Vaste secties: Nieuwe programma's, Topprogramma's, Topafleveringen</td></tr>
        <tr><td>YouTube</td><td>Het kanaal is de kern, met tabbladen en abonneren.</td><td>Chips boven de lijst: Nieuwste · Populair · Oudste</td></tr>
        <tr><td>YouTube Music</td><td>Podcastpagina's met sorteren.</td><td>Nieuwste · Oudste · Populairst</td></tr>
        <tr><td>Pocket Casts</td><td>Netwerken op Ontdek, door de redactie gekozen. Nu in aanbouw: netwerken in de zoekresultaten, met rond beeld, en de makersregel op de podcastpagina als link naar het netwerk.</td><td>Redactioneel</td></tr>
        <tr><td>Podchaser</td><td>Netwerkpagina's (bèta) met alle podcasts van een netwerk.</td><td>Keuzelijst</td></tr>
        <tr><td>Podtrac, Podscribe</td><td>Ranglijsten van uitgevers, maandelijks en alleen voor de VS.</td><td>Bereik</td></tr>
        <tr><td>Spotify</td><td>Geen makerspagina gevonden; de maker staat als tekst onder de titel. De hitlijsten geven wel <code>showPublisher</code> mee.</td><td>—</td></tr>
      </tbody>
    </table></div>
    <ul class="lessons">
      <li><b>Apple:</b> een kanaal is een merk, met logo en kleur. Drie ingangen volstaan: nieuw, populair, afleveringen.</li>
      <li><b>YouTube:</b> sorteerchips boven één lijst zijn duidelijker dan losse secties, zodra een kanaal groot is.</li>
      <li><b>Pocket Casts:</b> kiest dezelfde ingangen die Toadcast al heeft (de makersregel en zoeken), en geeft netwerken rond beeld om ze van een podcast te onderscheiden.</li>
      <li><b>Het gat:</b> een ranglijst van makers per land, per categorie en per dag heeft niemand.</li>
    </ul>
  </section>

  <section class="block">
    <h2>Wat de data toelaat</h2>
    <p class="sub">Gemeten op 30 september 2026, op de Nederlandse Top 200 van Apple (alle categorieën).</p>
    <div class="facts">
      <div class="fact"><div class="big">67 / 200</div><p>shows hangen aan een Apple-kanaal, verdeeld over 25 kanalen. NPO Luister alleen al heeft 24 shows in de lijst.</p></div>
      <div class="fact"><div class="big">3 lijsten</div><p>per kanaal zijn te lezen: topshows, nieuwe shows en topafleveringen. Dat gaat via het webtoken dat de verzamelaar al ophaalt.</p></div>
      <div class="fact"><div class="big">133</div><p>shows hebben alleen een makersnaam, en die is rommelig ("NPO Luister / BNNVARA", "Dag en Nacht | Podimo"). BNR en De Telegraaf hebben geen kanaal.</p></div>
      <div class="fact"><div class="big">0 extra</div><p>aanroepen voor "recent": Apple geeft de datum van de nieuwste aflevering al mee in elk zoekresultaat.</p></div>
    </div>
  </section>

  <div class="block">
    ${concept(1, 'De makerpagina', 'Het bestaande makerscherm wordt een echte pagina, met logo, kleur, volgen en sorteren op Populair, Recent en A–Z.',
      phone('Maker', 'Met Apple-kanaal: logo en kleurgloed') + phone('MakerDonker', 'Donker, met een donker logo dat omhoog geklemd is'),
      [['Populair', 'De plek in de hitlijst die je op Hitlijsten gekozen hebt (bron en land), altijd over alle categorieën. De regel onder de chips zegt welke lijst het is; tik erop om van bron te wisselen. Shows buiten de lijst staan eronder, op nieuwste aflevering.'],
       ['Recent', 'Nieuwste aflevering eerst.'],
       ['Ingangen', 'De makersregel op de podcastpagina (bestaat al), een sectie Makers in de zoekresultaten, en de andere concepten.'],
       ['Data', 'Kanalen uit de verzamelaar (logo, kleur, telling). Makers zonder kanaal via de bestaande zoekopdracht op naam: "12 gevonden in de Apple-catalogus".'],
       ['Zonder kanaal', 'Een neutraal monogram, geen gloed en geen deelknop.']], 'c1')}
    ${concept(2, 'Makers in de hitlijst', 'Dezelfde ranglijst, geteld per maker: wie heeft vandaag de meeste shows in de Top 200, per bron, land en categorie. Dat laat geen andere app zien.',
      phone('Makers', 'Hitlijsten → Podcasts → Per maker'),
      [['Plaats', 'Een weergave binnen het tabblad Podcasts, via een keuzechip in de toelichtingsregel. Er komt geen vijfde tabblad.'],
       ['Telregel', 'Het Apple-kanaal, anders het eerste deel van de makersnaam. Elke show telt één keer; makers met 2+ shows; bij een gelijk aantal gaat de hoogste plek voor. Een i-icoon legt het uit.'],
       ['Beweging', 'De rang van de maker ten opzichte van gisteren, zoals op de andere tabbladen. Op dag één komt die uit de geschiedenis van de verzamelaar.'],
       ['Verder', 'Tik op een rij opent de makerpagina. Onder de lijst staat "Vergelijk met Spotify" (concept 3).']], 'c2')}
    ${concept(3, 'Apple tegenover Spotify', 'De kernbelofte van de app, dezelfde lijst per bron, op het niveau van makers: wie is groot op Apple en klein op Spotify?',
      phone('Vergelijk', 'Hellingsgrafiek van de makersranglijsten'),
      [['Koppeling', 'Per show: Spotify-show → Apple-show → maker van Apple. Zo krijgt een maker nooit twee namen. Wat niet te koppelen is, telt niet mee en wordt genoemd.'],
       ['Onderste rij', '"Minder dan 2 shows": waar een maker in de andere lijst te klein is. Dat is eerlijker dan "niet in de lijst".'],
       ['Schaal', 'De top 10 per bron op rijen van 44px, met "Toon alle 31 makers". Lange namen krijgen een ellips; de volledige naam staat in de kaart.'],
       ['Toegankelijk', 'Elke maker is een eigen element voor TalkBack. De gekozen lijn is ook dikker en het label vet, dus niet alleen anders gekleurd.']], 'c3')}
    ${concept(4, 'Makers volgen', 'Je volgt een maker, niet al zijn shows. De Bibliotheek krijgt een tabblad Makers, met nieuwe podcasts bovenaan in de bestaande meldingskaart.',
      phone('Volgen', 'Bibliotheek → Makers'),
      [['Nieuwe podcast', 'Voor kanalen uit de lijst van nieuwe shows van de verzamelaar; alleen die geven een pushmelding. Zonder kanaal alleen in de kaart, en alleen als de show nooit eerder gezien is, ≤ 3 afleveringen heeft en jonger is dan 14 dagen.'],
       ['Tellingen', '"2 van 31 shows met een nieuwe aflevering". De ID\'s komen uit de verzamelaar, opgehaald met 1–2 batch-aanroepen, zonder RSS te verversen.'],
       ['Apple-regel', 'Makers van shows die je al volgt, worden voorgesteld met een knop Volg.'],
       ['Geen dubbelingen', 'Afleveringen blijven onder Gevolgd; Makers toont alleen tellingen en nieuwe shows.']], 'c4')}
    ${concept(5, 'Tips per medium', 'Een krant of omroep is ook een bron: wie je vertrouwt. Het bestaande Tips-scherm krijgt, als je één medium kiest, een telling, sorteren en een brug naar de eigen podcasts van dat medium.',
      phone('Tips', 'Tips van de media, gefilterd op één medium'),
      [['Sorteren', 'Nieuwste eerst (met maandkoppen) of Vaakst getipt (met "getipt door 3 media").'],
       ['Brug', '"Maakt ook podcasts" opent de makerpagina, maar alleen bij een exacte naamsovereenkomst. Eigen producties tellen niet als tip; dat filtert de verzamelaar al.'],
       ['Hergebruik', 'De bestaande tiprij, kop, verversknop en chiprij. Alleen de regel met het medium valt weg als er op dat medium gefilterd is.']], 'c5')}
  </div>

  <section class="block">
    <h2>Volgorde van bouwen</h2>
    <ol class="plan">
      <li><div><h3>Makerpagina en makersnormalisatie</h3><p>Sorteren op Populair, Recent en A–Z op het bestaande scherm, plus de regel die een makersnaam gelijktrekt. Alles hierna leunt daarop.</p></div></li>
      <li><div><h3>Kanalen in de verzamelaar</h3><p>Per land een bestand met makers: logo, kleur, show-ID's en nieuwe shows. Dat is één extra stap in de nachtelijke ronde, met het token dat er al is.</p></div></li>
      <li><div><h3>Makers in de hitlijst</h3><p>Het onderscheidende stuk. Voor de beweging is alleen het rekenwerk op de momentopnames nieuw.</p></div></li>
      <li><div><h3>Volgen, vergelijken, tips</h3><p>Concept 4, 3 en 5 bouwen op het voorgaande en kunnen in willekeurige volgorde.</p></div></li>
    </ol>
    <p class="note-p">Alle makers, podcasts en plekken in de schermen zijn verzonnen. De cijfers bij "Wat de data toelaat" zijn gemeten.</p>
  </section>

  <section class="block">
    <h2>Bronnen</h2>
    <ul class="srcs">
      <li>Apple: <a href="https://podcasters.apple.com/support/886-create-a-channel">Create a channel</a>, <a href="https://podcasters.apple.com/support/4118-set-up-channel-subscription">Set up a channel and subscription</a>, <a href="https://podcasts.apple.com/nl/channel/npo-luister/id6743084397">kanaal NPO Luister</a></li>
      <li>YouTube: <a href="https://www.androidpolice.com/youtube-oldest-sorting-coming-back/">sorteren op oudste</a>, <a href="https://9to5google.com/2023/04/04/youtube-channel-podcasts-tab/">Podcasts-tabblad</a>, <a href="https://9to5google.com/2024/04/24/youtube-music-podcast-sort/">YouTube Music sorteert podcasts</a></li>
      <li>Pocket Casts: <a href="https://github.com/Automattic/pocket-casts-android/pull/5864">netwerken in zoeken</a>, <a href="https://github.com/Automattic/pocket-casts-android/pull/5860">netwerk per podcast</a>, <a href="https://pocketcasts.com/discover">Discover</a></li>
      <li>Podchaser: <a href="https://www.podchaser.com/networks">Networks</a> · Podscribe: <a href="https://podscribe.com/top-publishers">Top Podcast Publishers</a> · NPO: <a href="https://npo.nl/pers/persberichten-en-publicaties/npo-luister-wordt-volwaardig-audioplatform-en-lanceert-nieuwe-app">NPO Luister</a></li>
    </ul>
  </section>
</div>
<script>
(function(){
  function fit(){
    document.querySelectorAll('.fit').forEach(function(f){
      var s=f.querySelector('.scale'); if(s) s.style.transform='scale('+(f.clientWidth/390)+')';
    });
  }
  fit(); window.addEventListener('resize',fit);
  if (window.ResizeObserver) new ResizeObserver(fit).observe(document.body);
})();
</script>
`;
fs.writeFileSync(path.join(dir, 'kanalen.html'), html);
console.log('kanalen.html', html.length);

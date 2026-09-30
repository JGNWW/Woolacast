# Kanalen — vijf concepten (versie 3)

## Uitgangspunten (A1–A10 uit ronde 1)

- **Woorden.** In de app heet de entiteit **Maker**; het bestaande `MakerScreen`
  en de makersregel heten al zo. "Kanaal" is alleen de Apple-databron voor logo
  en kleur. **Bron** blijft uitsluitend Apple of Spotify. De vraag van de gebruiker
  ("podcasts per bron") lezen we als *per maker*, en concept 3 bedient ook de
  tweede lezing: *per maker, vergeleken tussen de bronnen*.
- **Data.**
  - Kanaalgegevens (logo, kleur, `showCount`, `new-shows`) komen uit de
    verzamelaar (`collect.py`), omdat alleen die het webtoken heeft. Hij schrijft
    per land `apple/{land}/makers.json` naar `raw.githubusercontent`, alleen voor
    kanalen van shows die in de verzamelde lijsten staan.
  - Voor alle andere makers gebruikt de app live `byMaker` (zoeken op naam,
    limiet 60) met `releaseDate` en `trackCount`. De telling heet dan "gevonden
    in de Apple-catalogus", niet "podcasts".
- **Normalisatie van de makersnaam.** Eerst het Apple-kanaal. Anders het eerste
  segment na splitsen op ` / `, ` | ` en ` & ` ("NPO Luister / BNNVARA" wordt
  "NPO Luister"). Daarna hoofdletters en leestekens gelijktrekken. Voor Spotify
  (`showPublisher`) geldt dezelfde regel. Elke show telt bij precies één maker.
- **Maker zonder kanaal.** Neutraal monogram (`--surf3` met `--ink2`), geen
  gloed en geen deelknop. Te zien in 1 (donker), 2, 3 en 4.
- **Vorm.**
  - Makerlogo's zijn overal **rond**; hoezen blijven afgeronde vierkanten.
  - Componenten van de app worden hergebruikt:
    - `FilterChipBox` voor sorteren en filteren (aan = `--priC`);
    - `SmallChip` voor keuzelijsten;
    - `UnderlineTabs` (15px, tussenruimte 24) voor weergaven;
    - `WoolButton` (46px);
    - de rijen van `MakerScreen` (72px, hoes 52, chevron);
    - `AlertCard` en `TipRow` ongewijzigd.
  - Chips en pillen van 26–36px krijgen een aanraakgebied van 48dp
    (`minimumInteractiveComponentSize`).
- **Contrast.** Toelichtingsregels staan in `--ink2` (5,38:1), niet in `--ink3`.
  Rangcijfers zijn 19px/800 (grote tekst). De gloed van de makerpagina loopt tot
  boven in de statusbalk (`fullBleed`, net als de podcastpagina) op 8% van de
  kanaalkleur (donker 16%, bij een logo met een ring van 1px).
- **Typografie.** 26 / 14,5 / 12,5, met 11 voor labels. Chips (13) en tabbladen
  (15) volgen hun bestaande component.
- **Donker.** Met de tokens van Beeldgloed. De kanaalkleur wordt in donker
  omhoog geklemd tot hij ≥3:1 haalt tegen `#0E0F11`: hij wordt naar `#EDEEF0`
  gemengd (`liftForDark`), en de gloed gebruikt die geklemde kleur. Zie het
  scherm *Donker kanaallogo*, met Podium (`#1E1B16`).
- **Monogram** van makers zonder kanaal: `--ink` op `--surf3`, ook op 44–48px.

## 1 · De makerpagina
Het bestaande `MakerScreen` wordt uitgebreid. Ingangen: de makersregel op de
podcastpagina (bestaat al), een nieuwe sectie **Makers** in de zoekresultaten
(ronde logo's, zoals bij Pocket Casts), en de concepten 2–5.

- **Kop.** Rond logo, eyebrow "Maker", naam, telling. Eén primaire actie:
  **Volg maker** (wat volgen doet: zie 4). De deelknop is er alleen bij een
  Apple-kanaal-URL.
- **Sorteren** met drie chips:
  - **Populair**: de plek in de hitlijst van de bron en het land die je op
    Hitlijsten hebt gekozen, **altijd alle categorieën**. De toelichtingsregel
    zegt welke lijst het is en is tikbaar om van bron te wisselen. Shows buiten
    die lijst staan onder de tussenkop "Niet in de Top 200 · op nieuwste
    aflevering", met "—" als plek.
  - **Recent**: nieuwste aflevering eerst (`releaseDate`).
  - **A–Z**.
- **Rijen.** De plekpil heeft een toegankelijke tekst ("plek 4 in Apple NL").
- **De toelichting heeft één vaste vorm:** "Plek in {bron} · {land} · Top 200".
- **Populair met Spotify als bron** koppelt per show: Spotify-show → Apple-show,
  dezelfde opzoeking als op de podcastpagina. Wat niet te koppelen is, staat onder
  de lijst ("1 Spotify-show is niet aan Apple te koppelen").
- **Gevolgd** toont de knop met rand en een vinkje (*Volgt*), zodat hij ook in
  donker als knop leest.

Geschrapt na ronde 1: de chip "Nieuw", de knop "Nieuwste" en de zelfbedachte
kanaalvolgorde.

## 2 · Makers in de hitlijst
Op Hitlijsten → Podcasts wissel je met een `SmallChip` rechts in de
toelichtingsregel tussen **Per show** en **Per maker**. Dat kost geen extra regel.
Het is een weergave van dezelfde ranglijst, geen vijfde tabblad. Afleveringen,
Trending en Nieuw hebben geen makersweergave.

- **Telregel.**
  - Normalisatie zoals hierboven; elke show telt één keer, dus de som is ≤ 200.
  - Makers met 1 show tonen we niet ("2+ shows").
  - Bij een gelijk aantal gaat de maker met de hoogste plek voor. Voorbeeld:
    Studio Hemel (9, hoogste #1) staat boven Radio Oost (9, hoogste #9).
  - Het i-icoon (aanraakgebied 48dp) opent een sheet "Hoe tellen we?".
  - De lijstgrootte in de toelichting volgt de bron: Spotify-categorieën zijn
    50 diep.
- **Rij.** Rang, rond logo, naam, "24 shows · hoogste #2", drie hoesjes en de
  beweging.
- **Beweging** is de verandering in *rang van de maker* ten opzichte van
  **gisteren**. Dat is dezelfde basis als op de andere tabbladen:
  `store.baseline`.
  - Op dag één rekent de app de makerslijst van gisteren uit de
    `shows.history.json` van de verzamelaar, zoals hij elders de `moves` uit de
    dataset haalt.
  - Een maker die gisteren niet in de makerslijst stond, krijgt "NIEUW".
- **Ingang naar 3.** Onder de lijst staat "Vergelijk met Spotify", of "met Apple"
  als Spotify de bron is. Heeft het land geen lijst bij de andere bron, dan
  ontbreekt die regel.
- De aandeelbalk is geschrapt. Tik op een rij opent de makerpagina (1).

## 3 · Makers: Apple tegenover Spotify
Een eigen scherm voor de kernbelofte van de app, dezelfde lijst per bron, op het
niveau van makers.

- **Grafiek.** Twee kolommen met de makersranglijst van Apple en van Spotify in
  hetzelfde land (vandaag, Top 200, telregel van 2). Lijnen verbinden dezelfde
  maker.
- **Eén identiteit per maker.** De koppeling loopt per **show**: Spotify-show →
  Apple-show (dezelfde opzoeking als op de podcastpagina), en daarvan de maker
  van Apple (kanaal of genormaliseerde `artistName`). "Dag en Nacht | Podimo" bij
  Apple en "Podimo" bij Spotify worden zo nooit twee halve makers. Spotify-shows
  die niet te koppelen zijn, tellen niet mee. Hun aantal staat onder de grafiek.
- **Onderste rij: "Minder dan 2 shows".** Een maker staat in beide kolommen
  zodra hij in één lijst 2+ shows heeft. Heeft hij in de andere lijst 0 of 1,
  dan eindigt zijn lijn gestippeld in die onderste rij. Dat is waar, want
  "niet in de lijst" zou het niet zijn.
- **Schaal.**
  - Standaard de top 10 van beide lijsten. Daaronder "Toon alle 31 makers";
    de NL Top 200 heeft er 25–35 met 2+ shows.
  - Rijen van 44px; de hele label plus stip is het aanraakvlak (≥44×140).
  - Namen langer dan de kolom (±17 tekens) krijgen een ellips, zoals
    "Omroep Noordoost…". De volledige naam staat in de kaart en in de
    TalkBack-tekst.
- **Selectie en toegankelijkheid.**
  - De kaart bovenaan geeft de gekozen maker met plek en aantal shows per bron,
    en opent de makerpagina.
  - De grafiek zelf is `aria-hidden`. Elke maker is een eigen knooppunt:
    "Studio Hemel: Apple plek 3, 9 shows; Spotify plek 7, 3 shows".
  - Een lijst in dezelfde volgorde ligt ernaast voor TalkBack.
  - Kleur is niet de enige drager: de geselecteerde lijn is dikker en het label
    vet.
- **Randgevallen.**
  - Er is geen deelknop.
  - Landen zonder lijst bij een van de bronnen (zoals BE bij Spotify) hebben
    geen ingang naar dit scherm.
  - Gelijke stand zoals in 2.
- **Waarom apart van 1 en 2.** 1 gaat over de shows van één maker, 2 over één
  bron. Dit gaat over alle makers in twee bronnen tegelijk: wie groot is op Apple
  maar klein op Spotify.

## 4 · Makers volgen
Bibliotheek krijgt een tabblad: **Gevolgd · Makers · Wachtrij · Bewaard**. Het
model is dat van Apple, maar dan licht: je volgt een maker, niet al zijn shows.

- **Bovenaan de bestaande `AlertCard`** (donker paneel, bel, "Sinds gisteren"),
  met rijen "Nieuwe podcast · {maker} · N afl.". De show is de titel, de hele rij
  is tikbaar, en de NIEUW-pil staat er niet: kop en rij zeggen het al.
- **Wanneer is een show nieuw?**
  - Makers **met kanaal**: via `new-shows` uit de verzamelaar. Alleen deze
    krijgen een pushmelding (WorkManager, en de meldingsrechten van
    Android 13+).
  - Makers **zonder kanaal**: alleen in de kaart, nooit als pushmelding, en
    alleen als het ID nooit eerder gezien is, `trackCount` ≤ 3 heeft en een
    `releaseDate` van de laatste 14 dagen. Een oude show die in de
    zoekresultaten omhoog schuift, of een show die van naam veranderde, telt dus
    niet.
- **Je makers.** Per maker "N van M shows met een nieuwe aflevering" sinds
  gisteren.
  - Makers **met kanaal**: de lijst met show-ID's komt uit de verzamelaar,
    gevolgd door een batch-lookup (≤ 200 ID's per aanroep, dus 1–2 aanroepen,
    ook bij 217 shows). `releaseDate` zegt of er een nieuwe aflevering is.
  - Makers **zonder kanaal**: `byMaker`, en de tekst zegt "in de 12 gevonden
    shows". Het is geen volledige telling.
  - Er wordt geen RSS ververst. Tik opent de makerpagina op *Recent*.
- **Van podcasts die je volgt.** Makers van shows die je al volgt, met een
  knop *Volg*. Dat is de Apple-regel.
- **Lege toestand.** "Nog geen makers gevolgd", met direct daaronder de
  suggesties uit "Van podcasts die je volgt".
- **Ontdubbelen.** Afleveringen blijven onder *Gevolgd*. *Makers* toont alleen
  tellingen en nieuwe shows, dus niets verschijnt twee keer.

## 5 · Tips per medium
Het bestaande Tips-scherm houdt zijn opbouw: `TitleBar("Tips van de media")` met
de verversknop, en de chiprij met eerst het land, dan Alle, dan de media. Als er
één medium gekozen is, komen er drie dingen bij.

1. **Een telling en sortering.** "23 tips sinds juni", en een `SmallChip` met
   *Nieuwste eerst* en *Vaakst getipt*.
   - Bij *Nieuwste eerst* blijven de maandkoppen van `groupFor`.
   - Bij *Vaakst getipt* vervallen de maandkoppen en staat het aantal in de rij
     ("getipt door 3 media").
2. **Direct daaronder een compacte kaart "Maakt ook podcasts"**, met de
   vermelding "tellen niet als tip". Dat regelt `same_house` in `collect.py` al.
   - De kaart opent de makerpagina.
   - Hij verschijnt alleen bij een **exacte** overeenkomst tussen medium en maker
     na normalisatie, niet op de ruimere woordheuristiek van `same_house`.
   - Omgekeerd krijgt de makerpagina van zo'n medium de regel
     "Tipt ook podcasts →".
3. **`TipRow` blijft `TipRow`**, met één parameter: de regel met het medium
   verdwijnt als er op dat medium gefilterd is, omdat hij dan elke rij hetzelfde
   zou zeggen. De kop van het artikel staat zonder aanhalingstekens, met
   "Lees het artikel" en de afspeelknop.

Zo is "bron" ook: wie je vertrouwt. Er komt geen apart kanaalscherm naast Tips.

## Bekende punten buiten deze concepten
Ember-tekst op 12px ("Lees het artikel") haalt 4,19:1, en wit op ember haalt
4,45:1. Dat zit in bestaande componenten en is een apart punt voor de hele app.

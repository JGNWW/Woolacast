# Kanalen — vijf concepten (versie 2)

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
- **Donker.** Met de tokens van Beeldgloed; zie het scherm *Maker zonder kanaal,
  donker*.

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

Geschrapt na ronde 1: de chip "Nieuw", de knop "Nieuwste" en de zelfbedachte
kanaalvolgorde.

## 2 · Makers in de hitlijst
Op Hitlijsten → Podcasts een schakelaar **Per show | Per maker** (twee
`FilterChipBox`-chips). Het is een weergave van dezelfde ranglijst, geen vijfde
tabblad. Afleveringen, Trending en Nieuw hebben geen makersweergave.

- **Telregel.**
  - Normalisatie zoals hierboven; elke show telt één keer, dus de som is ≤ 200.
  - Makers met 1 show tonen we niet ("makers met 2+ shows").
  - Bij een gelijk aantal gaat de maker met de hoogste plek voor.
  - Het i-icoon opent een sheet "Hoe tellen we?".
  - De lijstgrootte in de toelichting volgt de bron: Spotify-categorieën zijn
    50 diep.
- **Rij.** Rang, rond logo, naam, "24 shows · hoogste #2", drie hoesjes en de
  beweging.
- **Beweging** is de verandering in *rang van de maker* ten opzichte van 7 dagen
  eerder, uit de eigen momentopnames. Dat is dezelfde betekenis als op de andere
  tabbladen.
  - Een maker die vorige week niet in de makerslijst stond, krijgt "NIEUW".
  - Met minder dan 7 dagen momentopnames is er geen pijl en geen "=".
- De aandeelbalk is geschrapt. Tik op een rij opent de makerpagina (1).

## 3 · Makers: Apple tegenover Spotify
Een eigen scherm voor de kernbelofte van de app, dezelfde lijst per bron, op het
niveau van makers.

- **Grafiek.** Twee kolommen met de makersranglijst van Apple en die van Spotify
  in hetzelfde land (vandaag, Top 200, makers met 2+ shows, telregel van 2).
  Lijnen verbinden dezelfde maker.
- **Niet in de lijst.** Een maker die in één lijst ontbreekt, eindigt gestippeld
  in de rij "Niet in de lijst".
- **Selectie.** Tik op een maker om zijn lijn te volgen. De kaart eronder geeft
  voor die maker de plek en het aantal shows per bron, en opent de makerpagina.
- **Ingang.** Vanuit 2, via "Vergelijk met Spotify" in de toelichting.
- **Waarom apart van 1.** 1 gaat over de shows van één maker. Dit gaat over alle
  makers tegelijk: wie groot is op Apple maar klein op Spotify. Dat laat geen
  andere podcastapp of dienst zien.
- **Eerlijkheid.** Spotify-makers komen uit `showPublisher`, dus er is geen
  titelmatching met Apple nodig. Er staan alleen makers in die in minstens één
  lijst 2 of meer shows hebben.

## 4 · Makers volgen
Bibliotheek krijgt een tabblad: **Gevolgd · Makers · Wachtrij · Bewaard**. Het
model is dat van Apple, maar dan licht: je volgt een maker, niet al zijn shows.

- **Bovenaan de bestaande `AlertCard`** (donker paneel, bel, "Sinds gisteren"),
  met rijen "Nieuwe podcast · {maker}". De show is de titel en de hele rij is
  tikbaar.
  - Bron voor kanalen: `new-shows` uit de verzamelaar.
  - Andere makers: een dagelijkse `byMaker`-vergelijking op nieuwe ID's, via
    WorkManager.
  - Een pushmelding alleen met de meldingsrechten van Android 13+.
- **Je makers.** Per maker "N shows met een nieuwe aflevering" sinds gisteren,
  uit `releaseDate` in één zoekopdracht per maker, dus zonder RSS te verversen.
  Tik opent de makerpagina op *Recent*. Er is geen samengevoegde afleveringsfeed,
  dus een maker met 217 shows vraagt nog steeds één aanroep.
- **Van podcasts die je volgt.** Makers van shows die je al volgt, met een
  knop *Volg*. Dat is de Apple-regel: volg je een show uit een kanaal, dan komt
  het kanaal in je Bibliotheek.
- **Ontdubbelen.** Afleveringen blijven onder *Gevolgd*. *Makers* toont alleen
  tellingen en nieuwe shows, dus niets verschijnt twee keer.

## 5 · Tips per medium
Het bestaande Tips-scherm met het mediafilter krijgt twee dingen.

1. **Kop en sortering voor het gekozen medium.** De kop toont "23 tips sinds
   juni". Een `SmallChip` biedt *Nieuwste eerst* en *Vaakst getipt* (het aantal
   media dat dezelfde show tipte). De rijen zijn de bestaande `TipRow`: kop van
   het artikel zonder aanhalingstekens, "Lees het artikel" en de afspeelknop.
2. **De brug naar de maker.** Heeft het medium zelf podcasts (een Apple-kanaal of
   makersnaam), dan staat onder de tips een kaart "Maakt ook podcasts", die de
   makerpagina opent. Een regel zegt: "Eigen podcasts van … tellen niet als tip".
   Dat regelt `same_house` in `collect.py` al. Omgekeerd krijgt de makerpagina
   van zo'n medium de regel "Tipt ook podcasts →".

Zo is "bron" ook: wie je vertrouwt. Er komt geen apart kanaalscherm naast Tips.

## Bekende punten buiten deze concepten
Ember-tekst op 12px ("Lees het artikel") haalt 4,19:1, en wit op ember haalt
4,45:1. Dat zit in bestaande componenten en is een apart punt voor de hele app.

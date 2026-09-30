# Beoordelingen door de criticus

Eén kritische agent met kennis van design language (Material 3, Apple HIG,
NN/g, Gestalt) en van podcastapps beoordeelde alle vijf concepten. Hij toetste:
- of het concept de vraag beantwoordt;
- de design language van de app (`base.css`, de componenten in `Components.kt`,
  P1–P8 uit `../donker-herontwerp/principles.md`);
- de eerlijkheid tegenover de gemeten data (`onderzoek.md`);
- informatieontwerp en toegankelijkheid. Het contrast is met python nagerekend
  volgens WCAG 2.x.

Een concept kwam pas op de lijst na goedkeuring. De schermafdrukken per ronde
zijn met `preview.mjs` te maken.

| Concept | R1 | R2 | R3 | R4 |
| --- | --- | --- | --- | --- |
| 1 · Makerpagina | afgekeurd | **goedgekeurd** | | |
| 2 · Makers in de hitlijst | afgekeurd | **goedgekeurd** | | |
| 3 · Apple tegenover Spotify | afgekeurd | afgekeurd | afgekeurd | **goedgekeurd** |
| 4 · Makers volgen | afgekeurd | afgekeurd | **goedgekeurd** | |
| 5 · Tips per medium | afgekeurd | **goedgekeurd** | | |

## Punten die voor alle concepten golden (ronde 1)
- **Eén woordenlijst.** *Maker* is de maker, *bron* alleen Apple of Spotify, en
  een *Apple-kanaal* is een databron.
- **Data van kanalen.** Die komen uit de verzamelaar (het webtoken), niet uit de
  app.
- **Makers zonder kanaal.** Er moet altijd een voorbeeld van zijn (twee derde
  van de lijst).
- **Makerlogo's zijn rond.** Een ander rol krijgt een andere vorm dan een hoes.
- **Bestaande componenten** in plaats van nieuwe chips, knoppen en rijen.
- **Contrast.**
  - Toelichtingsregels staan in `--ink2`.
  - De gloed gaat tot boven in de statusbalk, op 8%.
  - In donker wordt de kanaalkleur geklemd tot 3,2:1.
  - Monogrammen staan in `--ink`.
- **Typeschaal** 26 / 14,5 / 12,5 / 11.

## 1 · Makerpagina
- **R1 afgekeurd.**
  - De chip "Nieuw" had geen data en botste met het tabblad Nieuw.
  - Populair was niet gedefinieerd voor shows buiten de lijst.
  - De knop "Nieuwste" was willekeurig.
  - Er waren eigen componenten, en de kop haalde 2,47:1.
- **R2 goedgekeurd.**
  - Populair, Recent en A–Z.
  - Populair vast op alle categorieën, met een tikbare toelichting.
  - Een tussenkop "Niet in de Top 200".
  - Rijen van 72px en één primaire knop.
- **Daarna verwerkt.**
  - Koppeling van Spotify naar Apple, met een telling van wat niet te koppelen is.
  - "Volgt" met rand en vinkje.
  - Klemming in donker en een schijf achter echte logo's.

## 2 · Makers in de hitlijst
- **R1 afgekeurd.**
  - Vijf tabbladen passen niet.
  - Er was geen telregel.
  - De aandeelbalk was geschaald op de leider.
- **R2 goedgekeurd.**
  - Een weergave binnen Podcasts.
  - Eén maker per show, 2+ shows, gelijke stand op de hoogste plek.
- **Daarna verwerkt.**
  - De gelijke stand in de mockup.
  - Beweging ten opzichte van gisteren, zoals elders.
  - De schakelaar als `SmallChip`, met de ingang naar 3 in het menu.

## 3 · Apple tegenover Spotify
- **R1 afgekeurd.** Als raster per maker was het overbodig naast 1. Markering
  alleen met kleur, en een willekeurige kolom.
- **R2 afgekeurd.** De hellingsgrafiek koppelde makers niet over de bronnen
  heen. "Niet in de lijst" was onwaar bij 1 show, en de rijen waren 30px.
- **R3 afgekeurd.** Een maker op plek 11+ in de andere lijst had geen plek.
- **R4 goedgekeurd.**
  - Zones "Plek 11+" (met de echte plek) en "Minder dan 2 shows".
  - Spotify-exclusives tellen via hun makersnaam.
  - Een hint om te tikken.
- **Daarna verwerkt.** Een eigen rij per maker in beide zones, en een
  TalkBack-tekst.

## 4 · Makers volgen
- **R1 afgekeurd.**
  - Verzonnen Bibliotheek-tabbladen.
  - Een feed van alle shows schaalt niet.
  - Een tweede meldingsstijl, en kapotte meta.
- **R2 afgekeurd.**
  - "Eén aanroep per maker" klopte niet, door de zoeklimiet.
  - Valse meldingen van nieuwe podcasts.
  - Een dubbele NIEUW-pil.
- **R3 goedgekeurd.**
  - ID-lijst en batch-lookup.
  - Pushmeldingen alleen voor kanalen.
  - Drie voorwaarden voor makers zonder kanaal.
- **Daarna verwerkt.**
  - Kanalen blijven 90 dagen bewaard.
  - "Gevonden vandaag".
  - Chevrons ook in de chart-alerts.

## 5 · Tips per medium
- **R1 afgekeurd.**
  - Dubbelde het mediafilter op Tips en concept 1.
  - Verzonnen citaten.
  - "Volg" en "Nieuwste" zonder betekenis.
- **R2 goedgekeurd.** Een kleine uitbreiding van Tips, met de ongewijzigde
  `TipRow` en een exacte koppeling tussen medium en maker.
- **Daarna verwerkt.**
  - De bestaande titelbalk en landchip.
  - De kaart hoger.
  - De datum terug in de rij.

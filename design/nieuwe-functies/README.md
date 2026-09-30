# Nieuwe functies — tien goedgekeurde ideeën

Gevraagd: tien ideeën voor nieuwe functies, beoordeeld door een criticus die de
tien meest gebruikte podcastapps en hun meestgebruikte functies heeft onderzocht.
Een idee kwam pas op de lijst na goedkeuring; een afgekeurd idee werd vervangen
door een nieuw idee dat opnieuw beoordeeld werd.

| # | Functie | Ronde |
| --- | --- | --- |
| 1 | Downloaden en automatisch klaarzetten | 1 |
| 2 | Hoofdstukken | 1 |
| 3 | Transcriptie meelezen (eerst de dekking meten) | 1 |
| 4 | Stilte inkorten en stemversterking | 1 |
| 5 | Weekoverzicht van de hitlijsten | 1 |
| 6 | Terugblik, met "vroege luisteraar" | 1 |
| 7 | OPML importeren en exporteren, plus zelf een feed toevoegen | 1 |
| 8 | Hitlijstradio (zonder proefmodus) | 1 |
| 9 | Toadcast in de auto (Android Auto) | 2 |
| 10 | Instellingen per show | 2 |

Afgekeurd in ronde 1: een widget op het beginscherm en slimme lijsten met regels.
De redenen en wat een herkansing nodig heeft staan in `beoordelingen.md`.

In de beoordeling hebben de ideeën hun volgnummer van indienen: de hitlijstradio
was idee 10, de auto 11 en instellingen per show 12. Op de pagina zijn ze 8, 9 en 10.

## Bestanden
- `onderzoek.md` — de top 10 podcastapps, hun functies, en de lessen voor Toadcast
- `beoordelingen.md` — het oordeel van de criticus per idee, met voorwaarden
- `screens.mjs` — de telefoonschermen en het autoscherm (verzonnen namen)
- `preview.mjs` → `preview.html` — alle schermen naast elkaar
- `verdicts.mjs` + `build.mjs` → `functies.html` — de pagina met alles

```
node design/nieuwe-functies/preview.mjs
node design/nieuwe-functies/build.mjs
```

## Gebouwd: 1, 2, 3 en 7

Downloaden, hoofdstukken, meelezen en OPML zijn gebouwd en beoordeeld door een
senior developer en een senior designer, tot beiden alle vier goedkeurden
(drie rondes). Het verloop staat in `bouw-beoordeling-developer.md` en
`bouw-beoordeling-designer.md`; de schermen van de echte app in `schermen/`
(`./gradlew testDebugUnitTest -Pscreenshots`, test `FeatureScreenshots`). De
meting van hoeveel feeds tekst en hoofdstukken leveren staat in `dekking.md`.

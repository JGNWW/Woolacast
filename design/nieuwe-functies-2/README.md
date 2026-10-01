# Nieuwe functies, tweede reeks — tien goedgekeurde ideeën

Gevraagd: tien nieuwe ideeën voor functies, beoordeeld door een criticus die de
tien meest gebruikte podcastapps en hun meestgebruikte functies heeft onderzocht.
Een idee kwam pas op de lijst na goedkeuring; een afgekeurd idee werd vervangen
door een nieuw idee dat opnieuw beoordeeld werd. De tien uit de eerste reeks
(`../nieuwe-functies/`) zijn niet opnieuw voorgesteld.

Dertien ideeën in drie rondes: tien goedgekeurd, drie afgekeurd. Op de pagina
staan ze in de bouwvolgorde van de criticus; tussen haakjes het nummer van
indienen.

| # | Functie | Idee | Ronde |
| --- | --- | --- | --- |
| 1 | Nieuw bovenaan Gevolgd, en meldingen bij een nieuwe aflevering | 1 | 1 |
| 2 | Casten naar Chromecast en speakers | 3 | 1 |
| 3 | Spoelknoppen instellen, en de koptelefoon | 11 | 2 |
| 4 | Zoeken in de afleveringen van één show | 13 | 3 |
| 5 | Luistergeschiedenis | 4 | 1 |
| 6 | Verhaal in delen: begin bij aflevering 1 | 9 | 1 |
| 7 | Rond deze sprong (wat er gebeurde rond een stijging) | 5 | 1 |
| 8 | Een slaaptimer die meedenkt | 8 | 1 |
| 9 | Bladwijzers (zonder clips) | 2 | 1 |
| 10 | Back-up en verhuizen (alleen handmatig) | 7 | 1 |

Afgekeurd: de deelkaart van een notering (6) en proefluisteren vanuit de
hitlijst (10) in ronde 1, Apple en Spotify naast elkaar (12) in ronde 2. Waarom,
en wat een herkansing nodig heeft, staat in `beoordelingen.md`.

## Bestanden
- `ideeen-ronde-1.md`, `-2.md`, `-3.md` — de ideeën zoals ze zijn ingediend
- `onderzoek-aanvulling.md` — functies × de tien apps voor deze ideeën, met bronnen
  (aanvulling op `../nieuwe-functies/onderzoek.md`)
- `beoordelingen.md` — het oordeel van de criticus per idee, met voorwaarden
- `screens.mjs` — de telefoonschermen (verzonnen namen en cijfers); leent de
  bouwstenen van `../nieuwe-functies/screens.mjs`
- `preview.mjs` → `preview.html` — alle schermen naast elkaar
- `verdicts.mjs` + `build.mjs` → `functies.html` — de pagina met alles

```
node design/nieuwe-functies-2/preview.mjs
node design/nieuwe-functies-2/build.mjs
```

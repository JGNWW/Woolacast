# Kanalen — vijf concepten voor makers

Gevraagd: een kanaalachtige functie, om podcasts per bron te zien, geordend op
populair, recent en dergelijke. In de app heet zo'n bron een **Maker**, net als
het bestaande `MakerScreen`. *Bron* blijft Apple of Spotify.

| Concept | Idee |
| --- | --- |
| 1 · Makerpagina | Het makerscherm met logo, kleur, volgen en de sortering Populair · Recent · A–Z |
| 2 · Makers in de hitlijst | Hitlijsten → Podcasts → *Per maker*: wie heeft de meeste shows in de lijst |
| 3 · Apple tegenover Spotify | Hellingsgrafiek van de makersranglijsten van beide bronnen |
| 4 · Makers volgen | Bibliotheek → *Makers*: nieuwe podcasts en tellingen van gevolgde makers |
| 5 · Tips per medium | Tips-scherm per medium: sorteren, en een brug naar de eigen podcasts van dat medium |

Alle vijf zijn goedgekeurd door een kritische agent. Het verloop staat in
`beoordelingen.md`.

## Bestanden
- `onderzoek.md` — hoe andere apps het doen, en wat de data toelaat (gemeten)
- `concepten.md` — de specificatie per concept
- `beoordelingen.md` — het oordeel van de criticus per ronde
- `screens.mjs` — de telefoonschermen (verzonnen namen)
- `preview.mjs` → `preview.html` — de schermen naast elkaar
- `build.mjs` + `verdicts.mjs` → `kanalen.html` — de conceptpagina

```
node design/kanalen/preview.mjs
node design/kanalen/build.mjs
```

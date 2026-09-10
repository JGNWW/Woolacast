# Woolacast — UI-mockup

Bronbestanden van de mockup. De schermen zijn Design Components (`.dc.html`),
samengesteld tot één canvas dat als Artifact wordt gepubliceerd.

## Structuur

- `base.css` — tokens (licht) en gedeelde componenten
- `dark.css` — token-overschrijving voor het donkere scherm
- `parts/<Naam>.body.html` — de markup per scherm, met een `<!--dc {...} -->`-regel
  bovenaan voor extra CSS, logica en frame-formaat
- `parts/*.css` — CSS die maar op één scherm nodig is
- `parts/main.logic.js` — data en gedrag van het interactieve hitlijsten-scherm
- `parts/canvas.json` — plaatsing van de artboards op het canvas
- `assemble.mjs` — bouwt `parts/` + `base.css` tot `<Naam>.dc.html`

## Opnieuw bouwen

```
node design/assemble.mjs
```

Daarna het canvas opnieuw samenstellen met de `seed-canvas.mjs` uit de
design-skill en publiceren naar hetzelfde artifact.

## Schermen

| Artboard | Scherm |
| --- | --- |
| `Main` | Hitlijsten — bronchips en Podcasts/Afleveringen werken echt |
| `ChartsEpisodes` | Top afleveringen, met bron/land/categorie toegepast |
| `Filters` | Bottom sheet: bron, land, categorie |
| `PodcastDetail` | Podcastpagina met noteringen en volgen |
| `Tracker` | Noteringen van één show over bronnen, landen en tijd |
| `NowPlaying` | Speler |
| `Discover` | Ontdek: stijgers, landen, categorieën |
| `Library` | Bibliotheek: gevolgd, wachtrij, chart-alerts |
| `ChartsDark` | Hitlijsten in donkere modus |
| `System` | Kleur, typografie, componenten, iconen |

## Aannames

Wat de bronnen publiek publiceren verschilt, en dat stuurt de filters:

- **Apple Podcasts** — shows én afleveringen, ~175 landen, volledige categorieboom
- **Spotify** — top 200 shows per land; afleveringen en categorielijsten alleen
  in geselecteerde landen; dagelijks
- **YouTube** — alleen shows, wekelijks (woensdag), circa 38 landen

Alle podcasts, afleveringen en noteringen in de mockup zijn verzonnen.

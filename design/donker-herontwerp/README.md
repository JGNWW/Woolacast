# Donkere modus — vijf herontwerpconcepten

Aanleiding: de huidige donkere modus (`NightBg #15120E`, `NightSurface #1E1A15` …)
leest als bruin. Gevraagd: een donkerdere, neutralere donkere modus en/of
elementen die meekleuren met de hoes van de podcast.

Elk concept is als twee artboards uitgewerkt: de podcastpagina en het
hitlijsten-startscherm. Tik op de hoes (of op het hoesje in de mini-speler) om
te wisselen tussen drie testhoezen: rood, blauwgroen en amber. Zo zie je hoe de
UI meekleurt. Alle podcasts en hoezen zijn verzonnen.

| Concept | Idee | Wat kleurt mee |
| --- | --- | --- |
| A — Grafiet | huidige indeling, neutraal grafiet (`#121316`), ember blijft het enige accent | niets |
| B — Hoesgloed | kleurgloed uit de hoes in de kop; primaire actie en voortgang in de hoeskleur | kop, primaire actie, mini-speler |
| C — Tonaal | Material 3 content-based color: het schema komt uit de hoes, maar met oppervlakken van zeer lage chroma | accentrollen van de hele pagina |
| D — Matglas | vervaagde hoes als kop, balken van matglas, primaire actie neutraal wit | kop (via de hoes zelf), mini-speler |
| E — Vol beeld | hoes over de volle breedte, redactioneel en monochroom | één afspeelknop en de voortgang |
| F — Beeldgloed | E + B: hoes over de volle breedte, waarvan de kleur naar beneden weglekt tot in de lijst | gloed, afspeelknop, ringen, voortgang, mini-speler |

## Werkwijze

1. Research online naar goed ontwerp voor de donkere modus. Het toetsingskader
   staat in `principles.md`.
2. Elk concept is beoordeeld door een aparte kritische agent. Die toetste op
   P1–P8, rekende het WCAG-contrast na en lette vooral op visuele consistentie.
3. Een concept kwam pas in deze set nadat de criticus het had goedgekeurd.
   Afgekeurde versies zijn herzien en opnieuw voorgelegd. Het verloop per
   concept staat in `beoordelingen.md`.

## Bestanden

- `*.dc.html` — de artboards (Design Components, zelfde formaat als de rest van `design/`)
- `canvas.json` — de indeling van het canvas
- `principles.md` — de ontwerpprincipes met bronnen
- `beoordelingen.md` — het oordeel van de criticus per concept

# Woolacast — app-icoon

De pad met koptelefoon, nagetekend als vector naar de referentie van de
opdrachtgever, en uitgewerkt naar de Android-assets. Richting B en C staan
er nog als niet-gekozen alternatieven.

## Structuur

- `parts/mascot.svg` — de volledige tekening (192 px en groter)
- `parts/mascot-s.svg` — vereenvoudigd, houdt vorm tot 48 px
- `parts/mono.svg` — monochrome laag voor thema-iconen (Android 13+)
- `parts/notif.svg` — notificatie-icoon, 24 dp
- `parts/markB.svg`, `parts/markC.svg` — richting B en C
- `parts/<Naam>.body.html` — de borden op het canvas
- `assemble.mjs` — bouwt `parts/` + `base.css` tot `<Naam>.dc.html`

De tekeningen kleuren via CSS-variabelen (`--m-body`, `--m-phone`, …), dus
één bestand bedient alle varianten.

## Opnieuw bouwen

```
node design/icon/assemble.mjs
```

## Android-assets

| Asset | Spec |
| --- | --- |
| Adaptive icon | twee lagen van 108 × 108 dp; masker toont hooguit 72 dp; alleen een cirkel van 66 dp is gegarandeerd zichtbaar |
| Monochroom | één laag, voor thema-iconen vanaf Android 13 |
| Play Store | 512 × 512 px, 32-bits PNG, geen transparantie |
| Notificatie | 24 dp, één kleur wit, transparante achtergrond |

## Kleur

| Laag | Variabele | Waarde |
| --- | --- | --- |
| Kop | `--m-body` | `#C9946E` |
| Onderkaak | `--m-body2` | `#E7C9A9` |
| Vlekken, wenkbrauwen | `--m-wart` | `#B27E5B` |
| Mond, neusgaten | `--m-line` | `#96603E` |
| Iris | `--m-iris` | `#E8B84B` |
| Pupil | `--m-pupil` | `#4A3324` |
| Oorschelp | `--m-cup` | `#9AC2E7` |
| Binnenrand schelp | `--m-cup2` | `#86B0DA` |
| Oorkussen | `--m-pad` | `#F7EFE2` |
| Beugel | `--m-band` | `#A9CCBE` |
| Glans beugel | `--m-band2` | `#C6DFD4` |

Grond: `#1B1712` (inkt). Ember valt af — de tan van de kop loopt daarin
over. Alternatieven: `#2C4A46` (diep groen) en `#CFE2D9` (salie).

## Merkrechten

Het icoon gebruikt de Android-robot niet. Google staat reproductie en
modificatie van de robot toe onder CC BY 3.0, maar sluit merkrechten op de
robot en afgeleiden daarvan uit — en een app-icoon is precies zo'n
merkgebruik. De pad en de koptelefoon zijn eigen werk.

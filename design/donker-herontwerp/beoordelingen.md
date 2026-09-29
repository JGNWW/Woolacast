# Beoordelingen door de criticus

Elk concept had een eigen kritische agent. Die toetste het concept aan
`principles.md` (P1–P8), rekende het WCAG-contrast na in python en lette vooral
op visuele consistentie: tussen de twee schermen, tussen de drie testhoezen, en
tussen rollen, radii en typografie. Alleen een goedgekeurd concept mocht in de
set.

## A — Grafiet
- **Ronde 1: afgekeurd.**
  - De dalingskleur `#F08A74` lag bijna op ember `#F0895B` (ΔE 12,8). "▼1" en "NIEUW" leken daardoor dezelfde status.
  - Rangnummers waren ember op de podcastpagina en neutraal op Hitlijsten.
  - "=" en "= 0" werden door elkaar gebruikt.
- **Ronde 2: goedgekeurd.**
  - Dalen is nu `#FF7A85`.
  - "NIEUW" is een neutrale badge.
  - Rangcijfers en de notatie "= 0" zijn overal gelijk.
  - De navigatiepil is neutraal (was `#3A2419`, bruin).
  - Omlijning is `#63666F` (3,24:1).

## B — Hoesgloed
- **Ronde 1: afgekeurd.**
  - Het merk, dalen en het rode hoesaccent botsten.
  - De amberhoes bracht het bruin terug: mini-speler `#3A2A0B`, gloed `#6A4B10`.
- **Ronde 2: goedgekeurd.**
  - Dalen is nu `#FF7A93`.
  - De chroma van donkere vlakken is begrensd: amber-gloed `#4A3A20`, amber mini-speler `#2A2721`.
  - Rood en blauwgroen zijn na de beoordeling ook begrensd.
  - De tab-indicator is neutraal.
  - Typografie is 28/20/16/14/12.

## C — Tonaal
- **Ronde 1: afgekeurd.**
  - De primaryContainers op toon 30 (`#5A4300`, `#73332A`) lazen als mosterd en roest.
  - Amber-oppervlakken lagen op het oude palet.
  - Tab- en navigatie-indicator zagen er per scherm anders uit.
- **Ronde 2: goedgekeurd.**
  - Containers zijn nu neutraal `s3`, met het accent als voorgrond.
  - Warme hues hebben oppervlakken met C≤2.
  - Elke rol heeft één behandeling.
  - Tabs zijn een verbonden segmentgroep met 3px-voegen.
  - De mini-speler heeft één definitie.

## D — Matglas
- **Ronde 1: goedgekeurd.**
  - Leesbaarheid op de vervaagde kop is gemeten. Zelfs met een witte hoes halen de iconen op de glasknoppen ≥3,47:1.
  - Daarna verwerkt: de warme glastint is terug naar neutraal, de verzadiging is 1.1 in plaats van 1.35, tabs zijn 48px hoog en chips 40px.

## E — Vol beeld
- **Ronde 1: goedgekeurd.**
  - De scrim blijft leesbaar bij een lichte hoes: de metaregel haalt 6,4:1 bij amber.
  - Daarna verwerkt:
    - een steilere scrim en donkerdere kopknoppen tegen de olijftint;
    - een navigatie-indicator als streepje van 16×3px in plaats van een punt;
    - chips van 40px;
    - de outline als token.

## F — Beeldgloed (E + B, op verzoek)
De eisen van de gebruiker:
- de hoes blijft over de volle breedte staan (uit E);
- de accentkleuren komen uit de hoes (uit B);
- de kleur van de hoes lekt naar beneden weg en vervaagt;
- na ronde 2 kwam erbij: de gloed mag doorlopen tot in de afleveringslijst.

- **Ronde 1: afgekeurd.**
  - De vervaagde kopie overstemde de geklemde gloed, waardoor amber bruin werd (`#584011`).
  - De omlijning en de secundaire tekst op de tint haalden het contrast niet (2,13 en 3,86).
- **Ronde 2: afgekeurd.**
  - Het bruin en het contrast waren opgelost, maar er ontstond een harde naad op 700px.
- **Ronde 3: goedgekeurd.**
  - De gloed loopt naadloos en monotoon af tot in de lijst.
  - Alles haalt AA: omlijning `rgba(237,238,240,.5)`, secundaire tekst op de tint `.78`.
  - Amber leest als een geklemde warme gloed (C ≈ 0,02 aan het begin van de lijst), niet als het oude bruin.
- **Daarna verwerkt:** haarlijnen als `rgba(237,238,240,.08)`, zodat ze meegaan met de tint.

## Wat voor alle concepten gold
- Een neutrale basis tussen `#0E0F11` en `#131314`, in plaats van het warme `#15120E`.
- Dalen als roze-rood, ver van het merk-ember. Stijgen en dalen dragen altijd ook ▲/▼.
- Donkere vlakken die uit een hoes komen, krijgen een begrensde chroma. Anders wordt een warme hoes bruin.
- Tekstparen halen overal AA en UI-grafiek ≥3:1.

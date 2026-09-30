# Gebouwd: makerpagina, makers in de hitlijst, makers volgen

Concept 1, 2 en 4 uit `concepten.md` zijn gebouwd in de app. Een aparte
criticus toetste of de gebouwde schermen consistent zijn met de bestaande stijl
van de app. Dat deed hij op schermafdrukken van de echte Compose-schermen
(Robolectric, `MakerScreenshots`), naast de bestaande schermen, en in de code.
Een scherm was pas klaar als hij het CONSISTENT noemde.

| Scherm | R1 | R2 | R3 |
| --- | --- | --- | --- |
| Hitlijsten per maker + "Hoe tellen we?" | niet consistent | niet consistent | **consistent** |
| Makerpagina | niet consistent | **consistent** | consistent |
| Bibliotheek → Makers | niet consistent | niet consistent | **consistent** |
| Zoeken → Makers (in R2 toegevoegd) | — | **consistent** | consistent |

## Ronde 1

- **Gloed.** De gloed liep buiten Beeldgloed om. Nu levert de kanaalkleur een
  `CoverSeed`, en `coverColors`/`CoverBackdrop` maken de gloed, net als op
  Hitlijsten.
- **Knoppen.** Volgen en delen waren andere componenten dan op de podcastpagina.
  Nu zijn het `OutlinePillButton` ("Volg maker"/"Gevolgd") en
  `OutlineCircleButton`.
- **Kaart met nieuwe podcasts.** Die was een afwijkende kopie van AlertCard.
  Nu delen beide `PanelCard`/`PanelRow`, met chevron.
- **Hoesjesstapel.** De rand in achtergrondkleur gaf halo's op de gloed. Nu is
  het een uitsparing, in een vast vak.
- **Info-icoon.** Het was een uitroepteken. Dat is app-breed rechtgezet.
- **Woorden.** "shows" en "t.o.v." werden "podcasts" en "sinds gisteren".
  Daarnaast:
  - "Deze podcast" alleen vanaf de podcastpagina;
  - vanuit de Bibliotheek opent de makerpagina op Recent;
  - TalkBack-teksten.

## Ronde 2

- De regel boven de lijst sprong nog 4dp: `heightIn` stond vóór `padding`.
- "gevonden vandaag" bleef staan terwijl de rij dagen bleef. Nu wordt de
  vondstdatum onthouden.

## Ronde 3

Alles consistent. Het laatste advies, "beste plek" overal, is verwerkt.

## Bewust anders dan de spec

- **"Nog niet bekeken" boven de nieuwe podcasts, niet "Sinds gisteren".** Een
  rij blijft staan tot je hem opent. De criticus ging akkoord.
- **"beste #n" in plaats van "hoogste #n".** Zo past de regel naast de
  hoesjesstapel.

## Buiten dit werk: app-brede punten

- Randcontrast van `WoolButton`/`FilterChipBox` in licht.
- `RankNumber` compact: 16sp in `muted` haalt 3,06:1.
- Raakvlak van 48dp voor `SmallChip`, `FilterChipBox` en `IconAction`.

De nieuwe schermen gebruiken dezelfde componenten en erven deze punten.

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

## Muur (variant C) in plaats van rond logo

Het ronde logo is weg. Een maker krijgt het gezicht van zijn podcasts: op zijn
pagina een muur van hoezen, in lijsten een tegel. Beide schalen mee:

| Podcasts | Makerpagina | Tegel |
|---|---|---|
| 1 | die hoes, zoals op de podcastpagina | die hoes |
| 2 | duo, twee grote hoezen gekanteld | twee helften |
| 3–5 | muur, elke rij één hoes verder | 3: groot + twee klein; 4+: 2×2 |
| 6+ | muur, elke rij drie hoezen verder | 2×2 |

**Ronde 1: afgekeurd.** De pagina zelf was consistent: `CoverBackdrop`, de glazen
terugknop en de titel zijn dezelfde als op de podcastpagina. Afgekeurd om de
tegels:
- Dezelfde maker zag er per scherm anders uit, omdat elk scherm zijn eigen
  hoezen gebruikte.
- Een maker met twee podcasts, waarvan je er één volgt, stond als één losse
  hoes in de suggesties.
- De gloed kwam uit een hoes die grotendeels buiten beeld stond.

**Ronde 2: goedgekeurd.**
- Het gezicht van een maker is de eerste vier hoezen in Apple's volgorde. De
  app onthoudt het zodra ze zijn shows laadt, en elk scherm toont het.
  Suggesties halen het één keer op.
- De muur zet de eerste hoes midden achter de titel.

**Advies, verwerkt.** De verzamelaar zet de hoezen per kanaal in `makers.json`.
Zo klopt de tegel op Hitlijsten al bij de eerste weergave. Een maker zonder
kanaal toont daar zijn hoezen uit de lijst, tot de app zijn shows een keer
heeft geladen.

## Buiten dit werk: app-brede punten

- Randcontrast van `WoolButton`/`FilterChipBox` in licht.
- `RankNumber` compact: 16sp in `muted` haalt 3,06:1.
- Raakvlak van 48dp voor `SmallChip`, `FilterChipBox` en `IconAction`.

De nieuwe schermen gebruiken dezelfde componenten en erven deze punten.

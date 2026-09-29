# Ontwerpprincipes voor het donkere herontwerp (uit online research)

Bronnen: Material Design dark theme (m2.material.io/design/color/dark-theme.html),
Material 3 dynamic/content-based color (m3.material.io/styles/color/dynamic),
Apple HIG Dark Mode + Materials (developer.apple.com/design/human-interface-guidelines/dark-mode),
NN/g "5 Principles of Visual Design in UX" (nngroup.com/articles/principles-visual-design),
atmos.style / LogRocket / UX Design Institute dark-mode best practices,
Palette-API / Vibrant-extractie in muziekapps (Spotify, open-source spelers).

## P1 — Donkere basis: neutraal donkergrijs, niet zwart, niet getint
- Basisoppervlak rond #121212 (Material). Puur #000 vermijden: OLED-smearing bij
  scrollen en geen ruimte om diepte te tonen.
- Wie een tint gebruikt: chroma zeer laag houden (bijna neutraal). Een merkbare
  warme/bruine tint op grote vlakken leest als "vies" of "sepia" — precies de klacht
  van de gebruiker over het huidige ontwerp (#15120E, #1E1A15).

## P2 — Diepte via lichtere oppervlakken, niet via schaduw
- Hogere elevatie = lichter oppervlak (Material overlay; Apple base vs elevated).
- Maximaal 3–4 oppervlakteniveaus, elk duidelijk maar subtiel verschillend
  (ΔL ~3–5%). Schaduwen zijn op donker nauwelijks zichtbaar.

## P3 — Tekst: gebroken wit en contrast
- Hoofdtekst off-white (geen #FFF): minder halatie/"bloeden" voor mensen met astigmatisme.
- WCAG AA: 4.5:1 voor tekst < 24px, 3:1 voor grote tekst en UI-grafiek (iconen, randen van knoppen).
- Secundaire tekst moet op het HOOGSTE oppervlak nog 4.5:1 halen.

## P4 — Accentkleuren op donker: minder verzadigd, lichter
- Primaire kleuren desatureren/verlichten (Material "200-tint") zodat ze niet
  vibreren tegen donker en AA halen.
- Tekst óp een accentvlak: kies donker of licht per kleur, contrast afdwingen.

## P5 — Dynamische kleur uit de hoes (content-based color)
- Kleur uit de artwork halen (Palette-API: Vibrant / Muted / DarkMuted) en via
  een toonschaal (HCT) aan vaste rollen koppelen — niet de ruwe pixelkleur gebruiken.
- Luminantie van het accent klemmen, zodat zwarte, witte of neon-hoezen nooit
  onleesbare accenten opleveren. Fallback naar merkkleur bij grijze hoezen.
- Beperk waar kleur meegaat: één of twee rollen (kop-gloed, primaire actie,
  voortgang). Alles meekleuren = geen stabiel kader meer.

## P6 — Visuele consistentie (NN/g, Gestalt)
- Schaal: hooguit ~3 tekstgroottes per scherm; belangrijkste element het grootst.
- Hiërarchie: één duidelijk eerste aandachtspunt per scherm.
- Balans en contrast bewust inzetten; accent spaarzaam (60-30-10-idee).
- Gestalt: nabijheid en gelijkheid — gelijksoortige elementen (rijen, knoppen,
  chips) zien er overal hetzelfde uit; dezelfde rol = dezelfde kleur, op elk scherm.
- Consistent systeem: hoekradii, iconstijl (lijndikte), spatiëring (4/8-raster)
  herhalen zich; afwijkingen alleen met reden.

## P7 — Materialen / blur (Apple HIG)
- Doorschijnende materialen scheiden voor- en achtergrond; inhoud erop moet
  "vibrancy" krijgen (voldoende contrast), ook bij een lichte of drukke hoes.
- Blur alleen op dragende lagen (kop, balken), niet overal.

## P8 — Toegankelijkheid en interactie
- Raakvlakken ≥ 44–48 px. Kleur nooit als enige drager van betekenis
  (stijgen/dalen ook met pijl of teken). Kleuren die onderscheiden moeten worden
  verschillen ook in helderheid.

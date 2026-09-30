# Bouwbeoordeling door de designer

Getoetst: de 24 schermafbeeldingen in `schermen/`, de Compose-code (stand `7b21984a`), de
goedgekeurde mockups (`screens.mjs`) en de voorwaarden in `beoordelingen.md`. Contrast is
nagerekend met de WCAG-formule op de tokens uit `ui/theme/Color.kt`.

## Ronde 1

Uitslag: **alle vier afgekeurd**. De basis is goed. De voorwaarden uit de ideeënronde zijn
nageleefd: N=1 als standaard, bewaard en wachtrij blijven staan, zonder hoofdstukken geen
spoor, vorige/volgende ongemoeid, Tekst alleen met een transcript, de melding over shows die
alleen op Spotify staan, en het intent-filter. Wat de goedkeuring tegenhoudt is vooral één
thema-gat en een handvol teksten en toestanden.

**Voor alle functies (blokkerend)**

- A. `Theme.kt`: `LightScheme` definieert `surfaceContainerHighest`, `secondaryContainer` en
  `tertiaryContainer` niet, en `DarkScheme` mist de laatste twee. Material 3 (1.3.1) valt dan
  terug op zijn standaard paars en roze. Dat raakt de schakelaar, de voortgangsbalk en de
  zoekmarkering hieronder. Definieer deze rollen met de eigen tokens.
- B. `bibliotheek-gedownload-donker.png` is onbruikbaar. De test rendert zonder achtergrond
  en zonder `LocalContentColor`, waardoor titels onzichtbaar zijn en er zwarte tekst op een
  donkere kaart staat. In de app vangt de `Scaffold` dit waarschijnlijk op, maar het is niet
  aangetoond. Render het scherm opnieuw in een `Surface`. Voeg ook donkere opnames toe van
  Je shows, de dialoog, het importscherm, het automatisch-downloadenblad en zoeken in de tekst.

### 1. Downloaden — AFGEKEURD

Blokkerend:

1. `SettingSwitch` (DownloadsTab.kt): de schakelaar in de stand *uit* is onzichtbaar. De duim
   en de rand zijn `outline` (#E4DACA) op een spoor van #E6E0E9: 1,07:1, en het spoor tegen
   het blad is 1,28:1. In het donker is het 1,13:1. *Uit* is de standaard van automatisch
   downloaden, dus iedereen ziet dit als eerste. Geef de stand uit expliciete kleuren
   (`onSurfaceVariant` voor duim en rand, `surfaceContainerHigh` voor het spoor), zie ook A.
2. Foutteksten bij downloads: `DownloadWorker` toont `error.message` rauw. Dat worden Engelse
   OkHttp-teksten ("Unable to resolve host…") of vaktaal ("De server gaf 404."). Vertaal ze
   net als `ShowImporter.reason()`, bijvoorbeeld "Aflevering niet gevonden (404)" of "Geen
   verbinding".
3. Een mislukte rij in Gedownload is alleen opnieuw te proberen via het waarschuwingsdriehoekje.
   Niemand ziet dat als knop. Het importscherm heeft voor hetzelfde geval een knop "Opnieuw".
   Gebruik die (of een herhaal-icoon) ook hier.
4. `EpisodeRow` (podcastpagina): *bezig* en *wacht op wifi* tonen hetzelfde pijltje ↓ dat
   elders "Downloaden" betekent, zonder voortgang. In de Bibliotheek is het een ring met 65%.
   Volgens de voorwaarde "per rij bezig/klaar/mislukt" moet dat hier een kleine ring zijn.

Suggesties: de balk "Zelf gedownload" is `muted` en haalt 2,5:1 op het spoor; de mockup
gebruikte `ink2`. De wachtring heeft een spoor van 1,1:1. "Gedownload · tik om te verwijderen"
staat in accentkleur met een vinkje, terwijl het een verwijderactie is. Maak er "Download
verwijderen" van, met ongedaan maken in een snackbar, ook voor de ✕ in de lijst. De chips
1/2/3/5 zijn 36×36 dp; geef ze minimaal 48 dp breedte. Het automatisch-downloadenblad en het
afleveringsblad gebruiken een andere bladkleur dan de andere bladen. De zin "Met tekst om mee
te lezen van de maker, in de speler." loopt niet; beter is "Met meeleestekst van de maker".
De scrollende tabbladen zijn prima, maar scroll het gekozen tabblad in beeld.

### 2. Hoofdstukken — AFGEKEURD

Blokkerend:

1. `ChapterList`: de titels van voorbije hoofdstukken en alle tijden staan in `muted` (#988C7A).
   Op het blad is dat 3,25:1, onder AA voor tekst van 13–15,5 sp. De mockup gebruikte `ink2`
   (5,7:1). Gebruik `onSurfaceVariant`.
2. Slaaptimer "Einde hoofdstuk" legt een vaste positie vast (`AtPosition`). Spring je daarna
   +30 s of kies je een later hoofdstuk, dan pauzeert de speler meteen (`positionMs >= stopAt`).
   Ga je terug, dan stopt hij niet aan het eind van het hoofdstuk waar je dan bent. Onthoud
   daarom "einde van het huidige hoofdstuk" als modus en reken het stoppunt na elke sprong
   opnieuw uit.
3. TalkBack: welk hoofdstuk nu speelt zie je alleen aan vet en het equalizertje. Geef de rij
   `selected` of `stateDescription = "Speelt nu"`.

Suggesties: TalkBack leest de knop nu voor als "3/5 …". Geef hem het label "Hoofdstuk 3 van 5,
Live: Stadslicht met band, nog 8 minuten 57", en maak hem 48 dp hoog in plaats van 44. Het
gereedschapslabel "Einde hfst." wordt voorgelezen als afkorting; geef het een volledige
omschrijving. Scroll een lange lijst naar het huidige hoofdstuk. Noem het aantal hoofdstukken
in de semantiek van de balk.

### 3. Meelezen — AFGEKEURD

Blokkerend:

1. `highlight()` gebruikt `tertiaryContainer`, en die rol bestaat niet in het thema. De
   treffers worden daardoor Material-roze (#FFD8E4), en in het donker mauve (#633B48). Dat is
   buiten het palet; de mockup gebruikte amber (#F6D9A8). Zie A.

Suggesties: de gekozen treffer verschilt alleen door een onderstreping. Maak hem sterker,
zeker binnen de oplichtende zin. Verander "Geen" in "Geen treffers". Maak "Terug naar nu"
48 dp hoog, met `Role.Button`. Tekst in de plaats van Delen in de gereedschapsrij is logisch,
want delen staat ook in het ⋮-menu. De versie zonder tijden en de laad- en fouttoestand zijn goed.

### 4. OPML — AFGEKEURD

Blokkerend:

1. `ShowsSheet`: er staat "1 shows · ook je back-up" (zie `je-shows.png`). Het meervoud klopt
   niet.
2. `ImportScreen`: de voortgangsbalk voor het koppelen heeft een lila spoor (#E8DEF8) en het
   stopstipje van M3 1.3. Dat is buiten het palet. Zet `trackColor` op `surfaceContainerHigh`
   en laat het stipje weg, zie A.
3. `AddFeedDialog`: de rand van het tekstveld is `outline` op `surfaceContainerHigh`: 1,05:1.
   Zonder focus zie je het veld niet (`feed-toevoegen.png`). Gebruik `onSurfaceVariant` als
   rand, en de dialoogkleur die bij de bladen past.

Suggesties: in een fout-rij valt de reden weg ("Feed best…"), terwijl die het belangrijkst is.
Zet de reden op een eigen regel. Een lange bestandsnaam duwt "5 feeds" uit de kop. Geef
"Opnieuw" een bezig-toestand. De titel van een uitgeschakelde export staat in `outline`
(≈1,3:1); gebruik `onSurface` op 38%. Het TalkBack-label "Shows importeren of exporteren" mist
"feed toevoegen".

## Ronde 2

Getoetst op `29ec24e6`: de 29 nieuwe schermafbeeldingen (nu in een `Surface`, met donkere
versies) en de gewijzigde code. Uitslag: **1, 2 en 4 goedgekeurd, 3 afgekeurd** op één punt.

A en B zijn opgelost. De thema-rollen zijn ingevuld, er is geen paars of roze meer te zien, en
de donkere schermen zijn nu bruikbaar en kloppen met de app.

### 1. Downloaden — GOEDGEKEURD

Alle vier de punten zijn opgelost.

- De schakelaar in de stand uit haalt nu 4,4:1 in licht (duim tegen spoor) en 5,8:1 in donker.
- De foutreden staat in gewone taal op een eigen regel, met een knop "Opnieuw" en een ✕.
- Op de podcastpagina staan nu dezelfde tekens als op de knop: een ring, een stippelring,
  een vinkje en een waarschuwing.
- De chips zijn 48 dp. De bladen hebben dezelfde kleur. "Download verwijderen" is nu een
  gewone actie.

Suggesties:

- Een mislukte download staat nog onder de kop "Bezig". Maak er "Bezig of mislukt" van, of
  geef mislukte downloads een eigen groep.
- Verwijderen (✕ en in het blad) kan nog steeds niet ongedaan worden gemaakt. Een snackbar
  "Ongedaan maken" zou helpen.

### 2. Hoofdstukken — GOEDGEKEURD

Alle drie de punten zijn opgelost.

- De tijden en de voorbije titels staan in `onSurfaceVariant`: 5,7:1.
- `SleepTimer.EndOfChapter` rekent na elke sprong in de app het einde van het huidige
  hoofdstuk opnieuw uit.
- De rij is `selectable` met "Speelt nu".
- De knop is 48 dp en heeft een volledig TalkBack-label. De Timer leest nu "Slaaptimer: einde
  van dit hoofdstuk".

Suggestie:

- `afterSeek()` draait alleen bij sprongen via de app zelf. Springt iemand via de melding,
  een koptelefoon of Android Auto voorbij het stoppunt, dan pauzeert de speler nog steeds
  meteen. Reken het stoppunt daarom ook opnieuw uit in `onPositionDiscontinuity`.

### 3. Meelezen — AFGEKEURD

Blokkerend:

1. `highlight()` in TranscriptSheet.kt geeft de treffer alleen een achtergrond. De tekst houdt
   de kleur van de regel (`onSurfaceVariant`). Het contrast op 17 sp:

   | Treffer | Licht | Donker |
   |---|---|---|
   | Gewone treffer | Ink2 op #F6D9A8: 4,25:1 | NightInk2 op #4A3A1C: 4,35:1 |
   | Gekozen treffer | op #E9B45C: 3,07:1 | op #7A5A1E: 2,51:1 |

   Alle vier halen de norm van 4,5:1 niet. Juist het woord dat je zoekt wordt daardoor
   slechter leesbaar. De mockup gebruikte `ink` op de markering. Oplossing: geef de span
   `color = onSurface`. Dan wordt het:

   | Treffer | Licht | Donker |
   |---|---|---|
   | Gewone treffer | 12,3:1 | 9,5:1 |
   | Gekozen treffer | 8,9:1 | 5,5:1 |

Verder is alles goed:

- De amberkleur past bij het palet.
- De gekozen treffer valt op.
- "Geen treffers" en een "Terug naar nu" van 48 dp met `Role.Button` zijn doorgevoerd.

### 4. OPML — GOEDGEKEURD

Alle drie de punten zijn opgelost.

- Er staat nu "1 show".
- De voortgangsbalk heeft een eigen spoor, zonder stopstipje.
- De rand van het veld haalt 5,7:1, en de dialoog heeft de kleur van de bladen.

Ook de suggesties zijn goed doorgevoerd: de reden staat op een eigen regel, "5 feeds" blijft
in beeld, de uitgeschakelde export staat op 38% en het TalkBack-label is compleet. In het
donker is alles in orde.

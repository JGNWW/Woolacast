# Beoordelingen door de criticus — tweede reeks

Meetlat: `../nieuwe-functies/onderzoek.md`, plus de aanvulling
`onderzoek-aanvulling.md` in deze map. Per idee de vragen:

- Gebruiken of missen luisteraars van de top-10-apps dit echt? Welke apps hebben het?
- Kan het zonder eigen server en zonder account?
- Dupliceert het iets wat Toadcast al heeft, of wat in de eerste reeks al is
  goedgekeurd?
- Voegt het iets toe, in verhouding tot de moeite?

**Stand na drie rondes:** 10 goedgekeurd (1, 2, 3, 4, 5, 7, 8, 9, 11, 13), waarvan
2 en 7 in kleinere vorm. 3 afgekeurd (6, 10, 12).

**Voorgestelde bouwvolgorde:** 1, 3, 11, 13, 4, 9, 5, 8, 2, 7.

## Ronde 1

Uitslag: **8 goedgekeurd** (1, 2, 3, 4, 5, 7, 8, 9), waarvan 2 en 7 in kleinere
vorm. **2 afgekeurd** (6, 10).

Volgorde na ronde 1: 1, 3, 4, 9, 5, 8, 2, 7. De volgorde bovenaan telt.

Geldt voor alle goedgekeurde ideeën:

- **Geen nieuwe tabbladen in de Bibliotheek.** Daar staan er al vijf: Gevolgd,
  Makers, Wachtrij, Bewaard, Gedownload. Idee 1, 2 en 4 willen er elk één bij. Dat
  worden er acht. Zie de voorwaarden per idee.
- De speler is nog nooit hoorbaar getest op een toestel (README). Alles wat aan de
  speler zit (2, 3, 8, 9) wacht daarop.

### 1. Nieuw-inbox met meldingen bij een nieuwe aflevering — GOEDGEKEURD

**Waarom:**

- Een melding bij een nieuwe aflevering is tafelstakes. Spotify, Apple, Castbox,
  Overcast, Pocket Casts, Podcast Addict, Podbean en AntennaPod hebben haar. YouTube
  Music niet, en dat werd daar een bekende klacht.
- Een samengevoegde lijst hebben YouTube Music ("New episodes"), Spotify (de
  Following-feed, 2025), Castbox, Overcast, Pocket Casts, Podcast Addict en
  AntennaPod (Inbox).
- Het is precies de herkansing die ik in de eerste reeks vroeg (idee 9): drie vaste
  chips en "alles in de wachtrij", zonder regelbouwer.
- In de eerste reeks stond al dat de melding eerst gebouwd moest worden (idee 12).
  Toadcast meldt nu alleen nieuwe shows van makers en downloads.
- Zonder server: feeds lezen op het toestel.

**Voorwaarden:**

- **Eén feedronde, geen tweede worker.** Er draait al een `AutoDownloadWorker` die
  elke 6 uur de feeds leest. Maak daar één ronde van voor automatisch downloaden,
  de Nieuw-lijst en de meldingen. Geen aparte taak van 4 uur.
- **Voorwaardelijk ophalen bestaat nog niet.** `FeedClient` stuurt geen
  `If-None-Match` of `If-Modified-Since`. Bewaar `ETag` en `Last-Modified` per feed
  in `LocalStore`. Zonder die koppen haal je de feed gewoon op.
- **Eén definitie van "nieuw".** Nu hangt "2 nieuw" af van `lastOpened`. De inbox
  mag geen ander getal tonen. Neem "verschenen na het volgen (`followedAt`, al
  gevraagd voor de Terugblik) en niet beluisterd of weggeveegd". Naar links vegen
  verbergt de aflevering; het markeert haar niet als beluisterd.
- **Geen eigen tabblad.** Nieuw komt bovenaan Gevolgd.
- **Meldingen:** een eigen kanaal "Nieuwe afleveringen". Vraag de toestemming
  (Android 13+) pas als iemand de eerste schakelaar aanzet. Shows zonder RSS
  (alleen Spotify) krijgen geen schakelaar. "Alles in de wachtrij" voegt alleen toe
  wat er nog niet in staat, met een grens (bijvoorbeeld 20).

### 2. Bladwijzers en clips — GOEDGEKEURD (zonder clips)

**Waarom:**

- Bladwijzers hebben YouTube, Overcast, Pocket Casts en Podcast Addict. Spotify en
  Apple doen het deels. Pocket Casts vraagt er geld voor en bouwt er nog aan
  ("Smart Bookmarks", vegen om te delen in 8.22). Dat wijst op echt gebruik.
  AntennaPod mist het.
- Klein en volledig lokaal: een tijdstip en een notitie per aflevering.
- **Clips keur ik af.** Overcast heeft ze sinds 2019. Pocket Casts voegde ze toe in
  september 2024 en haalde ze in januari 2025 weer weg, omdat de bibliotheek
  eronder (FFmpegKit) ophield. Het is dus breekbaar. Knippen met Media3 Transformer
  vraagt de audio op het toestel en opnieuw coderen. En je deelt een stuk audio van
  een ander als bestand.
- **De link met tijdstip landt nergens.** Toadcast heeft geen webpagina. De link
  in de speler is nu `episode.link` (de site van de maker) of de MP3-URL. Geen van
  beide kent een tijdstip, en een Apple-afleverings-ID heeft Toadcast voor
  RSS-afleveringen niet.

**Voorwaarden:**

- Clips eruit. Delen wordt de bestaande deeltekst plus "vanaf 23:14". Beloof geen
  link die naar het tijdstip springt.
- Geen knop in de mediamelding. Die heeft er al vijf (snelheid, −15, afspelen, +30,
  bewaren), en Android toont er compact maar drie. De knop komt alleen in de speler.
- Een bladwijzer zetten is één tik, zonder dialoog. De notitie voeg je achteraf toe.
- Geen tabblad *Bladwijzers*. Toon ze onder de hoofdstukken van de aflevering, en
  als groep bovenaan *Bewaard*.
- Bewaar bij elke bladwijzer een `SavedEpisode`-kopie. Dan blijft hij werken als de
  aflevering uit de feed valt. Neem bladwijzers mee in de back-up (idee 7).

### 3. Casten naar Chromecast en slimme speakers — GOEDGEKEURD

**Waarom:**

- Tafelstakes volgens het eerste onderzoek. Alle tien apps casten. AntennaPod doet
  het alleen in de Play-versie, om dezelfde reden als hier.
- Geen server nodig: de speaker haalt de audio zelf bij de maker.
- Het splitsen in een Play- en een F-Droid-build volgt AntennaPod. Dat is een
  bekende, werkbare route.

**Voorwaarden:**

- **Eerst de speler op een echt toestel, dan Media3 bijwerken.** Toadcast zit op
  1.5.1. In de huidige Media3 (media3-cast 1.11.1) wikkelt `CastPlayer` de lokale
  ExoPlayer in (`setLocalPlayer`) en wisselt Media3 zelf via de Output Switcher.
  Bouw dat wisselen niet zelf.
- Een gedownloade aflevering cast je vanaf de URL uit de feed, niet vanaf het
  lokale bestand. Zonder internet is er geen cast-knop.
- Zeg wat op de speaker niet werkt: stilte inkorten en stemversterking (idee 4 uit
  de eerste reeks), en intro overslaan (idee 12). Slaaptimer, hoofdstukken,
  wachtrij en voortgang moeten wél werken. Test juist die.
- De cast-knop verschijnt alleen als er een Cast-apparaat in het netwerk is.

### 4. Luistergeschiedenis — GOEDGEKEURD

**Waarom:**

- Breed aanwezig: YouTube, Apple ("Recently Played"), Castbox, Pocket Casts (met
  zoeken, en per item wissen sinds 7.81), Podcast Addict, Podbean en AntennaPod.
  Spotify en Overcast doen het deels.
- De datalaag moet er toch komen. Bij de Terugblik (eerste reeks, idee 6) was de
  voorwaarde: begin meteen met geluisterde seconden per dag per show vastleggen.
  Dat is nog niet gebouwd. Toadcast onthoudt nu alleen welke afleveringen
  uitgeluisterd zijn (zonder tijd) en de laatste positie.
- Volledig lokaal, en de schakelaar om het uit te zetten is netjes.

**Voorwaarden:**

- **Eén log voor geschiedenis en Terugblik.** Per luistermoment: aflevering, show,
  datum en echt geluisterde seconden (afspeeltijd, niet het verschil in positie;
  spoelen telt niet mee). Twee aparte logs mogen niet.
- **Begrens de groei.** `LocalStore` is één JSON-bestand dat bij elke wijziging
  helemaal opnieuw geschreven wordt. Voeg per dag per aflevering samen, of verhuis
  nu naar Room (de README noemt dat al).
- Uitzetten en wissen gelden ook voor de Terugblik. Zeg dat bij de schakelaar.
- Geen tabblad. Maak de geschiedenis bereikbaar via een knop in de kop van de
  Bibliotheek.

### 5. Waarom stijgt dit? — de aanleiding bij een sprong — GOEDGEKEURD (als "rond deze sprong")

**Waarom:**

- Het vult Toadcasts eigen gat (les 5). Geen van de tien apps toont beweging door de
  tijd, laat staan wat erbij hoort. Dit maakt de chart-tracker en de chart-alerts
  begrijpelijk.
- De data is er al: de RSS van de show, de tips van de media met datum (13 landen),
  de historie per land uit de verzamelaar (`ChartsDataset.tracking`), Apple's
  "Nieuwe programma's" en de makers. Geen server erbij.
- Maar de titel belooft te veel. Dat iets tegelijk gebeurt, bewijst niet dat het de
  oorzaak is. En "nieuwe aflevering op maandag" is bij een weekshow bijna altijd
  waar. Dan zegt het niets.
- De eigen momentopnames bestaan alleen voor lijsten die je opende, en maar 30
  dagen. "Stond eerst hoog in de VS" kan daar niet op leunen.

**Voorwaarden:**

- Noem het niet "waarom". Kop: *Rond deze sprong*. Neutrale zinnen: "Op 28 sep
  getipt door NRC".
- Vaste vensters: een aanleiding telt alleen als ze hooguit 7 dagen vóór de sprong
  valt. Een nieuwe aflevering telt alleen als het de eerste is, een nieuw seizoen,
  of een terugkeer na een pauze van meer dan twee keer de gewone cadans.
- "Eerst hoog in een ander land" komt uit de historie van de verzamelaar, met een
  drempel (bijvoorbeeld top 20 daar, minstens 3 dagen eerder). Niet uit de eigen
  momentopnames.
- Per show hooguit één regel: de sterkste aanleiding. Op de grafiek hooguit een
  handvol stippen, met uitleg als je erop tikt.
- Meet eerst op een maand stijgers in NL en de VS hoe vaak er een aanleiding is.
  Is dat zelden (onder een kwart), toon de regel dan alleen in de chart-tracker en
  niet in de lijst.

### 6. Deelkaart van een notering — AFGEKEURD

**Waarom:**

- Luisteraars van de top 10 delen geen noteringen. Geen van de tien heeft zo'n
  kaart. Deelkaarten bestaan wel voor afleveringen en jaaroverzichten (Spotify,
  Pocket Casts, Overcast), maar dat is iets anders.
- De onderbouwing klopt niet. Chartable stopte op 12 december 2024, maar makers
  hebben gratis plekken: Apple's eigen lijsten, Podcharts en Ausha Charts, zonder
  account. Podstatus en Podchaser zijn betaald.
- Toadcast deelt de noteringen al als tekst vanuit de chart-tracker
  (`TrackerViewModel.shareText()`).
- De deelbare afbeelding is al goedgekeurd bij de Terugblik (Compose naar bitmap).
  Een tweede renderer met twee formaten en een minigrafiek is veel werk voor een
  kleine groep.
- "Elke kaart draagt de naam van de app" is reclame, geen luisterwaarde.

**Opnieuw indienen kan als:**

- één optie "als afbeelding" in de bestaande deelknop van de chart-tracker;
- op dezelfde renderer als de Terugblik-kaart, in één formaat;
- ná de Terugblik.

### 7. Volledige back-up en herstel — GOEDGEKEURD (alleen handmatig)

**Waarom:**

- Bij apps zonder account is dit wat zware luisteraars gebruiken en missen. Podcast
  Addict heeft een lokaal bestand, optioneel automatisch naar Google Drive.
  AntennaPod heeft database-export, en sinds 3.4 een automatische back-up. Daar
  komen nog steeds verzoeken voor binnen (#4850, #7564). Gesloten apps lossen het
  op met een account, en dat wil Toadcast niet.
- OPML bewaart alleen wat je volgt. Wachtrij, voortgang, bladwijzers en
  instellingen per show gaan verloren.
- **Maar het idee overschat het gat.** Android Auto Backup staat in Toadcast al aan
  (`allowBackup="true"`, downloads uitgesloten). Wie Google-back-up gebruikt, heeft
  het hele bestand dus al in Drive. En "de historie die niet terug te halen is"
  klopt niet: lokaal is dat 30 dagen, en de verzamelaar bewaart alles.
- Wat overblijft: wie zonder Google werkt (de F-Droid-build uit idee 3) en wie zelf
  wil verhuizen. Daarvoor volstaat handmatig.

**Voorwaarden:**

- Alleen handmatig exporteren en terugzetten, via het bestandsmenu. Geen wekelijkse
  ronde met vijf versies. Geen vraag bij de eerste start: zet "Back-up terugzetten"
  naast de bestaande OPML-import.
- Alleen wat van de gebruiker is: volgen, makers, wachtrij, bewaard, voortgang,
  beluisterd, bladwijzers, geschiedenis en instellingen (ook per show). Geen
  momentopnames en geen caches van lijsten of tips; die komen terug uit de
  verzamelaar.
- Een versienummer in het bestand. Terugzetten vervangt alles, met vooraf een
  samenvatting. Niet samenvoegen.
- Test eerst of Auto Backup werkt (`adb shell bmgr backupnow`), en noem het in
  Instellingen. Het blijft de standaard; het bestand is de uitweg.

### 8. Een slaaptimer die meedenkt — GOEDGEKEURD

**Waarom:**

- De slaaptimer zelf is tafelstakes en bestaat. Deze verfijningen gebruiken
  slaapluisteraars bij de open apps echt. Uitfaden: Overcast, Podcast Addict,
  AntennaPod, Pocket Casts. Schudden: Pocket Casts en AntennaPod. Een tijdvak:
  AntennaPod. Opnieuw starten: Pocket Casts. De bugmeldingen bij die apps laten
  zien dat mensen het gebruiken.
- Spotify sloot het idee "schudden" wegens te weinig stemmen. Het is dus geen wens
  van iedereen. Maar het is klein en lokaal.

**Voorwaarden:**

- Uitfaden staat standaard aan (20–30 seconden). Zet het volume daarna terug op
  100%, anders begint de volgende keer stil (die bug had Pocket Casts).
- Schudden werkt alleen terwijl de timer loopt. Luister pas in de laatste minuten
  en tijdens het uitfaden naar de sensor. Is er geen sensor, of is hij geweigerd,
  dan moet de timer gewoon werken (AntennaPod #8760).
- Spoel niet terug bij het stoppen, maar bij het **hervatten** na de slaaptimer. En
  niet bij "einde aflevering": daar is niets gemist.
- Het tijdvak geldt alleen als je zelf op afspelen drukt. Nooit in de auto (Android
  Auto, eerste reeks idee 11; zie AntennaPod #7052).
- Het menu blijft kort. Schudden, tijdvak en terugspoelen komen in Instellingen,
  niet in het slaaptimermenu.

### 9. Seriële podcasts: begin bij aflevering 1 — GOEDGEKEURD

**Waarom:**

- De twee grootste audio-apps doen het: Apple, en Spotify (volgorde "Serial" uit
  `itunes:type`). Pocket Casts, Podcast Addict en AntennaPod bieden seizoenen of
  sorteren per show.
- Voor een hitlijstenapp is het een echt gat. In de lijsten staan veel verhalende
  shows (true crime, fictie). "Afspelen" vanuit de lijst begint nu bij de nieuwste:
  midden in het verhaal. Dit repareert de kernroute van Toadcast.
- Klein: de parser leest nu geen `itunes:type`, `itunes:season`, `itunes:episode`
  of `itunes:episodeType`. Die erbij is weinig werk.

**Voorwaarden:**

- Lees ook `itunes:episodeType`. "Begin bij aflevering 1" slaat trailers en bonussen
  over.
- Sorteer op seizoen en `itunes:episode`, met de datum als terugval. Ontbreekt het
  begin in de feed (sommige feeds tonen maar een deel), zeg dan "oudste
  beschikbare", niet "aflevering 1".
- Doe hetzelfde bij afspelen van een show vanuit de hitlijst: bij een seriële show
  "begin bij 1" of "verder waar je was", niet de nieuwste. Hitlijstradio blijft per
  aflevering.
- Zet de volgende in de reeks alleen klaar als hij nog niet in de wachtrij staat,
  één tegelijk.
- Maak de volgorde per show om te zetten in de instellingen per show (eerste
  reeks, idee 12), voor makers die de tag verkeerd zetten.

### 10. Proefluisteren vanuit de hitlijst — AFGEKEURD

**Waarom:**

- Alleen Spotify heeft voorproefjes, en dan in de aanbevelingsfeed van Home, niet
  in een lijst. `podcast:soundbite` leest geen van de tien apps. Het staat in
  72.031 van de 4,7 mln. feeds (ongeveer 1,5%).
- De terugval "na minuut 5" is weer een vaste gok. Dat keurde ik in de eerste
  reeks af bij de proefmodus van de hitlijstradio: aan het begin zit vaak
  ingevoegde reclame, en die schuift per keer op.
- De bediening werkt niet. Op Android verwacht je bij lang drukken een menu. Een
  minuut je vinger op het scherm houden doet niemand.
- Technisch traag. Midden in een MP3 van een ander beginnen vraagt zoeken in een
  bestand op afstand; bij een MP3 zonder index is dat traag en onnauwkeurig. Bij
  showlijsten (Spotify) moet eerst de feed nog opgehaald worden.
- Elk fragment is een verzoek bij de host van de maker. Of het als download telt,
  is niet vastgesteld.
- Het overlapt met de goedgekeurde hitlijstradio, die ontdekken zonder moeite al
  dekt.

**Opnieuw indienen kan als:**

- het alleen een fragment speelt dat de maker zelf aanwijst: `podcast:soundbite`,
  of een trailer (`itunes:episodeType` trailer, die idee 9 gaat lezen);
- het een zichtbare knop is, geen lang drukken;
- eerst gemeten is hoeveel shows in de top 200 van NL en de VS zo'n fragment of
  trailer hebben. Is dat weinig, dan niet.

## Ronde 2

Uitslag: **11 goedgekeurd**, **12 afgekeurd**. Van de vervangers voor 6 en 10 haalt
er dus één het.

### 11. Spoelknoppen instellen en bediening met de koptelefoon — GOEDGEKEURD

**Waarom:**

- Bijna overal te vinden. Apple (10–60 s), Overcast (7–60 s), Pocket Casts, Podcast
  Addict (zelfs per show), AntennaPod, Castbox (5–30 s) en Podbean ("Seek by")
  hebben het. YouTube heeft dubbeltikken om te spoelen (5–60 s). Spotify zit vast
  op 15 seconden, en gebruikers vragen daar om meer.
- Ook de koptelefoon instellen is gewoon bij de podcastapps. Apple laat je kiezen
  tussen volgende/vorige en vooruit/terug. Overcast heeft "Remote Episode Skip",
  Pocket Casts "Headphone Controls", AntennaPod de keuze voor de vooruitknop.
- Weinig werk. De knoppen in de app tekenen het getal al als tekst bij een pijl
  zonder getal. De melding kiest haar icoon al per waarde: 5, 10, 15 en 30 met
  getal, anders een kale pijl.
- Zonder server, zonder account.
- **Het idee klopt op twee punten niet met de code:**
  - "Standaard zoals Android (volgende/vorige aflevering)" bestaat in Toadcast niet.
    `PlaybackService` haalt `COMMAND_SEEK_TO_NEXT` en `…_PREVIOUS` weg voor
    alle bedieningen. De speler bevat één aflevering; de wachtrij zit in
    `PlayerController`. Volgende op de koptelefoon doet nu dus waarschijnlijk niets.
  - ExoPlayer legt de spoelstappen vast in de Builder
    (`setSeekBackIncrementMs` / `setSeekForwardIncrementMs`). Achteraf wijzigen kan
    niet zonder de speler opnieuw te bouwen.

**Voorwaarden:**

- **Standaard spoelen.** Volgende en vorige op de koptelefoon spoelen standaard met
  de gekozen stappen, zoals Overcast en Pocket Casts. "Volgende in de wachtrij" is de
  optie. Die moet de service dan doorgeven aan `PlayerController.next()`, want de
  Media3-speler kent de wachtrij niet.
- Laat de stappen tijdens het afspelen meeveranderen met een `ForwardingPlayer` die
  `getSeekBackIncrement`, `seekBack` en de twee voor vooruit overschrijft. Zo zien
  de melding, het vergrendelscherm en straks de auto hetzelfde getal. Geen nieuwe
  speler bouwen bij elke wijziging.
- Verwerk de toetsen die koptelefoons echt sturen. Bluetooth stuurt meestal
  `KEYCODE_MEDIA_NEXT` en `…_PREVIOUS`; dubbel en driedubbel tikken doet de
  koptelefoon zelf. Bij een bedrade koptelefoon is het `KEYCODE_HEADSETHOOK`. Test
  beide op een toestel.
- De keuze geldt alleen voor de koptelefoon. De knoppen van Android Auto en het
  scherm veranderen niet van betekenis; AntennaPod kreeg daar klachten over
  (#2551).
- Eén algemene instelling. Spoelstappen per show komen er pas bij als de
  instellingen per show (eerste reeks, idee 12) er zijn, en alleen als er om
  gevraagd wordt.

### 12. Apple en Spotify naast elkaar — AFGEKEURD

**Waarom:**

- Geen luisteraar van de top 10 mist dit aantoonbaar. Elke app heeft één eigen
  lijst. Wie vergelijkt, zijn makers en analisten, en die hebben al Podchaser,
  Podfollow, Ausha Charts (gratis) en Podstatus.
- De onderbouwing is te stellig. Apple zegt zelf luisteren, volgen en uitluisteren
  te meten. Dat vooral nieuwe volgers tellen, is een conclusie van Podnews (2025),
  geen gegeven. Spotify meet het wekelijkse unieke publiek. Dat "alleen Spotify"
  vaak videoshows zijn: niet vastgesteld. En exclusieve Spotify-shows staan per
  definitie niet bij Apple; dat leert je niets.
- De lijsten passen slecht op elkaar. Spotify heeft per categorie een top 50, en
  alleen in een deel van de landen. Apple heeft een top 200. Spotify mist
  categorieën als *Kinderen & gezin* en *Overheid*, en heeft afleveringen niet per
  categorie. "14 van de top 50" vergelijkt dus vaak ongelijke lijsten.
- De koppelregel is ongeschikt. De podcastpagina zoekt een Spotify-show op door per
  titel de iTunes-zoekdienst te vragen en het eerste resultaat te nemen. Dat is
  één netwerkverzoek per show, vijftig voor één scherm. Er komen ook verkeerde
  koppelingen uit bij dezelfde titel. Het commentaar in `PodcastRepository` zegt dat zelf ook.
- Per show staan Apple en Spotify al naast elkaar: op de podcastpagina en in de
  chart-tracker. Een heel scherm erbij voegt weinig toe.

**Opnieuw indienen kan als:**

- het geen apart scherm is, maar een klein label in de bestaande lijst: bij een
  Apple-notering "ook #11 bij Spotify", en andersom;
- de koppeling uit de verzamelaar komt (`ShowRecord.u`, de Spotify-uri die
  `charts-service` al op naam koppelt), zonder zoekverzoeken vanuit de app;
- het alleen verschijnt waar beide bronnen dezelfde lijst hebben (alle categorieën,
  of een categorie die Spotify in dat land kent);
- de uitleg neutraal is ("Apple en Spotify tellen anders"), zonder beweringen over
  video of volgers.

## Ronde 3

Uitslag: **13 goedgekeurd**. Het vervangt het afgekeurde idee 12.

### 13. Zoeken in de afleveringen van één show — GOEDGEKEURD

**Waarom:**

- Luisteraars gebruiken en missen dit aantoonbaar.
  - Apple heeft een zoekveld onder "Alle afleveringen".
  - Spotify bouwde "Find in this show" na een lang verzoek in de Community.
  - Overcast zoekt ook in de shownotes; gebruikers noemen dat in recensies een
    topfunctie.
  - Pocket Casts zoekt in titels en shownotes.
  - Castbox heeft een vergrootglas op de kanaalpagina.
  - AntennaPod zoekt binnen een podcast in titels en omschrijvingen.
  - YouTube zoekt binnen een kanaal. Podcast Addict heeft per show een filter op
    trefwoorden.
- Het bestaat nog niet in Toadcast. De podcastpagina toont 20 afleveringen met
  "Toon meer afleveringen" (`EPISODES_AT_FIRST`), zonder zoeken. Het zoekscherm
  vraagt de iTunes-catalogus (`SearchRepository`), niet één feed.
- Goedkoop en volledig lokaal. `PodcastRepository.fromFeed` houdt alle afleveringen
  uit de feed vast, mét omschrijving, en `Html.toPlainText` bestaat.
- Het helpt juist de shows uit de hitlijsten. Daar staan veel lange, dagelijkse of
  wekelijkse shows met honderden afleveringen.
- **Eén bewering klopt niet.** "Sorteren nieuwste of oudste eerst blijft werken":
  de podcastpagina heeft geen sorteerknop. Die volgorde komt pas met idee 9
  (seriële shows en volgorde per show).

**Voorwaarden:**

- Zoek in **alle** afleveringen uit de feed, niet alleen in de 20 die zichtbaar zijn.
  Toon de treffers in de volgorde die de pagina heeft. Bouw voor dit idee geen
  eigen sorteerknop; volg idee 9.
- Maak de doorzoekbare tekst één keer klaar, buiten de hoofdthread: HTML naar platte
  tekst, kleine letters, accenten weg (`java.text.Normalizer`). Niet bij elke
  toetsaanslag honderden shownotes door `HtmlCompat` halen. Filter na een korte
  pauze in het typen.
- Een treffer die alleen in de shownotes zit, krijgt een regel tekst rond het
  gevonden woord onder de titel. Anders zie je niet waarom de aflevering erbij
  staat.
- Zeg altijd waarin gezocht is: "12 van de 300 afleveringen in de feed". Kwam de
  lijst niet uit de feed maar uit Apple's catalogus (de terugval
  `lookupDetail`, hooguit 50), zeg dat dan. Waarschuw alleen voor een ingekort
  archief als daar een teken van is: Apple telt meer afleveringen dan de feed, of
  de laagste `itunes:episode` is groter dan 1 (idee 9 leest die).
- Geen treffers: zeg dat, met een knop om in heel Toadcast te zoeken (het
  bestaande zoekscherm, met dezelfde zoekterm).


# Beoordelingen door de criticus

Meetlat: `onderzoek.md` in deze map. Per idee de vragen:

- Gebruiken of missen luisteraars van de top-10-apps dit echt?
- Kan het zonder eigen server?
- Dupliceert het iets wat Toadcast al heeft?
- Voegt het iets toe voor hitlijsten én voor gewoon luisteren?

## Ronde 1

Uitslag: **8 goedgekeurd** (1, 2, 3, 4, 5, 6, 7, 10), **2 afgekeurd** (8, 9).

### 1. Downloaden en automatisch klaarzetten — GOEDGEKEURD

**Waarom:**

- Tafelstakes bij alle tien apps en de grootste achterstand van Toadcast. Het
  staat al bovenaan "Nog te doen".
- Het ontwerp klopt met wat we weten. 61% streamt of downloadt pas vlak voor het
  luisteren (Podsurvey), dus "de nieuwste N" is beter dan een hele achterstand
  binnenhalen. Een opslaglimiet volgt Pocket Casts 8.8.
- Media3 `DownloadManager` met een `CacheDataSource` werkt volledig op het toestel.
  Het dupliceert niets: "Offline" in de app gaat nu alleen over de laatst opgehaalde
  lijst, niet over audio.

**Voorwaarden:**

- Automatisch downloaden staat standaard **uit**, of op N=1.
- Afleveringen die je hebt bewaard ("Bewaren") en afleveringen in de wachtrij
  worden nooit automatisch gewist.
- "Alleen op wifi" en "alleen bij opladen" komen uit de `Requirements` van Media3
  of WorkManager-voorwaarden. Niet zelf pollen.
- Laat per rij de status zien (bezig, klaar, mislukt) en een verbruiksbalk in
  Instellingen.
- Test downloads achter feeds met redirects en tracking-prefixes (OP3, Podtrac,
  Chartable-achtig). Die geven bij het ophalen van de audio vaak 302-ketens.

### 2. Hoofdstukken — GOEDGEKEURD

**Waarom:**

- Tafelstakes bij Apple, Spotify, YouTube, Pocket Casts, Overcast, Podcast Addict
  en AntennaPod.
- De slaaptimer "einde van dit hoofdstuk" is een fijne aanvulling.
- Zonder server te doen. ExoPlayer levert ID3 `ChapterFrame`'s al als metadata bij
  het afspelen, en `podcast:chapters` is een los JSON-bestand.
- Wees realistisch over de dekking. Maar ~0,9% van de afleveringen in de Podcast
  Index heeft `podcast:chapters`. ID3-hoofdstukken komen vaker voor, maar zijn pas
  zichtbaar als de audio (deels) geladen is.

**Voorwaarden:**

- Lees ook **Podlove Simple Chapters** (`psc:chapters`) uit de RSS. Die komen veel
  voor bij Nederlandse en Duitse makers, en je hebt ze zonder de audio aan te raken.
- Zonder hoofdstukken is er geen enkel spoor in de speler: geen lege lijst, geen
  streepjes.
- Laat vorige/volgende **niet** stilzwijgend van betekenis veranderen. Die knoppen
  gaan nu over de wachtrij. Spring per hoofdstuk via de lijst of via tikken op het
  hoofdstuklabel, of maak het een expliciete instelling.
- Voor de hoofdstukkenlijst vóór het afspelen: haal de ID3-kop op met een
  HTTP-range-verzoek. Anders toon je de lijst pas zodra het afspelen begint.

### 3. Transcriptie meelezen — GOEDGEKEURD (met harde voorwaarde)

**Waarom:**

- Transcripties zijn sinds 2024–2026 tafelstakes geworden (Apple, Spotify, Pocket
  Casts, Overcast, Amazon). De uitvoering volgt precies het goede model: meelezen,
  tikken om te springen, zoeken, en een label op de lijst.
- Op het toestel transcriberen niet doen is de juiste keuze.
- De zwakke plek is de dekking. Maar ~4% van de afleveringen heeft
  `podcast:transcript`. De grote apps halen hun dekking uit eigen AI, niet uit RSS.
  Voor de meeste gebruikers is de functie dus meestal onzichtbaar.
- Toch goedgekeurd: het is goedkoop zodra de hoofdstuk-infrastructuur er is
  (hetzelfde patroon: extra bestand uit de feed, gekoppeld aan de afspeelpositie).
  Het is ook een toegankelijkheidsfunctie.

**Voorwaarden:**

- **Meet eerst de dekking.** Laat de verzamelaar tellen welk deel van de
  afleveringen in de top 200 van NL en US een transcript heeft. Ligt dat onder de
  ~10%, dan pas bouwen ná 1, 2, 4 en 7.
- Het tabblad "Tekst" verschijnt alleen als er een transcript is.
- Ondersteun SRT, VTT en de Podcasting 2.0-JSON. Laat HTML-transcripten (zonder
  tijden) als platte tekst zien, zonder meelopen.

### 4. Stilte inkorten en stemversterking — GOEDGEKEURD

**Waarom:**

- Het meest onderscheidende luisterkenmerk uit het onderzoek. Overcast bouwde er
  zijn merk op, Pocket Casts, Podcast Addict, AntennaPod en Podbean hebben het, en
  YouTube (Trim Silence, Auto Speed) en Apple (Enhance Dialogue) halen het nu in.
- Technisch klein: Media3 1.5.1 heeft `setSkipSilenceEnabled` en
  `SilenceSkippingAudioProcessor`. `LoudnessEnhancer` en `DynamicsProcessing`
  (API 28+) zitten in Android zelf. Geen server nodig.
- Per show onthouden volgt Pocket Casts en Podcast Addict. Het teller-idee
  ("bespaard") is wat Podcast Addict en Overcast als statistiek tonen.

**Voorwaarden:**

- De teller "Bespaard" moet **echt gemeten** zijn. Gebruik een eigen
  `RenderersFactory` die de `SilenceSkippingAudioProcessor` vasthoudt en
  `getSkippedFrames()` optelt. Geen schatting op basis van de snelheid.
- Zet de stemversterking conservatief af, zonder clipping, en test haar op
  muziekrijke shows.
- De twee schakelaars komen in het snelheidsmenu. Een extra rij knoppen in de
  speler is niet nodig.
- De teller mag later in de Terugblik (idee 6) terugkomen, niet op twee plekken
  dubbel.

### 5. Weekoverzicht van de hitlijsten — GOEDGEKEURD

**Waarom:**

- Dit is precies het gat uit les 5. Geen van de tien apps laat beweging door de
  tijd zien, en de helft van de luisteraars ontdekt nieuwe shows in de eigen app.
- Een wekelijkse samenvatting (stijgers, binnenkomers, jouw shows) maakt van
  Toadcasts historie iets wat je terugziet zonder zelf te zoeken. Dat is de
  hitlijsten-tegenhanger van Spotify's recaps.
- Het dupliceert de chart-alerts niet: die zijn een kaart "Sinds gisteren" in de
  Bibliotheek, dit is een weekbeeld.

**Waar het nu niet klopt, en de voorwaarden:**

- De app legt momentopnames alleen vast als je een lijst **opent**
  (`ChartRepository` → `store.record`), en bewaart maar 30 dagen. Voor lijsten die
  je die week niet opende heb je dus gaten.
  - Neem de weekvergelijking daarom uit de verzamelaar: `charts-service` bewaart de
    dagelijkse historie en `movers`. Gebruik de eigen momentopnames alleen als
    aanvulling.
- Er is nog geen dagelijkse ophaalronde in de app, alleen de `MakerCheckWorker`.
  Er komt dus een eigen wekelijkse `PeriodicWorkRequest` bij.
- "Lijsten die jij gebruikt": leg je vast op hooguit drie lijsten. Afleiden uit de
  categorieën van gevolgde shows plus de laatst bekeken lijst, en in te stellen.
- De melding is een aparte meldingskanaal-categorie, uit te zetten (Android 13+:
  toestemming vragen op een zinnig moment).
- De dagelijkse alert-kaart blijft bestaan. "In plaats van losse alerts" geldt
  alleen voor push-meldingen.

### 6. Jouw luisterjaar (Terugblik) — GOEDGEKEURD

**Waarom:**

- Statistieken en jaaroverzichten zijn geliefd: Spotify Wrapped, Pocket Casts
  Playback en de Stats Heatmap, AntennaPod Echo, Podcast Addict. Het kan volledig
  op het toestel (les 7).
- "Vroege luisteraar" is de hitlijst-draai die geen enkele concurrent kan maken.
  Het past bij Toadcast als geen ander idee. Een deelbare kaart is ook gratis
  verspreiding.

**Voorwaarden:**

- Er is nu **geen** luisterlog, alleen de laatste positie per aflevering.
  - Begin daarom meteen met het vastleggen van geluisterde seconden per dag × show,
    ook als het scherm pas later komt. Elke week wachten is een week data kwijt.
- `FollowedShow` heeft geen volgdatum. Voeg `followedAt` toe.
- De lokale historie is 30 dagen. Haal "wanneer haalde de show de top 20" uit de
  verzamelaar (`charts/shows`).
  - Definieer "top 20" eenduidig: in de lijst voor het land en de categorie van die
    show, Apple óf Spotify, de eerste datum telt.
- Het eerste jaar is dun. Laat de Terugblik ook per maand werken, en toon geen
  kaart met minder dan een paar uur luisteren.
- Maak de deelbare afbeelding op het toestel (Compose naar bitmap, deel-sheet),
  zonder uploaden.

### 7. OPML importeren en exporteren — GOEDGEKEURD

**Waarom:**

- Les 4 letterlijk. Elke open app in de top heeft OPML (Pocket Casts, Overcast,
  Podcast Addict, Castbox, AntennaPod), geen enkele gesloten app.
- Bij de sluiting van Google Podcasts was het dé verhuisroute.
- Voor een app zonder account is het ook de enige back-up. Klein, lokaal,
  laag risico.

**Voorwaarden:**

- Een OPML bevat alleen feed-URL's, en Toadcast koppelt shows nu aan Apple- en
  Spotify-ID's. Maak daarom volgen op basis van alleen een feed-URL mogelijk.
  - Koppel later aan een Apple-ID zodra de show in een lijst of zoekresultaat
    opduikt: Apple's lookup geeft `feedUrl` mee.
  - Blokkeer de import niet op het opzoeken van een ID.
- Daarmee komt "eigen RSS-feed toevoegen" bijna gratis mee. Neem het meteen mee,
  want Toadcast mist het nu.
- Bij export: shows zonder RSS (alleen van Spotify) meld je apart ("3 shows konden
  niet mee: geen open feed").
- Registreer een intent-filter voor `text/x-opml` en XML-bestanden, zodat
  "delen met Toadcast" vanuit een andere app werkt.

### 8. Widget op het beginscherm — AFGEKEURD (in deze vorm)

**Waarom afgekeurd:**

- Widgets zijn in het onderzoek geen tafelstakes. Alleen Pocket Casts investeerde er
  zichtbaar in (2024), en er zijn geen gebruikscijfers die de moeite rechtvaardigen.
- Variant (a), de spelerwidget, dupliceert wat Android al geeft: de mediamelding,
  het vergrendelscherm en de mediaspeler in de snelle instellingen (Android 11+).
  Die heeft Toadcast via Media3 al gebouwd.
- Variant (b), de top 5 van een lijst, is wél eigen aan Toadcast. Maar hij leunt op
  een "bestaande dagelijkse ophaalronde" die in de app niet bestaat. Er is alleen de
  `MakerCheckWorker`, en momentopnames ontstaan pas bij het openen van een lijst.
- Samen is het veel werk (Glance, twee layouts, configuratiescherm) voor weinig
  luisterwaarde, terwijl tafelstakes als 1, 2 en 7 nog ontbreken.

**Opnieuw indienen kan als:**

- alleen de hitlijstwidget;
- ververst door dezelfde worker als het weekoverzicht (idee 5), met data uit de
  verzamelaar;
- ná de tafelstakes.

### 9. Slimme lijsten (filters) — AFGEKEURD (in deze vorm)

**Waarom afgekeurd:**

- Filters zijn onderscheidend, maar alleen bij de apps van zware luisteraars
  (Pocket Casts 1,3–1,7%, Podcast Addict, Overcast, AntennaPod). Geen van de grote
  vijf heeft ze.
- Een regelbouwer met datum, duur, status en bron is veel interface voor een
  kleine groep. Het tast de eenvoud van een app aan die draait om hitlijsten
  (les 10: alleen achter een instelling).
- Het hitlijstfilter ("top 10 van mijn categorieën, nog niet gehoord") overlapt met
  Hitlijsten → Afleveringen en met idee 10.
- Wat hier wél ontbreekt, en wat grote apps wel hebben, is één
  **samengevoegde lijst "Nieuwe afleveringen"** van alle gevolgde shows. Spotify
  voegde in 2025 precies zo'n volgen-feed toe. Toadcast toont nu alleen "2 nieuw"
  per show.

**Opnieuw indienen kan als** zo'n Nieuw-inbox, met hooguit drie vaste chips
(bijvoorbeeld *Onbeluisterd · Kort < 30 min · Deze week*) en "alles in de wachtrij".
Zonder regelbouwer.

### 10. Hitlijstradio — GOEDGEKEURD (zonder proefmodus)

**Waarom:**

- Het verbindt hitlijst en luisteren met één tik. Dat is les 5 in zijn zuiverste
  vorm.
- Het leunt op wat er al is: de afleveringslijsten per categorie via
  `charts-service`, de wachtrij, en de voortgang om te bepalen wat je al hoorde.
  Geen server nodig.
- Er is enig bewijs dat achterover-ontdekken werkt: Spotify meldt dat meer dan de
  helft van de gebruikers van Prompted Playlists een nieuwe show vond.
- De **proefmodus** ("eerste 10 minuten") keur ik af. De eerste minuten van een
  populaire aflevering zijn juist vaak ingevoegde voorrolreclame en intro. Je proeft
  dus vooral advertenties, en het maakt de wachtrijlogica ingewikkeld.

**Voorwaarden:**

- Vraag of meld wat er met de bestaande wachtrij gebeurt (vervangen of achteraan
  toevoegen).
- Hooguit één aflevering per show.
- Een grens aan het aantal: 10, of een totale duur.
- Werkt alleen op lijsten met afleveringen. Spotify levert shows, geen afleveringen;
  daar komt de knop niet, of hij pakt per show de nieuwste aflevering. Kies één
  van beide en zeg het in de interface.
- Wil je later "proeven", doe dat dan met hoofdstukken (idee 2) of
  `podcast:soundbite` waar die bestaan. Niet met een vaste eerste 10 minuten.

## Ronde 2

Uitslag: **beide goedgekeurd** (11 en 12). Ze vervangen de afgekeurde 8 en 9.

### 11. Toadcast in de auto (Android Auto) — GOEDGEKEURD

**Waarom:**

- Tafelstakes. Alle tien apps uit het onderzoek werken met Android Auto of CarPlay,
  en in de auto kun je alleen luisteren. Dat is precies het moment waarop mensen
  van YouTube naar een audio-app gaan (Cumulus herfst 2025).
- Kan zonder server (les 6). `PlaybackService` is nu een `MediaSessionService` met
  eigen `CommandButton`'s voor −15/+30 en snelheid. Een `MediaLibraryService`
  ervan maken is een uitbreiding, geen herbouw.
- De bladerboom is goed gekozen. *Hitlijst* als tabblad maakt het meer dan een kale
  afspeler: geen concurrent zet een hitlijst in de auto.

**Voorwaarden:**

- **Eerst moet de speler op een echt toestel werken.** Volgens de README is
  afspelen nog nooit hoorbaar getest. Test Android Auto daarna met de Desktop Head
  Unit.
- Android Auto toont hooguit vier tabbladen en een beperkt aantal eigen
  actieknoppen. −15/+30 en snelheid passen, meer niet.
- Hoezen moeten als `content://`-URI via een eigen `ContentProvider` worden
  aangeboden, niet als losse http-URL.
- "Hey Google, speel Toadcast" vraagt twee dingen:
  - `onPlaybackResumption` (laatste aflevering en positie uit `LocalStore`);
  - afhandeling van een spraakopdracht met zoekterm in `onAddMediaItems`, minstens
    "speel [show]" → de nieuwste aflevering.
  - Zonder die zoekafhandeling keurt Google de app af.
- *Nieuw* en *Hitlijst* moeten het ook doen zonder verse data (laatste cache).
  Zonder verbinding zie je een duidelijke lege staat, geen draaiend wiel.
- **Android Automotive is niet "gratis mee"**. De service is dezelfde, maar
  Automotive vraagt een eigen build of formfactor in de Play Console, en een eigen
  kwaliteitsbeoordeling. Doe eerst Android Auto; Automotive is een aparte stap.
- *Gedownload* verschijnt pas als idee 1 er is. Toon geen leeg tabblad.

### 12. Instellingen per show — GOEDGEKEURD

**Waarom:**

- Les 10: Pocket Casts, Podcast Addict, Overcast en AntennaPod hebben
  instellingen per show (snelheid, intro en outro overslaan, effecten), en zware
  luisteraars missen ze als ze overstappen.
- Het ontwerp respecteert de eis uit ronde 1 die idee 9 deed sneuvelen. Alles staat
  op "zoals algemeen", en alleen wie het menu opent ziet opties. Er is geen
  regelbouwer, en wie de app nooit aanpast merkt er niets van.
- Het label "1,3× · intro 45 s" en de aanduiding "voor deze show" bij de snelheid
  lossen het klassieke probleem op: "waarom speelt deze show ineens sneller?".
- Volledig lokaal: een map van show-ID naar instellingen in `LocalStore`.

**Voorwaarden:**

- **Bouw alleen de opties waarvan de basis er is.**
  - Snelheid en intro/outro overslaan kunnen nu.
  - Stilte inkorten komt pas met idee 4, automatisch downloaden pas met idee 1.
  - De schakelaar "meldingen bij nieuwe aflevering" stuurt een functie aan die niet
    bestaat. De app stuurt nu alleen makermeldingen (`MakerCheckWorker`), geen
    meldingen bij nieuwe afleveringen. Die melding is zelf een tafelstake en moet
    eerst gebouwd worden (algemene schakelaar plus een dagelijkse feedcontrole).
    Toon de schakelaar pas daarna.
- Intro overslaan geldt alleen als je een aflevering vanaf 0 start, niet bij
  hervatten. Outro overslaan markeert de aflevering als beluisterd en gaat door naar
  de wachtrij.
- Waarschuw in de uitleg dat ingevoegde reclame de intro per keer anders lang
  maakt. Pocket Casts doet het ook zo, dus acceptabel.
- Een snelheid die je in de speler kiest terwijl een show een eigen snelheid heeft,
  past de **show-instelling** aan. Maak dat zichtbaar ("voor deze show"), met een
  manier om terug te gaan naar "zoals algemeen".
- Gebruik een stabiele sleutel: ook shows die via OPML met alleen een feed-URL
  binnenkomen (idee 7) moeten hun instellingen houden als ze later aan een Apple-ID
  gekoppeld worden.

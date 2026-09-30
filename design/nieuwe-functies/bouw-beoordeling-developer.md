# Bouwbeoordeling door de senior developer

Beoordeeld: `60ae451c` plus de lint-commit `7b21984a`, vergeleken met `6fcae872`.
De unit-tests draaien groen (`:app:testDebugUnitTest`, exit 0). Regelnummers
verwijzen naar HEAD.

## Ronde 1

Uitslag: **0 goedgekeurd**, **4 afgekeurd** (1, 2, 3, 4). Functie 2 en 3 zijn
dichtbij: bij 2 één kleine bug, bij 3 alleen de productvoorwaarde.

### 1. Downloaden en automatisch klaarzetten — AFGEKEURD

Blokkerend:

1. `data/download/DownloadWorker.kt:80` — hervatten stuurt `Range: bytes=N-` zonder `If-Range`/ETag en zonder de `Content-Range`-totaal te vergelijken. Hosts met dynamisch ingevoegde reclame (Acast, Megaphone, Art19, vaak achter OP3/Podtrac) leveren per verzoek andere bytes. Na een wifi-onderbreking plak je dan twee verschillende bestanden aan elkaar en krijg je stille, kapotte audio. Oplossing: bewaar ETag/Last-Modified en de lengte naast de `.part`; stuur `If-Range`; verschilt het totaal of krijg je 200, begin dan opnieuw. Een 416 (het `.part`-bestand is al compleet, maar het proces stierf vóór `renameTo`) loopt nu drie keer vast tot FAILED, en ook "Opnieuw" blijft falen. Behandel 416 als klaar of begin opnieuw.
2. `Downloads.kt:216` + `DownloadWorker.kt:180/189` — de automatische downloads blijven rondpompen. Past alles niet onder de grens (bijvoorbeeld omdat handmatige downloads de ruimte al vullen), dan wist `cleanUp` de net binnengehaalde automatische aflevering. Zes uur later zet `autoPlan` hem opnieuw in de rij, want hij staat niet meer in `existing`. Dat kost vier keer per dag honderden MB. Oplossing: controleer de ruimte vóór het klaarzetten en sla over met de reden "geen ruimte". Wis daarnaast nooit een aflevering die in het actuele `wanted` staat.
3. `ui/library/LibraryViewModel.kt:409` en `ui/tracker/TrackerViewModel.kt:75` — ontvolgen buiten de podcastpagina zet automatisch downloaden niet uit. `AutoDownloadWorker` blijft dan feeds ophalen en audio binnenhalen voor een show die je niet meer volgt. Voor een `feed-`-show gaat dat bovendien via een zoekopdracht op de titel (zie 4.1). Oplossing: haal in `LocalStore.toggleFollow` bij ontvolgen ook `autoDownload[id]` weg, of laat de worker alleen gevolgde shows bekijken.
4. `ui/WoolacastNav.kt:386` (en de rijen in DetailScreen/DownloadsTab) — "Download verwijderen" op de aflevering die nu speelt, wist het bestand onder ExoPlayer weg. De eerstvolgende seek of het hervatten geeft een bronfout. `cleanUp` beschermt `playingId`, maar `remove` doet dat niet. Oplossing: wissel eerst naar de remote-URL (`replaceMediaItem` op de huidige positie) of stel het wissen uit tot de aflevering niet meer speelt.
5. `AndroidManifest.xml` (`allowBackup="true"`, geen backup-regels) + `Downloads.kt:46` — audio in `filesDir/downloads` valt onder Auto Backup. Boven de 25 MB stopt Android de hele app-back-up, dus ook `store.json` met je gevolgde shows. Bij overzetten naar een nieuw toestel gaan er gigabytes mee. Oplossing: gebruik `noBackupFilesDir`, of sluit `downloads/` uit met `dataExtractionRules`/`fullBackupContent`.
6. Voorwaarde van de criticus niet nagekomen: "test downloads achter redirects en tracking-prefixes". Er is geen enkele test voor `fetch` (alleen `DownloadPolicyTest`). Oplossing: haal `fetch` los van de Worker en test het met MockWebServer: een 302-keten, hervatten met 206, 200 op een Range-verzoek, 416, en een ander totaal bij het hervatten.

Suggesties:

- `Downloads.kt:53`: `_progress.value = …` is read-modify-write vanaf parallelle workers. Gebruik `_progress.update {}`. Bij `isStopped` (`DownloadWorker.kt:50/62/94`) wordt `report(null)` niet aangeroepen, dus de rij blijft op "Downloaden 43%" staan in plaats van "Wacht op wifi".
- `DownloadWorker.kt:102`: `setForeground` en de voortgangsflow gaan elke 256 KB af, op snelle wifi tientallen keren per seconde. Android laat dan meldingen vallen, en `WoolacastNav` hercomponeert steeds. Beperk het tot hooguit één keer per seconde.
- `DetailScreen.kt:126` en `DownloadsTab.kt:118`: `waitingForWifi()` is een binder-aanroep tijdens compositie, per rij, en de ViewModel pollt elke 5 s zolang hij leeft. Gebruik een `NetworkCallback` via `callbackFlow`.
- `cleanUp` (`Downloads.kt:129`) beschermt half beluisterde afleveringen niet, terwijl `AutoDownloadWorker` en de uitleg in DetailScreen ("…of al begon") dat wel beloven. Trek dat gelijk.
- Vanaf Android 12 gooit `setForeground` vanuit de achtergrond (bij periodiek werk) een fout. Die wordt ingeslikt, en daarna geldt de limiet van 10 minuten. Het hervatten vangt dat op, maar alleen goed na punt 1.

### 2. Hoofdstukken — AFGEKEURD

Blokkerend:

1. `player/PlayerController.kt:342` — de slaaptimer "Einde hoofdstuk" vergelijkt met een vaste positie (`positionMs >= stopAt`). Wie vooruitspringt voorbij die plek, met +30 over een reclameblok of een later hoofdstuk uit de lijst, ziet het afspelen meteen stoppen. Oplossing: onthoud het hoofdstuk en niet de positie, en bereken het einde opnieuw na een seek. Of zet de timer uit als je voorbij het einde springt. Voeg er een test voor toe.

Suggesties:

- `Chapters.kt:40`: een netwerkfout wordt als "geen hoofdstukken" gecachet tot het proces stopt. Cache alleen geslaagde resultaten.
- `Id3Chapters.read` doet eerst een verzoek van 10 bytes en daarna een venster van 32 KB, dus twee rondes door de redirect-keten. Haal meteen 32 KB op. Bij een 200 op een Range-verzoek met grote offset kan `skipNBytesCompat` veel downloaden; begrens dat op `MAX_FETCH`.
- De voorwaarden zijn verder netjes nagekomen: Podlove, geen spoor zonder hoofdstukken, vorige/volgende ongemoeid, en een range-verzoek. De tests dekken JSON en ID3 v2.3/v2.4.

### 3. Transcriptie meelezen — AFGEKEURD

Blokkerend:

1. De harde voorwaarde van de criticus is niet nagekomen: "Meet eerst de dekking (top 200 NL en US); onder ~10% pas bouwen ná 1, 2, 4 en 7." In `charts-service/` staat geen meting en idee 4 is niet gebouwd. Oplossing: laat `collect.py` het aandeel met `podcast:transcript` tellen en leg het getal vast. Of vraag de criticus expliciet om een vrijstelling.

Technisch is de functie in orde: SRT, VTT, JSON en HTML als platte tekst, de knop alleen als er een transcript is, tikken om te springen, zoeken. Suggesties:

- `Transcripts.kt:54`: `body.string()` heeft geen bovengrens. Begrens op een paar MB.
- Faalt de beste vorm (bijvoorbeeld een JSON-404), dan is er geen terugval naar VTT/SRT. Bewaar in `RssFeedParser` alle refs.
- Er wordt niet op taal gekozen als er meerdere zijn.
- `decode` vervangt `&amp;` vóór `&lt;`, waardoor `&amp;lt;` twee keer gedecodeerd wordt.

### 4. OPML import/export + zelf feed toevoegen — AFGEKEURD

Blokkerend:

1. `ui/WoolacastNav.kt:373` + `data/PodcastRepository.kt:29` — "Ga naar podcast" vanuit de speler (en elke andere plek die `feedUrl = null` doorgeeft) voor een show die alleen op zijn feed gevolgd wordt (`feed-…`), valt terug op `searchFeedUrl(title)`. Die pakt het eerste Apple-resultaat met die naam. Je ziet dan een andere podcast onder de id van jouw gevolgde show, of een foutmelding. Dat raakt precies de kern van deze functie. Oplossing: los `feed-`-ids op via `store.follows` (hun `feedUrl`) en gebruik voor zulke ids nooit de zoekterugval.
2. `data/opml/ShowImporter.kt:88` + `ui/library/ImportScreen.kt:103-107` — de import draait in `viewModelScope` en slaat pas aan het eind iets op met `followAll`. Wie tijdens "Feeds controleren: 40 van 120" teruggaat, of wiens proces wordt gestopt, verliest alles. Het koppelen draait wel bewust in `appScope`. Oplossing: volg per geslaagde feed meteen, of draai de import in `appScope` of een Worker.
3. Geen tests voor `ShowImporter.import`/`link` en `LocalStore.relinkFollow`, terwijl het verhuizen van downloads, wachtrij en bewaard naar een nieuwe id precies het risicovolle deel is. Voeg tests toe met een nep-`FeedClient`/`AppleCatalogApi`.

Suggesties:

- Een import van 200 feeds haalt elke volledige feed op, ook via mobiele data (tot honderden MB). Waarschuw of vraag om wifi.
- `link()` zoekt bij elke start opnieuw álle niet-gekoppelde feeds op, met 3 s per stuk. Onthoud "niet gevonden" een tijd.
- Export: een Apple-show met `feedUrl == null` telt als "geen open feed", terwijl hij er een heeft. Zoek de feed eerst op.
- `MainActivity.takeOpml`: na procesdood levert Android de oorspronkelijke VIEW/SEND-intent opnieuw af. Controleer daarom `savedInstanceState == null`. De uri-toestemming is dan vaak al verlopen en de foutmelding zegt ten onrechte "geen OPML".

### Algemeen (suggesties, stijl)

- Volledig gekwalificeerde namen midden in de code (`kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>` in `WoolacastApp.kt`, `nl.woolacast.data.opml.Opml.write` en `kotlinx.coroutines.withContext` in `LibraryViewModel.export`, `androidx.compose.foundation.Canvas` in `PlayerScreen`) en ongesorteerde imports wijken af van de rest van de code. De Nederlandse commentaren zijn wel consistent en van goede dichtheid.
- `LocalStore.download()` leest het niet-volatile `data` buiten de mutex, vanaf de threads van de workers. Maak het `@Volatile` of lees uit `_downloads.value`.

## Ronde 2

Beoordeeld: `29ec24e6`, vergeleken met `7b21984a`. Alle 113 unit-tests slagen
(29 overgeslagen, 0 fouten). Regelnummers verwijzen naar `29ec24e6`.

Uitslag: **2 goedgekeurd** (3, 4), **2 afgekeurd** (1, 2). Bij allebei gaat het
om één restpunt dat met een paar regels op te lossen is.

### 1. Downloaden en automatisch klaarzetten — AFGEKEURD

Van ronde 1 is opgelost: 1.2 (`fits()` en `evicted` maken een einde aan het rondpompen), 1.3 (`toggleFollow`), 1.4 (`orphans` houden het bestand vast), 1.5 (back-upregels voor beide API-routes) en 1.6 (`AudioFetcherTest` met MockWebServer). Punt 1.1 is grotendeels opgelost: `If-Range`, `.meta` en 416 werken nu goed.

Blokkerend:

1. `data/download/AudioFetcher.kt:59-66` — een 206 die níét aansluit (`continues()` is false: een ander begin of een andere totale lengte) krijgt `append = false`. Het deelantwoord wordt dan vanaf byte 0 als heel bestand weggeschreven. `total` is de lengte van alleen dat stuk, dus de controle `done < total` slaagt en de aflevering wordt DONE met het begin eraf. Bij een m4a zonder `moov` is hij onspeelbaar, bij een mp3 mist het begin. Het gebeurt als een server `If-Range` negeert maar `Range` wel honoreert, precies het geval waartegen deze code moet beschermen. De afwijking wordt dus wél gezien, maar daarna wordt het verkeerde gedaan. Oplossing: gooi bij een 206 met `!append` `part` en `.meta` weg en gooi een `IOException` (de volgende poging begint schoon zonder Range). Voeg een test toe: "206 met een ander totaal plakt niet en schrijft geen stuk als geheel".

Suggesties:

- `Downloads.kt:153`: een orphan wordt op bestandsnaam gewist. Die naam hangt vast aan de aflevering-id. Wis je de download van wat speelt en download je hem daarna opnieuw, dan krijgt de nieuwe download dezelfde naam. Bij de volgende wissel wist `cleanUp` dan het nieuwe bestand, terwijl het record DONE blijft. Haal de orphan-regel weg in `enqueue`, of sla orphans over waarvoor een record bestaat.
- `AudioFetcher.kt:52`: het 416-pad "al compleet" laat het `.meta`-bestand staan tot de download ooit gewist wordt. Wis het daar ook.
- `DownloadWorker.reason`: "Kon het bestand niet opslaan" (een mislukte `renameTo`) wordt "Niet genoeg ruimte". Dat klopt meestal niet; maak er "Opslaan lukte niet" van.
- Het pollen elke 5 s voor "wacht op wifi" staat nog open (bewust niet gedaan; blijft een suggestie).

### 2. Hoofdstukken — AFGEKEURD

`EndOfChapter` als modus met `chapterStop()` is de goede opzet. Het cachen van netwerkfouten en de ID3-kop in één venster zijn opgelost.

Blokkerend:

1. `player/PlayerController.kt:148` / `player/PlaybackService.kt:339-340` — het stoppunt wordt alleen opnieuw berekend bij sprongen via `PlayerController` (`afterSeek`). De −15/+30-knoppen in de melding, op het vergrendelscherm, via een koptelefoon of van andere media-controllers roepen `session.player.seekBack/seekForward/seekTo` rechtstreeks aan. Met "Einde hoofdstuk" aan en +30 over de hoofdstukgrens op het vergrendelscherm pauzeert de volgende tick meteen: dezelfde bug als in ronde 1, via een andere ingang. Oplossing: reken het stoppunt opnieuw in de bestaande `listener.onEvents` bij `EVENT_POSITION_DISCONTINUITY` (reden `DISCONTINUITY_REASON_SEEK`). Dat vangt elke bron, en de losse `afterSeek`-aanroepen kunnen dan weg.

Suggesties:

- `ChapterStopTest` staat in `SpeedsTest.kt` met volledig gekwalificeerde namen. Geef hem een eigen bestand met gewone imports, zoals de andere tests.

### 3. Transcriptie meelezen — GOEDGEKEURD

De productvoorwaarde is afgehandeld: de dekking is gemeten (`dekking.md`: NL 5–7%, US 14%) en de gebruiker heeft expliciet gekozen om nu te bouwen. De bouw zelf is in orde. De groottegrens van 5 MB via `source.request` is correct (de charset uit Content-Type, anders UTF-8), en `&amp;` wordt nu als laatste ontsleuteld.

Suggesties (uit ronde 1, blijven staan): terugval naar VTT/SRT als de beste vorm faalt, en kiezen op taal.

### 4. OPML import/export + zelf feed toevoegen — GOEDGEKEURD

Alle drie de blokkerende punten zijn opgelost:

- `openPodcast` vult de feed aan uit de gevolgde shows, en `detail()` zoekt een `feed-`-id nooit meer op titel op.
- De import draait in `appScope` en volgt per geslaagde feed meteen; het scherm pakt de sessie weer op.
- `ShowImporterTest` dekt import, koppelen/meeverhuizen en ontvolgen.

De export met `lookupMany` en de controle op `savedInstanceState` zijn netjes.

Suggesties:

- `ShowImporter.kt:78-86`: start je een tweede import (een ander bestand) terwijl de eerste nog loopt, dan blijft de oude job zijn uitkomsten in de nieuwe `session` schrijven. Hij zet die sessie bovendien op `done`. Annuleer `importJob` bij een nieuwe key, of controleer de key in de callback.
- Een import haalt nog steeds alle feeds op, ook via mobiele data.

## Ronde 3

Beoordeeld: `9d2c5e0e`, vergeleken met `29ec24e6` (met daartussen `fe581fb8`).
Alle 114 unit-tests slagen (29 overgeslagen, 0 fouten).

Uitslag: **beide goedgekeurd** (1 en 2). Daarmee zijn alle vier de functies
goedgekeurd.

### 1. Downloaden en automatisch klaarzetten — GOEDGEKEURD

Het blokkerende punt is opgelost. Een 206 zonder hervatting, of een 206 die niet aansluit, gooit nu het deel en `.meta` weg en daarna een `IOException`. `append` kan dus alleen nog waar zijn bij een geldige hervatting, en de volgende poging begint zonder Range. De nieuwe test `een 206 die niet aansluit wordt geen half bestand` dekt precies dit geval.

De suggesties zijn ook verwerkt:

- `.meta` gaat weg bij een 416 op een compleet deel.
- `cleanUp` wist een orphan niet meer als een download-record die bestandsnaam weer gebruikt.
- De `renameTo`-fout heeft een eerlijke tekst.

Suggestie die openstaat: "wacht op wifi" pollen elke 5 s in plaats van een `NetworkCallback`.

### 2. Hoofdstukken — GOEDGEKEURD

Het blokkerende punt is opgelost. `onPositionDiscontinuity` met `DISCONTINUITY_REASON_SEEK` legt het stoppunt opnieuw, dus ook bij sprongen uit de melding, het vergrendelscherm, een koptelefoon of Android Auto. De MediaController geeft de nieuwe positie en dit bericht op de hoofdthread af vóór de volgende tick, dus er is geen venster waarin de oude grens nog geldt. `ChapterStopTest` staat in een eigen bestand.

Suggesties:

- Nu de listener elke seek opvangt, zijn de `afterSeek`-aanroepen in `seekTo`/`seekBy`/`seekToMs`/`previous` dubbel. Ze doen geen kwaad, maar kunnen terug naar `syncFromPlayer()`.
- Het pad via de listener heeft geen test. Een test met een `FakePlayer`/`TestExoPlayer` waarin een externe seek het stoppunt verlegt, legt dit gedrag vast.

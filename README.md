# Woolacast

Een Android-podcastapp waarvan de kern een hitlijsten-browser is: dezelfde lijst
bekijken per **bron**, **land**, **categorie** en **niveau** (podcast of
aflevering), en zien hoe een show over die lijsten heen beweegt.

Geen eigen server, geen account, geen API-sleutel. De app praat rechtstreeks met
open bronnen en bewaart alles lokaal.

## Bouwen en installeren

```
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

De APK komt in `app/build/outputs/apk/debug/`. Vereist de Android SDK
(compileSdk 35) en JDK 17. Zonder toestel kun je hem ook zijladen: aanzetten bij
*Apps van onbekende bronnen* en het bestand openen.

## Wat "geen cloud" wel en niet betekent

Wat waar is:

- Er is **geen backend van ons** — niets om te hosten, geen account, geen sleutel.
- Alle lijsten worden **lokaal bewaard**. Zonder verbinding opent de app met de
  laatst opgehaalde lijst en zegt erbij van wanneer die is.
- Wat je volgt en de dagelijkse momentopnames staan in één JSON-bestand in de
  app-opslag. Er gaat niets naar buiten.

Wat niet waar kan zijn: een *live* hitlijst komt nu eenmaal ergens vandaan. De
app moet het net op om te verversen. Wat we wel kunnen kiezen is *wiens* server
dat is, en die keuze is hier: open projecten zonder poortwachter.

## Bronnen

| Bron | Sleutel? | Wat het geeft |
| --- | --- | --- |
| Apple Podcasts | nee | shows 200 diep per categorie, afleveringen 100, 175 landen |
| Spotify | nee | shows én afleveringen in 26 landen, categorieen in zeven |
| RSS van de podcast zelf | nee | afleveringen, audio, omschrijvingen, artwork |

Voor shows gebruikt de app de ranglijst van de winkel zelf:

```
GET https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/charts
      ?cc={land}&g={genre-id}&name=Podcasts&limit=200
```

Die geeft alleen ids, maar gaat 200 diep, kent elke categorie (26 = alle samen)
en vraagt geen sleutel. Eén batch-lookup maakt er volledige vermeldingen van,
mét `feedUrl` — zodat de podcastpagina meteen naar de RSS kan zonder extra
aanroep. Getest: NL alle 200/200, NL Comedy 196/200, DE True crime 192/200.

De marketing-feed blijft in gebruik voor afleveringen en loopt vast boven de
honderd.

De podcastpagina leest de **RSS van de maker zelf** — daar staat alles in en er
zit niemand tussen. Dat is dezelfde route die AntennaPod neemt. Alleen bij een
Apple-lijst is er één opzoeking nodig om de feed-URL te vinden, want Apple geeft
een catalogus-id in plaats van een feed. Spotify geeft alleen een
`spotify:show:` uri, dus daar zoekt de app de naam op in diezelfde catalogus.

### Afleveringen per categorie

Apple heeft die lijst. `MZStoreServices.../charts?name=PodcastEpisodes` geeft de
echte ranglijst per categorie, 200 diep, zonder sleutel. Alleen: het zijn ids,
en **afleverings-ids zijn nergens publiek op te lossen**. De lookup-API kent
alleen show-ids (getest, `resultCount: 0`), `amp-api` geeft zonder bearer token
een 401, en de oude `toppodcastepisodes`-feed is leeg.

Dat is precies waar een dienst als Podchaser het verschil maakt: die draait
servers die dat id-voor-id ophalen, opslaan en opnieuw serveren. Een app op een
telefoon kan geen 200 losse aanroepen doen per lijst.

Daarom staat er nu een verzamelaar in `charts-service/`: een script dat die ids
één voor één oplost en het resultaat als platte JSON in deze repo zet. Een
GitHub Action kan het draaien; de app leest alleen van
`raw.githubusercontent.com`. Geen server, geen sleutel, geen account — de repo
moet alleen publiek zijn.

Staat de lijst er, dan toont de app Apple's echte volgorde, 50 diep per
categorie. Staat hij er niet, dan valt de app terug op de top 100 ophalen en
zelf op categorie schiften. Elke aflevering draagt één genre — soms een hoofdgenre
met id, soms alleen een subgenrenaam als "Nieuwscommentaar". De genreboom van de
winkel knoopt die aan hun hoofdgenre, in de taal van die winkel:

```
GET https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/genres?id=26&cc=nl
```

Eén aanroep per land, daarna uit het geheugen. In NL en DE getest: 100 van de
100 afleveringen laten zich zo toewijzen.

**Dit is dus een afgeleide lijst, geen kopie van Apple's eigen categorielijst.**
Grote categorieen leveren genoeg op (Nieuws ~39, Sport ~14 in NL), kleine weinig
(Comedy ~6). De app zet er "geschift op categorie" bij zodat het verschil
zichtbaar blijft.

### Over Spotify

`podcastcharts.byspotify.com` laadt zijn lijsten uit een JSON-endpoint op de
eigen site, zonder sleutel en zonder inlog:

```
GET https://podcastcharts.byspotify.com/api/charts/{categorie}?region={land}&limit={n}
Accept: application/json
```

Dat is dezelfde aanroep die hun pagina zelf doet, en de app doet hem nu ook.
Uitgemeten tegen de live API:

| Lijst | Landen | Niveau | Omvang |
| --- | --- | --- | --- |
| `top-podcasts` | 26 | shows | 200 |
| `top-episodes` | 26 | afleveringen | 200 |
| `trending` | 26 | shows | 200 |
| 16 categorieen (`comedy`, `news`, `true-crime`, …) | 7: us au br de mx se gb | **alleen shows** | 50 |

**Afleveringen per categorie bestaan bij Spotify niet.** Dat is geen
landsverschil maar een grens van de bron: hun eigen site zet `top-episodes`
náást de genres, niet eronder. Nagelopen in alle zeven categoriemarkten — us,
au, br, de, mx, se en gb geven daar allemaal shows terug, zonder
afleveringsvelden. Extra parameters (`type`, `level`, `chartType`, `genre`,
`category`) worden genegeerd; samengestelde vormen als `comedy_episodes` geven
een serverfout.

Apple kan die combinatie wel, dus de app biedt aan om over te stappen.

België zit er niet bij, en categorieen bestaan níét voor Nederland. De app zegt
dat in plaats van een lege lijst te tonen. `chartRankMove` geeft de richting van
de beweging mee (UP/DOWN/UNCHANGED); het *aantal* plaatsen vult de app aan uit
de eigen momentopnames.

**Wat je moet weten:** dit endpoint is niet gedocumenteerd. Spotify kan het
zonder aankondiging veranderen of dichtzetten, en het valt vrijwel zeker buiten
hun gebruiksvoorwaarden om er data uit te halen voor een eigen app — zeker als
je die verspreidt. Voor eigen gebruik is het risico dat het een keer stopt; ga
je publiceren, kijk er dan eerst naar. De code faalt netjes als het wegvalt: je
krijgt de uitleg en de lijst uit de cache.

Spotify geeft geen feed-URL, alleen een `spotify:show:` uri. De podcastpagina
zoekt de naam daarom op in de publieke Apple-catalogus om alsnog bij de RSS te
komen.

### Over Podchaser

Podchaser heeft dezelfde lijsten, maar hun GraphQL-endpoint antwoordt
`Invalid authorization request`: een commerciële data-API met sleutels en
voorwaarden over hergebruik. Nu Spotify rechtstreeks werkt is dat niet nodig.

### Over AntennaPod en Pocket Casts

Een app is geen databron — je kunt AntennaPod of Pocket Casts niet aanroepen
voor een hitlijst. Wat wel kan is hun *aanpak* overnemen, en dat is gebeurd:
RSS-first, geen backend.

Hun code overnemen kan ook, maar dat is een licentiekeuze:

- **AntennaPod** is GPL-3.0. Code daaruit gebruiken maakt Woolacast ook GPL-3.0.
- **Pocket Casts** is MPL-2.0 (Automattic). Soepeler: per bestand copyleft.

Geen van beide heeft trouwens hitlijsten — dat is nou juist wat deze app toevoegt.

## Stijgers en dalers

Er bestaat geen publieke bron voor "gisteren". De app bewaart daarom zelf per
lijst één momentopname per dag en vergelijkt de verse lijst met de meest recente
opname van een *eerdere* dag. Gevolg: **de eerste dag na installatie is er geen
beweging te zien.** Vanaf dag twee wel.

Dat mechanisme is ook de basis onder de chart-tracker die nog moet komen.

## Stand van zaken

De schermen volgen de mockup in `design/` — dezelfde maatvoering, kleuren,
lijniconen en lettertypen (Bricolage Grotesque voor titels en noteringen,
Instrument Sans voor de rest, beide gebundeld onder de SIL Open Font License).

| Onderdeel | Status |
| --- | --- |
| Hitlijsten: Apple en Spotify, land × categorie × niveau | werkt |
| Afleveringen direct uit een lijst afspelen (audio via de feed van de show) | werkt |
| Offline: laatst opgehaalde lijst | werkt |
| Stijgers en dalers | werkt vanaf de tweede dag |
| Podcastpagina uit RSS: kop, cadans, noteringsbalk, uitklapbare omschrijving | werkt |
| Afleveringen: voortgang, "#3 NL"-pilletje, wachtrij / hierna / bewaren / delen | werkt |
| Speler: artwork, bron-chip, balk met handvat, −15 / +30, vorige / volgende | werkt |
| Speler: snelheid, slaaptimer, wachtrij, delen, bewaren | werkt |
| Wachtrij speelt vanzelf door na de huidige aflevering | werkt |
| Afspelen (Media3, achtergrond, vergrendelscherm) | gebouwd, nog niet op een toestel getest |
| Volgen + bibliotheek: "2 nieuw" / "bijgewerkt di" per gevolgde show | werkt (leest de feeds) |
| Chart-alerts op shows die je volgt | werkt zodra er twee dagen historie is |
| Chart-tracker: tegels, grafiek met assen, hoogste notering, landen | werkt |
| Ontdek: stijgers, andere landen (met aantal bronnen), categorieën | werkt |
| Zoeken in podcasts en afleveringen | werkt |
| Luistervoortgang onthouden en hervatten | werkt |
| Afleveringen per categorie, Apple's echte volgorde | via charts-service |
| Donker thema | werkt |

De app bouwt en lint schoon. Wat er nog niet is: draaien op een echt toestel.

## Structuur

```
app/src/main/java/nl/woolacast/
  domain/     modellen, ChartQuery, ChartSource, catalogus van landen en categorieen
  data/
    apple/    de publieke Apple-feeds plus de genreboom
    spotify/  het chart-endpoint van podcastcharts.byspotify.com
    feed/     RSS-parser en -client: de route zonder tussenpersoon
    local/    volgen, momentopnames en de offline cache in één JSON-bestand
  player/     Media3-service met een StateFlow-laag eromheen
  ui/         Compose-schermen, thema, navigatie
              common/ bouwstenen uit de mockup: iconen, vlaggen, tabs, knoppen
              player/ het uitklapbare spelerscherm
charts-service/  legt dagelijks de ranglijsten vast (historie) en haalt op
              wat de app niet zelf kan: Apple's afleveringen per categorie
tools/        svg_to_vector.py — maakt de launcher-iconen uit de tekening
design/       de mockup en het icoon als bewerkbaar canvas
```

Bewust simpel voor een eerste versie: handmatige bedrading in plaats van een
DI-raamwerk, en een JSON-bestand in plaats van een database. Zodra er echte
historie overheen gaat is `LocalStore` het punt om naar Room te verhuizen.

## Nog te doen

- Afleveringen downloaden, zodat luisteren ook zonder verbinding kan.
- Draaien op een echt toestel: de speler is nog nooit hoorbaar getest.
- Afhankelijkheden zijn gepind op versies van eind 2024 en werken; lint meldt
  dat er nieuwere zijn (AGP 9.4 inmiddels).
- `applicationId` staat op `nl.woolacast` — aanpassen naar een domein dat je
  zelf bezit voordat je publiceert.

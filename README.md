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
| Apple Podcasts | nee | shows én afleveringen, 175 landen, alle categorieen |
| Spotify | nee | shows én afleveringen in 26 landen, categorieen in zeven |
| RSS van de podcast zelf | nee | afleveringen, audio, omschrijvingen, artwork |

Beide feeds van Apple lopen vast boven de honderd vermeldingen, dus honderd is
het maximum per lijst.

De podcastpagina leest de **RSS van de maker zelf** — daar staat alles in en er
zit niemand tussen. Dat is dezelfde route die AntennaPod neemt. Alleen bij een
Apple-lijst is er één opzoeking nodig om de feed-URL te vinden, want Apple geeft
een catalogus-id in plaats van een feed. Spotify geeft alleen een
`spotify:show:` uri, dus daar zoekt de app de naam op in diezelfde catalogus.

### Over de categorielijsten van Apple

Apple toont op `podcasts.apple.com/{land}/charts` wél afleveringen per
categorie, maar die lijst komt van `amp-api.podcasts.apple.com` en die geeft
zonder bearer token een 401. De oude rss-generator heeft een endpoint
`toppodcastepisodes` dat een genre accepteert, maar dat levert al jaren een lege
feed op; `?genre=` op de marketing-feed wordt genegeerd (getest: dezelfde
uitslag voor drie verschillende genres).

Wat wel keyless kan, en wat de app doet: de top 100 afleveringen ophalen en zelf
op categorie schiften. Elke aflevering draagt één genre — soms een hoofdgenre
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

| Lijst | Landen | Omvang |
| --- | --- | --- |
| `top-podcasts`, `top-episodes`, `trending` | 26 | 200 |
| 16 categorieen (`comedy`, `news`, `true-crime`, …) | 7: us au br de mx se gb | 50 |

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

| Onderdeel | Status |
| --- | --- |
| Hitlijsten: Apple en Spotify | werkt |
| Offline: laatst opgehaalde lijst | werkt |
| Stijgers en dalers | werkt vanaf de tweede dag |
| Podcastpagina uit RSS | werkt |
| Speler: mini-balk die uitklapt naar volledig scherm | werkt |
| Afspelen (Media3, achtergrond, vergrendelscherm) | gebouwd, nog niet op een toestel getest |
| Volgen + bibliotheek | werkt |
| Ontdek | ingang op de lijsten |
| Chart-tracker | nog niet |
| Afleveringen downloaden voor offline luisteren | nog niet |

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
              player/ het uitklapbare spelerscherm
tools/        svg_to_vector.py — maakt de launcher-iconen uit de tekening
design/       de mockup en het icoon als bewerkbaar canvas
```

Bewust simpel voor een eerste versie: handmatige bedrading in plaats van een
DI-raamwerk, en een JSON-bestand in plaats van een database. Zodra er echte
historie overheen gaat is `LocalStore` het punt om naar Room te verhuizen.

## Nog te doen

- Lettertypen: Bricolage Grotesque en Instrument Sans staan in het ontwerp maar
  nog niet in `res/font/`; de maatvoering in `Type.kt` klopt al wel.
- Afleveringen downloaden, zodat luisteren ook zonder verbinding kan.
- Chart-tracker en chart-alerts.
- Afhankelijkheden zijn gepind op versies van eind 2024 en werken; lint meldt
  dat er nieuwere zijn (AGP 9.4 inmiddels).
- `applicationId` staat op `nl.woolacast` — aanpassen naar een domein dat je
  zelf bezit voordat je publiceert.

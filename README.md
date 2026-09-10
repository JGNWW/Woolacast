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

| Bron | Open? | Sleutel? | Wat het geeft |
| --- | --- | --- | --- |
| Apple Podcasts | publieke feeds | nee | shows én afleveringen, 175 landen, per categorie |
| fyyd.de | open API | nee | shows per taalgebied |
| RSS van de podcast zelf | volledig open | nee | afleveringen, audio, omschrijvingen, artwork |
| Spotify | — | — | geen open weg |
| YouTube | — | — | geen open weg |

De podcastpagina leest de **RSS van de maker zelf** — daar staat alles in en er
zit niemand tussen. Dat is dezelfde route die AntennaPod neemt. Alleen bij een
Apple-lijst is er één opzoeking nodig om de feed-URL te vinden, want Apple geeft
een catalogus-id in plaats van een feed. Bij fyyd komt de feed-URL meteen mee,
dus daar is zelfs die stap niet nodig.

### Over Podchaser

Podchaser heeft de lijsten van Apple én Spotify, dus qua data zou het passen.
Maar hun GraphQL-endpoint antwoordt `Invalid authorization request`: het is een
commerciële data-API met sleutels en voorwaarden over hergebruik. Dat is precies
het tegenovergestelde van geen-cloud-en-open-source. Daarom staat het er niet in.

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
| Hitlijsten: bron, land, categorie, niveau | werkt |
| Offline: laatst opgehaalde lijst | werkt |
| Stijgers en dalers | werkt vanaf de tweede dag |
| Podcastpagina uit RSS | werkt |
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
    apple/    de publieke Apple-feeds
    fyyd/     open API zonder sleutel
    feed/     RSS-parser en -client: de route zonder tussenpersoon
    local/    volgen, momentopnames en de offline cache in één JSON-bestand
  player/     Media3-service met een StateFlow-laag eromheen
  ui/         Compose-schermen, thema, navigatie
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

# Woolacast

Een Android-podcastapp waarvan de kern een hitlijsten-browser is: dezelfde lijst
bekijken per **bron**, **land**, **categorie** en **niveau** (podcast of
aflevering), en zien hoe een show over die lijsten heen beweegt.

## Stand van zaken

Eerste versie. De hitlijsten werken end-to-end op echte data, je kunt een
podcast openen, afleveringen afspelen en shows volgen.

| Onderdeel | Status |
| --- | --- |
| Hitlijsten: bron, land, categorie, niveau | werkt op Apple-data |
| Stijgers en dalers | werkt vanaf de tweede dag (zie hieronder) |
| Podcastpagina met afleveringen | werkt |
| Afspelen (Media3, achtergrond, vergrendelscherm) | gebouwd, nog niet op een toestel getest |
| Volgen + bibliotheek | werkt |
| Ontdek | ingang op de lijsten, nog geen eigen inhoud |
| Chart-tracker (één show over bronnen en landen) | nog niet |

## Waar de data vandaan komt

Alleen publieke, sleutelloze feeds. Alle drie hieronder zijn tijdens het bouwen
tegen de live API gecontroleerd.

| Wat | Endpoint |
| --- | --- |
| Top shows | `rss.marketingtools.apple.com/api/v2/{land}/podcasts/top/{n}/podcasts.json` |
| Top afleveringen | `.../top/{n}/podcast-episodes.json` |
| Top shows per categorie | `itunes.apple.com/{land}/rss/toppodcasts/limit={n}/genre={id}/json` |
| Podcast + afleveringen | `itunes.apple.com/lookup?id={id}&entity=podcastEpisode` |

De lookup geeft een directe audio-URL per aflevering; dat is wat de speler
afspeelt.

**Spotify en YouTube hebben geen open API.** Hun lijsten bestaan alleen als
webpagina en scrapen is tegen hun voorwaarden. Beide bronnen staan wel in de app
met hun echte eigenschappen en een uitleg in plaats van data
(`UnsupportedChartSource`), zodat ze aan te zetten zijn zodra er een weg is.

**Categorieen werken alleen op showniveau.** Apple publiceert geen
afleveringenlijst per categorie. De app zegt dat en biedt aan de filter te
laten vallen, in plaats van een lege lijst te tonen.

## Stijgers en dalers

Er bestaat geen publieke bron voor "gisteren". De app bewaart daarom zelf per
lijst één momentopname per dag en vergelijkt de verse lijst met de meest recente
opname van een *eerdere* dag. Gevolg: **de eerste dag na installatie is er geen
beweging te zien.** Vanaf dag twee wel.

Dat mechanisme is ook de basis onder de chart-tracker die nog moet komen.

## Bouwen

```
./gradlew assembleDebug
```

Vereist de Android SDK (compileSdk 35) en JDK 17.

## Structuur

```
app/src/main/java/nl/woolacast/
  domain/     modellen, ChartQuery, ChartSource, de catalogus met landen en categorieen
  data/       Apple-bronnen, repositories, netwerk, lokale opslag
  player/     Media3-service en een StateFlow-laag eromheen
  ui/         Compose-schermen, thema, navigatie
tools/        svg_to_vector.py — maakt de launcher-iconen uit de tekening
design/       de mockup en het icoon als bewerkbaar canvas
```

Bewust simpel gehouden voor een eerste versie: handmatige bedrading in plaats
van een DI-raamwerk, en een JSON-bestand in plaats van een database. Zodra er
echte historie overheen gaat — grafieken over maanden — is `LocalStore` het
punt om naar Room te verhuizen.

## Nog te doen

- Lettertypen: Bricolage Grotesque en Instrument Sans staan in het ontwerp maar
  nog niet in `res/font/`; de maatvoering in `Type.kt` klopt al wel.
- Chart-tracker: één show over bronnen, landen en tijd.
- Chart-alerts op shows die je volgt.
- `applicationId` staat op `nl.woolacast` — aanpassen naar een domein dat je
  zelf bezit voordat je publiceert.

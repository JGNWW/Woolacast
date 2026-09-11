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

## Trending en Nieuw

Twee extra tabbladen naast Podcasts en Afleveringen.

**Trending.** Spotify publiceert een echte trending-lijst (`/api/charts/trending`,
dezelfde 26 landen als de Top 200). Apple niet: daar toont het tabblad de
snelste stijgers in de Top 200 uit de eigen dagelijkse metingen, dus pas vanaf
de tweede dag.

**Nieuw.** Apple heeft er wél een echte lijst voor: het tabblad "Nieuw" op
podcasts.apple.com is redactioneel, en het eerste blok daarop is "Nieuwe
programma's" — per land een eigen selectie van zo'n 20–32 shows. Die staat
alleen achter de `amp-api` met het bearer-token dat de webpagina zelf in zijn
JavaScript meedraagt. Het klusje vist dat token elke run opnieuw uit de pagina
(`collect.py new`, draait ook mee in `snapshot`) en schrijft
`apple/{land}/26/new.json`. Spotify heeft geen nieuw-lijst; daar toont het
tabblad wie de laatste dagen de Top 200 binnenkwam, gegroepeerd per dag, uit
de eigen metingen.

## Tips van de media

Kranten, omroepen en podcastgidsen tippen podcasts; de app haalt die tips op
en koppelt ze aan de catalogus, zodat je er meteen naartoe kunt. Er is geen
gezamenlijke bron: per medium leest `collect.py tips` de rubriek — een RSS als
die er is, anders de indexpagina van de gids, waarna elk artikel wordt gelezen
op vetgedrukte en aangehaalde titels. Wat overblijft moet **exact** een show in
Apple's catalogus zijn; anders sneuvelt het (een citaat van een geïnterviewde
lijkt anders precies op een titel).

**Welke tip hoort bij welke podcast.** Een recensie noemt onderweg andere
podcasts: eerder werk van dezelfde makers, iets uit hetzelfde genre. Die horen
niet als losse tip in de app, want dan staat er een kop boven die er niet over
gaat. Daarom telt alleen bewijs uit de kop en het webadres van het stuk: een
recensie koppelt aan de podcast die dáár genoemd wordt, en levert niets als die
niet exact in de catalogus staat. Alleen een tiplijst — te herkennen aan "vijf
podcasttips", "de beste kinderpodcasts", "top 10" — mag meerdere podcasts
noemen. De regel onder een tip is de zin uit het artikel waarin die podcast
besproken wordt, niet de eerste alinea van het stuk.

**Beeldmerken.** Per medium wordt één keer het `apple-touch-icon` van de site
opgehaald en bij de gegevens gezet (`charts/logos/`). De app laadt het
daarvandaan en niet bij de uitgever: die hoeft niet te weten wie er in de app
leest. Media die ons blokkeren of alleen een `.ico` aanbieden (Telegraaf, Trouw,
De Tijd, De Standaard, NDR) houden hun beginletters in een gekleurd blokje.

**Elke nacht.** `.github/workflows/charts-tips.yml` draait de ronde dagelijks om
04:40 UTC. Een podcastrubriek verschijnt wekelijks tot dagelijks; wie er een week
overheen laat gaan, laat de app verouderen. Let op: GitHub start een schema
alleen vanaf de standaardtak — zolang dit werk op een aparte tak staat, moet je
hem met de hand starten.

Wat dat oplevert, gemeten:

| Land | Tips | Media |
| --- | --- | --- |
| US | 30 | Podcast Review, Rolling Stone |
| NL | 19 | NOS, NPO, NPO Radio 1, Trouw |
| GB | 18 | Financial Times, NME, The Guardian |
| FR | 15 | Le Figaro, Le Monde, Nouvelobs.com, Radio France |
| DE | 10 | BR, Deutschlandfunk, FAZ, STERN |
| BR | 9 | Folha de S.Paulo, G1, Hugo Gloss, O GLOBO |
| JP | 6 | Natalie.mu, Xtrend.nikkei.com, 産経ニュース |
| BE | 5 | De Standaard, HLN, Humo |
| ES | 3 | Cadena SER, RTVE, elDiario |
| IE | 3 | Hot Press, The Irish Times |
| MX | 3 | EL PAÍS, El Universal |
| CA | 2 | CTV News, The Tyee |
| AU | 1 | news.com.au |
| DK | 1 | Radio4 |
| NO | 1 | NRK |
| IN, IT, SE | 0 | — |

De aantallen liggen lager dan vóór de koppelingsregel, en dat is de bedoeling:
WDR leverde veertien "tips" die menu-items van hun cultuurpagina bleken, la
Repubblica koppelde zijn eigen programmapagina's aan zichzelf. Japan en India
vragen om herkenning van titels in een ander schrift — dat kan de lezer nu niet.

### Een eigen podcast aanprijzen is geen tip

Dit filter zat op de weg via Google Nieuws maar niet op die via de gidsen, en
daar liep veel doorheen. De helft van de Duitse oogst bestond uit omroepen die
hun eigen programma aankondigden ("Die NDR Podcasts nach Radioprogramm",
"F.A.Z. Bücher-Podcast"), en de negentien Ierse tips waren vrijwel allemaal
podcasts van The Irish Times en RTÉ zelf. Nu geldt overal dezelfde regel: is de
uitgever van de show hetzelfde huis als het medium, dan is het een aankondiging.

Twee dingen bleken daarbij nodig. Een afkorting hoort bij de naam die hij
afkort, want BR is de Bayerischer Rundfunk en FAZ de Frankfurter Allgemeine
Zeitung, en die delen geen woord maar wel hun beginletters. En een naam met
puntjes ertussen ("F.A.Z.") herkent geen woordgrens; die reeks wordt eerst
aaneengeplakt.

Omgekeerd mag een los woord uit de naam van een medium niet meetellen. "The
Irish Times" deelt "Irish" met half Ierland en "Guardian Australia" deelt
"Australia" met half Australië: op dat woord filterden we juist de echte tips
weg. Alleen de volledige naam telt, of een naam die uit één woord bestaat.

Het totaal zakt daarmee van 168 naar 126. Dat is de bedoeling: wat wegviel was
geen tip.

### Welke zoekopdracht werkt echt

De algemene zoekopdrachten zijn niet bedacht maar gemeten. Per taal is een
handvol kandidaten uitgeprobeerd en geteld hoeveel artikelen ze opleveren en
hoeveel daarvan echt een podcast koppelen. Wat niets gaf en ook nauwelijks
artikelen teruggaf, staat er niet in: elke vraag kost een verzoek.

Er zijn twee vormen, en het verschil tussen die twee bleek groter dan het
verschil tussen de woorden. Een exacte zin tussen aanhalingstekens
(`"beste podcasts"`) vindt de rubriek die precies zo heet. Losse woorden naast
elkaar (`podcast recensie`) zoeken allebei de woorden ergens in het stuk, en
vinden zo de recensie die zijn kop anders noemt. In het Nederlands is dat het
verschil tussen één en zes koppelingen, en het bracht Trouw en de Volkskrant
binnen die de exacte zin miste. In het Duits gaf `podcast rezension` er drie waar
`"Podcast-Rezension"` er nul gaf, met Der Spiegel erbij. In het Frans deed geen
enkele exacte zin iets en geven de losse woorden er vijf.

Dat leverde per taal een andere winnaar op. In het Duits is `"beste Podcasts"`
vrijwel dood (één artikel in drie maanden) terwijl `"Podcast-Tipps"` er 31 geeft;
in het Nederlands is het precies andersom. `"podcast van de week"` klinkt goed maar
gaf één artikel, `"podcastserie"` gaf er 48. Duitsland ging van elf naar zeventien
tips toen de lijst op de meting werd aangepast.

Onderweg kwam een fout boven die hele talen kostte: elke taal zet zijn eigen
tekens om een titel. Wij kenden ‘zo’ en “zo”, maar niet het Duitse „zo“, het
Zweedse ”zo” of het Franse «zo». In die talen zag de lezer dus nooit een titel
staan. Dat is nu recht.

En `when:90d` doet wat het belooft: de oudste treffer in een ronde is 89 dagen
oud. Bij een zoekopdracht op een grote krant komen we wel tegen de honderd
artikelen per vraag aan die Google teruggeeft.

### Alle achttien landen even breed

Elk land heeft nu twaalf grote titels in de lijst en vijf algemene zoekopdrachten
in de eigen taal, van `"beste podcasts"` tot `"migliori podcast"` en
`"podkastanbefaling"`. Daarbovenop krijgt elke titel een eigen zoekopdracht.

Japan deed eerst niet mee omdat de regels het schrift niet aankonden. Drie dingen
stonden in de weg: een titel staat daar tussen 「 en 」 in plaats van tussen
aanhalingstekens, Japans kent geen hoofdletters (waardoor de regel "helemaal in
hoofdletters is een rubriekskop" altijd toesloeg), en het woord podcast heet daar
ポッドキャスト. Alle drie zijn opgelost, en Japan staat nu op vijf tips van
Natalie, Mainichi, Sankei en Nikkei.

Wat daarbij bovenkwam: Japanse kranten drukken podcast-persberichten af alsof het
artikelen zijn. Een kop die met プレスリリース begint is geen tip, en dat geldt net
zo goed voor "Persbericht", "Pressemitteilung" en "comunicado de prensa".

Italië en Zweden blijven op nul. Daar noemen de koppen de podcast niet bij naam en
zetten de gidsen hun keuzes alleen in het artikel, waar de verzamelaar bij hun
rubrieken niet binnenkomt.

### Google Nieuws

Google Nieuws bundelt wat we anders per medium moeten najagen, en komt ook
binnen bij titels die hun artikelen achter een toestemmingsscherm zetten: de
kop staat in de feed. Per land draaien een handvol zoekopdrachten in de eigen
taal ("beste podcasts", "Podcast-Tipps", "mejores podcasts"), plus één
zoekopdracht over de grote titels van dat land.

De feed geeft alleen kop, medium, datum en een doorverwijslink. Geen
samenvatting, en de link komt niet bij de uitgever uit: Google verwijst met
JavaScript door en het echte adres zit niet leesbaar in de link. Koppelen moet
dus op de kop alleen, en daarom is de regel streng. Een treffer moet aan alles
voldoen:

- het medium staat op de lijst grote titels van dat land;
- de kop bevat het woord podcast;
- de titel staat tussen aanhalingstekens en vlak bij dat woord;
- het is niet het medium dat zijn eigen aflevering aankondigt.

Die laatste kostte de meeste moeite. "Norwich City: Podcast" van de BBC is geen
tip, en "SZ-Podcast: Hey München" evenmin.

In het Nederlands, Vlaams en Spaans werkt dit: daar noemt een recensiekop de
podcast, en zo komen Trouw, De Standaard en Cadena SER binnen, die we zelf niet
kunnen lezen. In het Engels levert het niets, want daar beschrijft de kop de
podcast in plaats van hem te noemen ("best podcasts of the week").

Over robots.txt: `news.google.com` verbiedt alles behalve de voorpagina. Die
regel staat er voor crawlers die de site afstruinen; `/rss/search` is een
syndicatie-ingang die Google zelf aan feedlezers aanbiedt, en zo gebruiken we
hem: een handvol verzoeken per dag, onder onze eigen naam, zonder browser na te
doen. Dat is een bewuste keuze, vastgelegd in `ROBOTS_EXCEPTIONS` met uitleg.
Wie die regel weghaalt, zet de bron stil.

### Waar we níet langs komen

De DPG-titels (Volkskrant, Trouw, Parool, AD) publiceren hun podcastrubriek niet
in een feed en hun pagina's staan achter een toestemmingsscherm. Via Google
Nieuws vangen we hun koppen wel op, maar het artikel zelf lezen we niet, en om
dat scherm heen gaan we niet.

### De app leest ook zelf

De verzamelaar draait een keer per nacht. Wie 's ochtends kijkt ziet dus wat er
gisteren was, en een podcastrubriek verschijnt juist vaak 's ochtends. Daarom
leest de app de feeds ook zelf, bij het openen van het tipsscherm en met de knop
rechtsboven.

Waar hij moet kijken staat in `charts/feeds/{land}.json`: per land een handvol
feeds met hun soort (`guide`, `news`, `google`) en de lijst media die in dat land
meetellen. Dat is met opzet gegevens en geen code — een bron erbij vraagt geen
nieuwe versie van de app.

Wat de app niet doet is het artikel achter de kop lezen. Dat werk verschilt per
site, breekt het vaakst, en een reparatie daar is een nieuw gegevensbestand in
plaats van een nieuwe versie in de winkel. De app ziet alleen wat de feed geeft,
en de regels (`TipRules`) zijn daarom strenger. Twee ervan dragen het meeste:

- De opsomming onder een tiplijst. "Met deze week: Luister Anita, Dan Taberski's
  Manifesto, Proces X en De onderwereld" is de enige plek waar een feed zijn
  besproken podcasts bij naam noemt.
- De naam achter het woord podcast zonder aanhalingstekens, woord voor woord
  langer geprobeerd. Waar de zin ophoudt en de titel begint is van buiten niet te
  zien, dus laat Apple's catalogus beslissen welke lengte bestaat.

Voor Nederland levert een verversing dertien tips op. Wat de verzamelaar vond
gaat voor bij het samenvoegen: daar is het artikel bij gelezen.

Een intro per artikel ophalen (`og:description`) is gebouwd en weer weggehaald:
28 artikelen in vier landen, nul extra koppelingen. De feeds die er toe doen
geven hun opsomming al mee, en de rest staat achter een toestemmingsscherm of is
geen tip.

### Zelf zoeken: de prospector

`collect.py prospect --countries de` loopt de grote media van een land af en
probeert een handvol paden waarachter een podcastrubriek pleegt te zitten
(`/podcast`, `/tag/podcast`, `/podcast-tipps`, `/poddar`, …). Wat bereikbaar is
wordt meteen getoetst: hoeveel van de genoemde titels zijn echt terug te vinden
in Apple's catalogus? Wat scoort komt eruit als een regel die je in
`TIP_SOURCES` kunt plakken. Zo hoeft de lijst media niet met de hand bijgehouden
te worden — alleen wélke titels in een land meetellen blijft een keuze.

Het is een verkenner, geen dagelijkse klus: hij doet honderden verzoeken en
hoort af en toe te draaien, niet elke ochtend.

### Wat we niet doen

De verzamelaar leest `robots.txt` van elke host en houdt zich eraan, en stelt
zich voor als `Woolacast-charts/1.0` met een adres erbij. Geen browser nadoen,
geen 403 omzeilen: wie ons niet wil, krijgt ons niet. Dat kost bereik —
Google News heeft bijvoorbeeld een prima RSS-zoekfunctie die per land en taal
werkt, maar hun `robots.txt` verbiedt `/rss/` voor iedereen, dus die route ligt
dicht. Hetzelfde geldt voor de tagpagina's van DPG-titels en De Standaard.

De verdeling is scheef en dat is geen bug: **alleen een echte podcastrubriek
levert wat op.** Een gewone cultuurfeed uitkammen op het woord "podcast" gaf
in vrijwel elk land nul bruikbare tips; de prospector zoekt daarom naar de
rubriek zelf. Wat niet lukte: de tagpagina's van DPG-titels (Volkskrant,
Parool, De Morgen, HLN) staan achter een firewall die automatische lezers
weert, en in Zweden, Denemarken, Ierland, Canada, Mexico, Japan en India had
geen van de grote titels een rubriek op een vindbaar pad.

Het koppelen is streng, want anders glipt van alles erdoor. Een kandidaat moet
**exact** een show in Apple's catalogus zijn, met een vergelijking die letters
uit elk schrift bewaart (anders valt "Подкаст Asia Pacific" samen met "Asia
Pacific"). Een naam van één woord telt alleen als hij in het artikel binnen
honderd tekens van het woord "podcast" staat — zo overleeft "Serial" wel, maar
sneuvelt een rubriekskop als "Deutschland".

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
| Trending: Spotify's eigen lijst; bij Apple de snelste stijgers uit eigen metingen | werkt (Apple vanaf dag twee) |
| Nieuw: Apple's redactionele "Nieuwe programma's" per land; bij Spotify de binnenkomers uit eigen metingen | werkt (Spotify vanaf dag twee) |
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
| Tips van de media: op Ontdek, als eigen scherm, en op de podcastpagina | werkt in 13 landen |

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
              en Apple's redactionele lijst "Nieuwe programma's", plus de
              podcasttips van kranten, omroepen en gidsen
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

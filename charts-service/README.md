# charts-service

Verzamelt alleen wat de app niet zelf kan ophalen. Twee klussen, met heel
verschillende kosten.

## snapshot — goedkoop, mag overal draaien

Legt de ranglijsten van Apple en Spotify vast. Twee aanroepen per lijst;
achttien landen kost een paar honderd aanroepen en zo'n tien minuten.

Dit bestaat **niet** om de lijst zelf — die haalt de app in één of twee
aanroepen op. Het bestaat om de **historie**: stijgers, dalers en de tracker
kun je niet met terugwerkende kracht bepalen. Wie gisteren niet vastlegde,
heeft gisteren niet.

Er wordt daarom ook alleen bewaard wat onherhaalbaar is: de rangen per dag, en
een lijstje met de grootste stijgers. Geen titels of artwork — die weet de app
zelf al.

## episodes — duur, houd het klein

Apple's afleveringenlijst per categorie. Die is publiek, maar geeft alleen ids,
en een afleverings-id is nergens massaal op te lossen: elk id kost een eigen
aanroep. Twintig categorieën van vijftig is duizend aanroepen per land.

Dit is het enige waar de volledige lijst wél wordt bewaard, want de app kan hem
niet zelf ophalen.

De app leest alleen. Er draait geen server, er is geen sleutel en er is geen
account nodig.

## Hoe je erbij komt

```
https://raw.githubusercontent.com/JGNWW/Woolacast/claude/android-podcast-app-mockup-weuhxw/charts-service/charts/index.json
https://raw.githubusercontent.com/JGNWW/Woolacast/claude/android-podcast-app-mockup-weuhxw/charts-service/charts/nl/1303/episodes.json
```

De repo moet publiek zijn; raw geeft privérepo's niet zonder token vrij.

`index.json` noemt alles wat er staat, met per lijst welke dagen er zijn
vastgelegd. De bestanden:

- `{bron}/{land}/{genre}/{niveau}.history.json` — rang per id per dag
- `{bron}/{land}/{genre}/{niveau}.json` — de volledige lijst, alleen waar de
  app hem niet zelf kan ophalen
- `movers/{land}.json` — de grootste stijgers van vandaag, klaar om te tonen

Een lijstbestand ziet er zo uit:

```json
{
  "source": "apple",
  "level": "episodes",
  "country": "nl",
  "genreId": 1303,
  "genreLabel": "Comedy",
  "updated": "2026-09-10T12:30:47Z",
  "requested": 50,
  "resolved": 50,
  "entries": [
    {
      "rank": 1,
      "id": "1000788592718",
      "title": "De balzak rugzak",
      "showId": "1740313422",
      "showTitle": "Chantal & Tina",
      "publisher": "Tonny Media",
      "artworkUrl": "https://…/600x600bb.jpg",
      "feedUrl": "https://…/podcast.rss",
      "durationMs": 1826000,
      "releaseDate": "2026-09-09",
      "episodeNumber": 121
    }
  ]
}
```

`feedUrl` zit erbij, zodat een lezer meteen naar de RSS van de podcast kan.

## Zelf draaien

```
python3 collect.py --country nl --limit 50 --out charts
```

Alleen de standaardbibliotheek, geen installatie nodig.

## Wat dit kost

| Klus | Aanroepen | Duur | Dagelijks draaien? |
| --- | --- | --- | --- |
| `snapshot`, 18 landen | ~800 | ~10 min | ja, dat is waar hij voor is |
| `episodes`, 1 land | ~1000 | ~4 min | kan, maar houd het bij één of twee landen |

Beide workflows staan **op handmatig**. Bij `snapshot` staat de cron als
commentaar klaar; dat is de klus die het waard is om aan te zetten. Bij
`episodes` staat er bewust geen: meer landen is lineair meer verkeer richting
Apple, en dat is verkeer dat zij niet gevraagd hebben.

De eerste run levert nog geen stijgers op — er is dan nog geen gisteren.

De ranglijst zelf is openbaar en vraagt geen sleutel, maar geautomatiseerd
ophalen is niet iets waar Apple's voorwaarden om staan te springen. Voor een
klein, open project is het risico dat het een keer stopt. Ga je dit groot
uitrollen, kijk er dan eerst goed naar.

## Herkomst van de gegevens

Die komen van Apple en zijn van hen. Deze map is een doorgeefluik, geen eigenaar.

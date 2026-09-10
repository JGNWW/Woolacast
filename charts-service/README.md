# charts-service

Apple's echte afleveringen-ranglijst per categorie, als platte JSON.

Deze repo bestaat om één reden: die lijst is publiek, maar niet in bulk op te
halen. De ranglijst geeft alleen ids, en een afleverings-id is nergens
massaal op te lossen — elk id kost een eigen aanroep. Een telefoon kan dat niet
per lijst doen. Een klusje dat af en toe draait wel.

De app leest hier alleen van. Er draait geen server, er is geen sleutel en er is
geen account nodig.

## Hoe je erbij komt

```
https://raw.githubusercontent.com/JGNWW/Woolacast/claude/android-podcast-app-mockup-weuhxw/charts-service/charts/index.json
https://raw.githubusercontent.com/JGNWW/Woolacast/claude/android-podcast-app-mockup-weuhxw/charts-service/charts/nl/1303/episodes.json
```

De repo moet publiek zijn; raw geeft privérepo's niet zonder token vrij.

`index.json` noemt alles wat er staat. De lijstbestanden zien er zo uit:

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

Elke aflevering is één aanroep naar `podcasts.apple.com`. Met zes tegelijk kost
een categorie van vijftig ongeveer vijftien seconden; twintig categorieën voor
één land dus zo'n vijf minuten en duizend aanroepen.

Daarom staat de workflow **op handmatig**. Zet de cron pas aan als je die
afweging bewust maakt, en houd het bescheiden — één land, top vijftig, één keer
per dag. Meer landen erbij betekent lineair meer verkeer richting Apple, en dat
is verkeer dat zij niet gevraagd hebben.

De ranglijst zelf is openbaar en vraagt geen sleutel, maar geautomatiseerd
ophalen is niet iets waar Apple's voorwaarden om staan te springen. Voor een
klein, open project is het risico dat het een keer stopt. Ga je dit groot
uitrollen, kijk er dan eerst goed naar.

## Herkomst van de gegevens

Die komen van Apple en zijn van hen. Deze map is een doorgeefluik, geen eigenaar.

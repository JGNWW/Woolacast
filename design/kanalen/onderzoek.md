# Kanalen — onderzoek

Vraag: een kanaalachtige functie, waarmee je podcasts per bron (maker, netwerk,
uitgever) ziet, en die kunt ordenen op populair, recent en dergelijke.

## Hoe anderen het doen

| App | Wat er is | Ordening |
| --- | --- | --- |
| **Apple Podcasts** | *Kanalen*: een maker bundelt zijn shows op één pagina, met logo, achtergrondkleur, beschrijving en eventueel een abonnement. Volg je een show uit een kanaal, dan verschijnt het kanaal in je Bibliotheek. | Vaste secties, geen sorteerknop: *Nieuwe programma's*, *Topprogramma's*, *Topafleveringen*, en een lijst met alles. |
| **YouTube** | Het kanaal is de kern. Tabbladen (Home, Video's, Podcasts, Playlists); abonneren. | Chips boven de lijst: **Nieuwste · Populair · Oudste**. Nieuwste is standaard. |
| **YouTube Music** | Podcastpagina's met sorteren. | *Nieuwste eerst · Oudste eerst · Populairst*. |
| **Pocket Casts** | *Networks* op Ontdek, door redactie samengesteld (Pushkin, TED, NYT, Vox …). Wordt nu uitgebreid: netwerken in de zoekresultaten, met een eigen filterpil, en de makersregel op de podcastpagina wordt een link naar het netwerk (open broncode, Android-PR's #5860 en #5864). | Redactionele volgorde. |
| **Podchaser** | *Networks* (bèta) met alle podcasts van een netwerk. | Sorteren via een keuzelijst. |
| **Podtrac / Podscribe** | Ranglijsten van *uitgevers* in plaats van shows (maandelijks, VS). | Bereik. |
| **NPO Luister** | Eigen app voor al het NPO-aanbod, ingedeeld op categorie en thema. Heeft ook een Apple-kanaal met 217 programma's. | Redactioneel. |
| **Spotify** | Geen kanaalpagina voor podcastmakers gevonden; de maker staat als tekst onder de titel. De hitlijsten geven wel `showPublisher` mee. | — |

Lessen:

1. **Apple** laat zien dat een kanaal een *merk* is (logo, kleur) en dat drie
   ingangen volstaan: wat nieuw is, wat populair is, en de afleveringen.
2. **YouTube** laat zien dat sorteren met drie chips boven één lijst begrijpelijker
   is dan losse secties, zodra een kanaal groot is.
3. **Pocket Casts** kiest precies de twee ingangen die wij ook hebben: de makersregel
   op de podcastpagina, en zoeken. Een netwerk krijgt daar ronde artwork om het van een
   podcast te onderscheiden.
4. Uitgeversranglijsten (**Podtrac**) bestaan, maar alleen voor de branche. Een
   ranglijst van makers per land en categorie, gratis en per dag, heeft niemand.
   Dat is het gat voor een hitlijstenapp.

## Wat de data toelaat (gemeten op 30 september 2026)

- **Apple kent kanalen, en ze zijn te lezen.** De amp-api met het webtoken dat
  `collect.py` al ophaalt geeft per kanaal: naam, logo, achtergrondkleur,
  beschrijving, `showCount` en drie lijsten: `view/top-shows`, `view/new-shows` en
  `view/top-episodes`. Een podcast wijst naar zijn kanaal via `?include=channel`.
  Dit zijn precies de drie secties van Apple's kanaalpagina.
- **Een derde van de lijst hangt aan een kanaal.** Van de Nederlandse Top 200
  (Apple, alle categorieën) hebben 67 shows een Apple-kanaal, verdeeld over 25
  kanalen. Het grootste: NPO Luister met 24 shows in de lijst. Dan NRC (7),
  de Volkskrant (4), AD, Tonny Media en Dag en Nacht (3).
- **De rest heeft alleen een makersnaam**, en die is rommelig: `NPO Luister /
  BNNVARA`, `Dag en Nacht | Podimo`, `Podimo & Alexander Klöpping`. BNR (9 shows)
  en De Telegraaf (7) hebben geen kanaal maar zijn wel grote makers. Een
  kanaalfunctie heeft dus een terugval nodig op de makersnaam, met normalisatie
  (splitsen op `/`, `|`, `&`).
- **Spotify** geeft per show `showPublisher` in dezelfde aanroep als de lijst.
- **Recent** is gratis: het zoek- en opzoekresultaat van Apple bevat
  `releaseDate` van de nieuwste aflevering en `trackCount`.
- **Populair** kan op drie manieren, en het concept moet zeggen welke:
  1. Apple's eigen volgorde in `top-shows` (ondoorzichtig, alleen voor kanalen);
  2. de hoogste plek in de hitlijsten die de app al heeft (per bron, land,
     categorie) — dit is wat Toadcast onderscheidt;
  3. beweging uit de eigen dagelijkse momentopnames.
- **Wat er al is in de app:** `MakerScreen` toont alles van één maker, via een
  zoekopdracht op de makersnaam (`SearchRepository.byMaker`). De makersregel op
  de podcastpagina is al een link. Er is geen sortering, geen volgen en geen
  kanaalbeeld.

## Bronnen

- Apple: [Create a channel](https://podcasters.apple.com/support/886-create-a-channel), [Set up a channel and subscription](https://podcasters.apple.com/support/4118-set-up-channel-subscription), kanaal [NPO Luister](https://podcasts.apple.com/nl/channel/npo-luister/id6743084397)
- YouTube: [sorteren op oudste keert terug](https://www.androidpolice.com/youtube-oldest-sorting-coming-back/), [Podcasts-tabblad op kanalen](https://9to5google.com/2023/04/04/youtube-channel-podcasts-tab/), [YouTube Music sorteert podcasts](https://9to5google.com/2024/04/24/youtube-music-podcast-sort/)
- Pocket Casts: [PR #5864 netwerken in zoeken](https://github.com/Automattic/pocket-casts-android/pull/5864), [PR #5860 netwerk per podcast](https://github.com/Automattic/pocket-casts-android/pull/5860), [Discover](https://pocketcasts.com/discover)
- Podchaser: [Networks](https://www.podchaser.com/networks)
- Podscribe: [Top Podcast Publishers](https://podscribe.com/top-publishers)
- NPO: [NPO Luister wordt volwaardig audioplatform](https://npo.nl/pers/persberichten-en-publicaties/npo-luister-wordt-volwaardig-audioplatform-en-lanceert-nieuwe-app)

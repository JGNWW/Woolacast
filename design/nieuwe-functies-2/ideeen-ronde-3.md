# Nieuwe functies, tweede reeks — idee ronde 3

Ter vervanging van het afgekeurde idee 12 (Apple en Spotify naast elkaar).
Genummerd als 13.

## 13. Zoeken in de afleveringen van één show

Op de podcastpagina staat nu een lijst van twintig afleveringen met "alles tonen"
eronder (`DetailScreen`, `allEpisodes`). Wie bij een show met honderden
afleveringen die ene aflevering zoekt ("die met Sanne Kuipers", "de aflevering over
de Afsluitdijk"), moet scrollen. Het zoekscherm van Toadcast zoekt in de
catalogus van Apple, niet in één feed.

Wat het doet: een vergrootglas in de kop van de afleveringenlijst. Typen filtert
meteen de afleveringen van deze show op titel en omschrijving (shownotes), met het
gevonden woord gemarkeerd en het aantal treffers ("12 afleveringen met
*kuipers*"). Sorteren nieuwste of oudste eerst blijft werken. Tik = de aflevering,
zoals altijd.

Hoe: de feed is al opgehaald en geparsed voor de podcastpagina; dit is filteren in
het geheugen, zonder netwerk en zonder server. HTML uit de omschrijving wordt eerst
platte tekst (`Html.kt` bestaat). Hoofdletters en accenten tellen niet mee
("cafe" vindt "café").

Grens: een feed toont soms maar een deel van het archief (bijvoorbeeld de laatste
300). Dan zegt de pagina onder de treffers "Gezocht in de 300 afleveringen in de
feed", zodat niemand denkt dat een oudere aflevering niet bestaat.

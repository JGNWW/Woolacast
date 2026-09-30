# Dekking van transcripties en hoofdstukken

De criticus vroeg eerst te meten hoeveel afleveringen in de top 200 een
transcriptie meeleveren, want de app transcribeert niet zelf. Gemeten op
30 september 2026 met `python3 tools/extras_coverage.py nl us`: Apple, alle
categorieën, de nieuwste vijf afleveringen per show.

| Land | Shows met feed | Gelezen | Show met transcript (nieuwste afl.) | Afleveringen met transcript | Show met hoofdstukken | Afleveringen met hoofdstukken |
| --- | --- | --- | --- | --- | --- | --- |
| NL | 199 van 200 | 199 | 13 (7%) | 54 van 982 (5%) | 9 (5%) | 39 van 982 (4%) |
| US | 198 van 200 | 197 | 27 (14%) | 135 van 972 (14%) | 1 (1%) | 4 van 972 (0%) |

Hoofdstukken in de ID3-kop van het mp3-bestand tellen hier niet mee; die leest de
app pas bij het afspelen, dus in het echt ligt dat getal hoger.

Wat dit betekent: in Nederland zit meelezen onder de grens van 10% die de
criticus noemde, in de VS erboven. Het is toch nu gebouwd, omdat daar expliciet
om gevraagd werd. De app toont het tabblad Tekst alleen als er tekst is, dus wie
geen transcripties tegenkomt, ziet er niets van.

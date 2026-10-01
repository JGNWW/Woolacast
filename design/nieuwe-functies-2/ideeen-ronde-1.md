# Nieuwe functies, tweede reeks — ideeën ronde 1

Stand van de app op 1 oktober 2026: hitlijsten (Apple en Spotify), trending,
nieuw, tips van de media, makers, aanbevelingen op het toestel, chart-tracker,
chart-alerts, volgen, wachtrij, bewaren, snelheid, slaaptimer (minuten, einde
aflevering, einde hoofdstuk), downloaden, hoofdstukken, meelezen, OPML.

Al goedgekeurd in de eerste reeks en dus niet opnieuw: downloaden, hoofdstukken,
meelezen, stilte inkorten en stemversterking, weekoverzicht, terugblik, OPML,
hitlijstradio, Android Auto, instellingen per show.

## 1. Nieuw-inbox met meldingen bij een nieuwe aflevering

Eén lijst **Nieuw** bovenaan de Bibliotheek met alle nieuwe afleveringen van
gevolgde shows, nieuwste eerst, gegroepeerd per dag. Drie vaste chips:
*Onbeluisterd · Kort < 30 min · Deze week*, en één knop *Alles in de wachtrij*.
Vegen naar rechts zet een aflevering in de wachtrij, naar links haalt hem uit de
lijst. Per show een schakelaar *Melding bij een nieuwe aflevering* (standaard uit;
bij volgen één keer gevraagd). Eén melding per ronde, gebundeld ("3 nieuwe
afleveringen: …"), met de knoppen *Afspelen* en *In de wachtrij*.

Hoe: een periodieke WorkManager-taak (elke 4 uur, alleen met netwerk) haalt de
feeds van gevolgde shows op met `If-None-Match` / `If-Modified-Since`, zodat een
ongewijzigde feed een 304 is. De Bibliotheek leest de feeds nu al voor "2 nieuw";
dit voegt ze samen. Dit is de herkansing van het afgekeurde idee "slimme lijsten".

## 2. Bladwijzers en clips

In de speler een bladwijzerknop (ook in de mediamelding): legt het tijdstip vast,
met een optionele notitie van één regel. Per aflevering een lijstje bladwijzers
onder het hoofdstukkenlijstje; in de Bibliotheek een tabblad *Bladwijzers* over
alle afleveringen. Tik = afspelen vanaf dat punt.

Delen kan op twee manieren: als link met tijdstip ("vanaf 23:14") en als
**clip**: kies een stuk van hooguit 60 seconden, de app knipt het uit de audio
(Media3 Transformer, op het toestel) en deelt een m4a met de hoes erbij.

## 3. Casten naar Chromecast en slimme speakers

Een cast-knop rechtsboven in de speler. De speaker haalt de audio zelf op bij de
maker (de URL uit de feed), de telefoon wordt de afstandsbediening. Snelheid,
−15/+30, wachtrij en voortgang blijven werken; stopt het casten, dan gaat de
telefoon verder waar de speaker was. Met Media3 `CastPlayer` en de Google
Cast-SDK. Omdat die SDK Play-diensten nodig heeft: een aparte build zonder cast
voor F-Droid, zoals AntennaPod.

## 4. Luistergeschiedenis

Een lijst van alles wat je gehoord hebt, per dag gegroepeerd: hoes, titel, show,
hoe ver je kwam en hoe lang je luisterde. Tik = verder waar je was. Zoeken in de
geschiedenis, per item verwijderen, alles wissen, en een schakelaar om het
bijhouden uit te zetten. Volledig lokaal. Dit is ook de datalaag die de
Terugblik uit de eerste reeks nodig heeft (geluisterde tijd per dag per show).

## 5. Waarom stijgt dit? — de aanleiding bij een sprong

Bij een stijger (10 plaatsen of meer, of een binnenkomer) zet de app één regel
met de aanleiding die hij zelf kan zien:

- *Nieuwe aflevering op ma 29 sep* — uit de RSS van de show;
- *Getipt door NRC op 28 sep* — uit de bestaande tips van de media;
- *Stond eerst hoog in de VS* — uit de momentopnames van andere landen;
- *In Apple's selectie "Nieuwe programma's"* — uit `new.json`;
- *Nieuwe show van NPO Luister* — uit de makers.

In de chart-tracker staan dezelfde aanleidingen als stipjes op de grafiek, zodat
je ziet dat de sprong samenvalt met die tip of die aflevering. Geen aanleiding
gevonden = geen regel. Alles op het toestel uit data die er al is.

## 6. Deelkaart van een notering

Vanuit de chart-tracker of het menu van een show: een afbeelding met hoes,
plek, lijst, land, bron, datum, beweging en een mini-grafiek van 30 dagen
("#3 in Comedy · Nederland · Apple · 1 okt 2026 · ↑12"). Formaten vierkant en
staand (story). Op het toestel gemaakt, gedeeld via het deelmenu van Android.

Voor wie: makers, die sinds Chartable in 2024 stopte geen gratis plek meer hebben
om hun notering bij te houden en te delen, en fans die een show willen aanraden.
Elke gedeelde kaart draagt klein de naam van de app.

## 7. Volledige back-up en herstel

OPML bewaart alleen wat je volgt. Een back-up bewaart alles: gevolgde shows en
makers, wachtrij, bewaard, voortgang, beluisterd, bladwijzers, instellingen per
show, en de eigen momentopnames van de hitlijsten (de historie die niet terug te
halen is). Eén bestand (`.toadcast`, een zip met JSON).

Handmatig, of automatisch elke week naar een map die je kiest via het
Android-bestandsmenu (Storage Access Framework). Die map kan op het toestel staan
of bij Google Drive, Nextcloud of Dropbox als die app een documentprovider
levert. Bewaart de laatste vijf. Herstellen kan bij de eerste start ("Heb je een
back-up?") en vanuit Instellingen, met een samenvatting vooraf (wat er
overschreven wordt). Geen account, geen server van ons.

## 8. Een slaaptimer die meedenkt

De slaaptimer bestaat al. Dit maakt hem af:

- de laatste 30 seconden zacht uitfaden in plaats van hard stoppen;
- schudden met de telefoon zet de timer terug op de gekozen tijd (een korte
  tril bevestigt het), zodat je niet hoeft te kijken;
- automatisch: "zet de timer elke avond aan tussen 22:00 en 06:00";
- bij het stoppen 30 seconden terugspoelen, zodat je de volgende keer niet midden
  in een zin begint.

Elk onderdeel apart aan te zetten in het slaaptimermenu.

## 9. Seriële podcasts: begin bij aflevering 1

Feeds geven met `<itunes:type>serial</itunes:type>` aan dat een show een verhaal
in delen is (true crime, fictie, geschiedenisreeksen). De podcastpagina toont die
shows dan oudste eerst, met *Begin bij aflevering 1* als grote knop in plaats van
de nieuwste, en `<itunes:season>` / `<itunes:episode>` als seizoenskiezer. Volg je
een seriële show, dan zet de wachtrij na een aflevering de volgende in de reeks
klaar in plaats van de nieuwste.

## 10. Proefluisteren vanuit de hitlijst

Lang drukken op een show of aflevering in een hitlijst speelt een fragment van 60
seconden, zonder de wachtrij of je voortgang aan te raken. Het fragment komt uit
`podcast:soundbite` als de maker het levert; anders uit de nieuwste aflevering,
na het eerste hoofdstuk of na minuut 5 (om intro en reclame over te slaan).
Loslaten = stoppen; omhoog vegen = de hele aflevering afspelen.

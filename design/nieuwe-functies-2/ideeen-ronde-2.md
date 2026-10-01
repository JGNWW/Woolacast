# Nieuwe functies, tweede reeks — ideeën ronde 2

Ter vervanging van de afgekeurde ideeën 6 (deelkaart) en 10 (proefluisteren).
Genummerd als 11 en 12.

## 11. Spoelknoppen instellen en bediening met de koptelefoon

Nu staan terugspoelen en doorspoelen vast op −15 en +30 seconden
(`SKIP_BACK_MS`, `SKIP_FORWARD_MS` in `PlayerController`). In Instellingen →
Afspelen kies je beide zelf: terug 5, 10, 15 of 30 seconden; vooruit 10, 15, 30,
45 of 60 seconden. De knoppen in de speler, de mediamelding, het vergrendelscherm
en straks de auto tonen het gekozen getal.

Daarbij één keuze voor de koptelefoon: wat dubbel en driedubbel tikken doen.
Standaard zoals Android het doet (volgende / vorige aflevering); als optie
*doorspoelen / terugspoelen* met de gekozen stappen, wat bij een podcast vaak
handiger is dan een hele aflevering overslaan.

Hoe: `setSeekBackIncrementMs` en `setSeekForwardIncrementMs` van Media3 met de
gekozen waarde; de iconen krijgen het getal als tekst. Koptelefoonknoppen via
`MediaSession.Callback.onMediaButtonEvent`.

## 12. Apple en Spotify naast elkaar

Op Hitlijsten een knop *Vergelijk* die dezelfde lijst (land, categorie, niveau)
van Apple en Spotify naast elkaar zet. Drie groepen:

- **In beide** — met de twee plekken naast elkaar ("#3 Apple · #11 Spotify");
- **Alleen Apple**;
- **Alleen Spotify**.

Bovenaan één regel: "14 van de top 50 staan in beide lijsten". Tik op een show
opent de podcastpagina zoals altijd. Het koppelen gaat op titel en maker, met
dezelfde regel die de podcastpagina nu gebruikt om een Spotify-show in Apple's
catalogus te vinden. Wat niet te koppelen is, staat onder "Alleen Spotify" met een
klein *niet gekoppeld*.

Waarom: Toadcast is de enige app die beide bronnen heeft. Apple en Spotify meten
iets anders (Apple vooral nieuwe volgers, Spotify ook luisteren), dus het verschil
zegt iets: een show die alleen bij Spotify hoog staat, is vaak een videoshow of een
Spotify-exclusief. Alles uit lijsten die de app al ophaalt; geen server.

// Het verloop van de beoordeling per concept, zoals de criticus het gaf.
export const VERDICTS = {
  c1: { rounds: [
    { ok: false, text: 'De chip "Nieuw" had geen data (geen startdatum bij makers zonder kanaal) en botste met het tabblad Nieuw. Populair was niet gedefinieerd voor shows buiten de lijst. Er waren eigen chips, knoppen van 40px en compacte rijen in plaats van de componenten van de app, en de eyebrow op de gekleurde kop haalde 2,47:1.' },
    { ok: true, text: 'Drie chips (Populair, Recent, A–Z), Populair vast op alle categorieën met een tikbare toelichting, een tussenkop voor shows buiten de lijst, rijen van 72px, één primaire knop en AA op de gloed (≥4,58:1). Daarna verwerkt: de koppeling van Spotify naar Apple benoemd, "Volgt" met rand en vinkje, donkere kanaallogo\'s geklemd tot 3,2:1.' }
  ] },
  c2: { rounds: [
    { ok: false, text: 'Vijf tabbladen passen niet op 390px. Er was geen telregel voor rommelige makersnamen, dus dubbeltelling lag op de loer. De aandeelbalk was geschaald op de leider en misleidde.' },
    { ok: true, text: 'Een weergave binnen Podcasts, een sluitende telregel (één maker per show, 2+ shows, gelijke stand op de hoogste plek) en geen balk. Daarna verwerkt: gelijke stand in de mockup rechtgezet, beweging ten opzichte van gisteren zoals op de andere tabbladen, en de schakelaar in de toelichtingsregel.' }
  ] },
  c3: { rounds: [
    { ok: false, text: 'Als raster per maker was het overbodig naast Populair op de makerpagina. De markering hing alleen aan kleur (1,04:1), en de kolom Apple BE was willekeurig.' },
    { ok: false, text: 'Herzien tot een hellingsgrafiek over alle makers. Maar makers werden niet over de bronnen heen gekoppeld, zodat één maker twee halve lijnen kon krijgen. "Niet in de lijst" was onwaar bij 1 show, en rijen van 30px waren te klein.' },
    { ok: false, text: 'Koppeling per show, de rij "Minder dan 2 shows" en rijen van 44px. Door de top 10 had een maker op plek 11+ in de andere lijst echter geen plek.' },
    { ok: true, text: 'Twee zones onderaan: "Plek 11+", met de echte plek in het label, en "Minder dan 2 shows". Daardoor zegt de grafiek nergens meer iets onwaars. Spotify-exclusives tellen mee via hun makersnaam. Daarna verwerkt: een eigen rij van 44px per maker in beide zones.' }
  ] },
  c4: { rounds: [
    { ok: false, text: 'De mockup verzon Bibliotheek-tabbladen. Een samengevoegde feed van alle shows (217 bij NPO Luister) schaalt niet zonder server, en er was een tweede meldingsstijl naast AlertCard.' },
    { ok: false, text: '"Eén aanroep per maker" klopte niet (zoeklimiet 60), en een dagelijkse vergelijking op naam zou valse meldingen van nieuwe podcasts geven.' },
    { ok: true, text: 'ID-lijst uit de verzamelaar plus batch-lookup; pushmeldingen alleen voor makers met kanaal; zonder kanaal alleen in de kaart, met drie voorwaarden. Daarna verwerkt: kanalen blijven 90 dagen bewaard, "gevonden vandaag" voor makers zonder kanaal.' }
  ] },
  c5: { rounds: [
    { ok: false, text: 'Als apart kanaalscherm dubbelde het met het mediafilter op Tips en met de makerpagina. De rij toonde citaten die de data niet heeft.' },
    { ok: true, text: 'Een kleine uitbreiding van het bestaande Tips-scherm met de ongewijzigde tiprij, sorteren, en een brug naar de makerpagina bij een exacte naamsovereenkomst. Daarna verwerkt: de bestaande titelbalk en landchip, en de datum terug in de rij.' }
  ] }
};

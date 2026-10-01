# Onderzoek — aanvulling voor de tweede reeks

Aanvulling op `../nieuwe-functies/onderzoek.md`, voor de functies uit
`ideeen-ronde-1.md`. Stand: 1 oktober 2026.

Werkwijze: helppagina's, changelogs, issues en nieuwsberichten van de apps zelf.
Wat niet te vinden was, staat als `?` (niet vastgesteld). Geen cijfers verzonnen.
De kolom TC (Toadcast nu) komt uit de code onder `app/src/main/java/nl/woolacast/`.

## Functies × apps

✓ = aanwezig · ~ = deels, betaald of alleen in sommige versies · ✗ = niet aanwezig ·
? = niet vastgesteld. AP = AntennaPod (referentie), TC = Toadcast nu.

| Functie | YT | Spot | Apple | Amzn | iHrt | Cbox | Ovc | PC | PA | Pbn | AP | TC |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Melding bij een nieuwe aflevering | ~¹ | ✓ | ✓ | ? | ? | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗² |
| Eén lijst met nieuwe afleveringen van gevolgde shows | ✓¹ | ✓ | ? | ? | ? | ✓ | ✓ | ✓ | ✓ | ? | ✓ | ~³ |
| Bladwijzers | ✓ | ~ | ~ | ✗ | ✗ | ? | ✓ | ~⁴ | ✓ | ? | ✗ | ✗ |
| Delen vanaf een tijdstip (link) | ✓ | ✓ | ✓ | ? | ? | ? | ? | ✓ | ✓ | ? | ? | ✗ |
| Audio- of videoclip als bestand | ? | ? | ? | ? | ? | ? | ✓ | ✗⁵ | ? | ? | ✗ | ✗ |
| Casten / slimme speaker (uit het eerste onderzoek) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | ✗ |
| Luistergeschiedenis | ✓ | ~ | ~⁶ | ? | ? | ✓ | ~⁷ | ✓ | ✓ | ✓ | ✓ | ✗⁸ |
| Volledige back-up als bestand, zonder account | ✗ | ✗ | ✗ | ✗ | ✗ | ? | ~⁹ | ✗ | ✓ | ? | ✓ | ~¹⁰ |
| Slaaptimer: zacht uitfaden | ? | ?¹¹ | ? | ? | ? | ? | ✓ | ✓¹² | ✓ | ? | ✓ | ✗ |
| Slaaptimer: schudden om te verlengen | ? | ✗ | ? | ? | ? | ? | ✗ | ✓ | ? | ? | ✓ | ✗ |
| Slaaptimer: vanzelf aan in een tijdvak | ? | ? | ? | ? | ? | ? | ? | ~¹³ | ? | ? | ✓ | ✗ |
| Seriële volgorde (`itunes:type serial`) | ? | ✓ | ✓ | ? | ? | ? | ? | ~¹⁴ | ~¹⁴ | ? | ~¹⁴ | ✗ |
| Voorproefje (kort fragment) in de app | ~¹⁵ | ✓ | ? | ? | ? | ? | ? | ✗ | ✗ | ? | ✗ | ✗ |
| Leest `podcast:soundbite` | ? | ✗ | ✗ | ? | ✗ | ? | ? | ✗ | ✗ | ✗ | ✗ | ✗ |
| Deelbare afbeelding (aflevering of jaaroverzicht) | ? | ✓ | ? | ? | ? | ? | ✓¹⁶ | ✓ | ? | ? | ? | ✗ |
| Deelkaart van een hitlijstnotering | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ~¹⁷ |
| Uitleg bij een sprong in de hitlijst | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Spoelstappen zelf instellen (ronde 2) | ✓¹⁸ | ✗ | ✓ | ? | ? | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗¹⁹ |
| Koptelefoon: volgende/vorige als spoelen, instelbaar (ronde 2) | ? | ? | ✓ | ? | ? | ? | ✓ | ✓ | ? | ? | ✓ | ✗²⁰ |
| Apple- en Spotify-lijst naast elkaar (ronde 2) | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ~²¹ |

De rijen over hitlijsten volgen uit het eerste onderzoek: geen van de tien toont
beweging door de tijd, dus ook geen kaart of uitleg daarvan. Een vergelijking van
twee bronnen kan een app met één eigen lijst niet maken.

Voetnoten:

1. De YouTube-app heeft de bel voor kanalen. YouTube Music heeft een automatische
   lijst "New episodes", maar geen melding bij een nieuwe aflevering (9to5Google,
   april 2024).
2. Toadcast stuurt alleen meldingen over nieuwe shows van makers
   (`MakerCheckWorker`) en over downloads (`DownloadWorker`).
3. Per show "2 nieuw" in de Bibliotheek (`LibraryViewModel.refreshFeeds`), op basis
   van `lastOpened`. Geen samengevoegde lijst.
4. Bladwijzers zijn betaald (Plus/Patron). Pocket Casts bouwt er nog aan: "Smart
   Bookmarks" en vegen om te delen in 8.22.
5. Pocket Casts voegde audio- en videoclips toe in 7.72 (september 2024) en haalde
   ze weg in 7.82 (PR #3481, 24 januari 2025). Reden: de gebruikte bibliotheek
   (FFmpegKit) zou ophouden te werken. Delen via een link met tijdstip bleef.
6. "Recently Played", volgens gebruikers alleen voor gevolgde shows.
7. Overcast toont recent gespeelde afleveringen, maar houdt bewust geen blijvend
   luisterlog bij.
8. Toadcast onthoudt alleen *welke* afleveringen uitgeluisterd zijn (`listened`, tot
   2.000, zonder tijd) en de laatste positie per aflevering.
9. Overcast werkt met een account. Er is wel een uitgebreide OPML-export met
   voortgang per aflevering.
10. Toadcast heeft OPML-export. Daarnaast staat Android Auto Backup aan
    (`android:allowBackup="true"`, downloads uitgesloten in `backup_rules.xml` en
    `data_extraction_rules.xml`). Wie Google-back-up aan heeft, krijgt het hele
    `LocalStore`-bestand dus al in Google Drive.
11. Onduidelijk. Sommige gidsen zeggen dat Spotify uitfaadt. Er staat een open idee
    "Add a smooth fade-out when Sleep Timer ends" in de Spotify Community.
12. Afgeleid uit de changelog: "Fix volume that was not returning to the original
    level after restarting sleep timer by shaking the device".
13. "Auto Restart Sleep Timer": de timer start opnieuw als je binnen 5 minuten na
    de pauze weer afspeelt. Geen vast tijdvak.
14. Geen bewijs dat `itunes:type` gelezen wordt. Wel sorteren per show (oudste
    eerst). Pocket Casts toont afleveringsnummers en groepeert per seizoen.
    Podcast Addict leest de `podcast:season`-tag.
15. Shorts en voorbeelden in de feed; niet als functie per aflevering vastgesteld.
16. Overcast maakt een video van de clip met de hoes als beeld (2019).
17. De chart-tracker deelt de noteringen nu als tekst (`TrackerViewModel.shareText()`).
18. In de YouTube-app: dubbeltikken om te spoelen, 5 tot 60 seconden. Voor YouTube
    Music niet vastgesteld.
19. Vast op −15 en +30 (`SKIP_BACK_MS`, `SKIP_FORWARD_MS` in `PlayerController`).
20. `PlaybackService` haalt `COMMAND_SEEK_TO_NEXT` en `…_PREVIOUS` weg voor alle
    bedieningen, en de speler bevat één aflevering (de wachtrij zit in
    `PlayerController`). Volgende/vorige op een koptelefoon doet nu dus
    waarschijnlijk niets. Niet getest op een toestel.
21. Per show staan Apple en Spotify al naast elkaar: op de podcastpagina
    (`SourceColumnsHeader`) en in de chart-tracker. Niet per lijst.

Ronde 2, de andere apps:

- **Apple:** terug en vooruit los in te stellen op 10, 15, 30, 45 of 60 seconden.
  Bij "externe bediening" kies je tussen volgende/vorige aflevering en
  vooruit/terug.
- **Spotify:** vast op 15 seconden. Gebruikers vragen in de Community om meer.
- **Overcast:** 7, 15, 30, 45 of 60 seconden. "Remote Episode Skip": twee keer
  klikken spoelt vooruit, drie keer terug.
- **Pocket Casts:** spoelstappen in Instellingen, en apart "Headphone Controls" voor
  volgende en vorige (ook "volgend hoofdstuk").
- **Castbox (Android):** lang drukken op de knop geeft 5, 10, 15, 20 of 30 seconden.
- **Podbean:** "Seek by" in Instellingen.
- **Podcast Addict:** spoelstappen, ook per show.
- **AntennaPod:** spoelstappen, plus de keuze of de vooruitknop van de hardware
  spoelt of naar de volgende aflevering gaat. Er zijn veel bugmeldingen over:
  Bluetooth, bedrade koptelefoons, en Android Auto, waar de knop "volgende" ging
  spoelen (#2551).
- **iHeartRadio:** alleen voor de app in de auto gevonden (30 seconden heen en
  terug). Instelbaar: niet vastgesteld.
- **Amazon Music:** niet vastgesteld.

## Wat Apple en Spotify meten (ronde 2)

- **Apple** noemt drie factoren voor Top Shows: luisteren, volgen en hoe vaak
  afleveringen worden uitgeluisterd. Het exacte recept is geheim. Podnews (mei
  2025) concludeert dat vooral *nieuwe* volgers in de laatste dagen tellen. Hun
  voorbeeld: 24 nieuwe volgers waren genoeg voor de top 100. Ratings tellen niet
  mee.
- **Spotify** zegt dat Top Podcasts gaat over het wekelijkse unieke publiek (top 200
  per land). Een stream telt na 60 seconden luisteren, en één keer per persoon per
  dag. Categorielijsten zijn een top 50, en alleen in een deel van de landen.
- De bewering "Apple vooral nieuwe volgers, Spotify ook luisteren" klopt dus
  ongeveer, maar is te stellig. Apple zegt zelf ook luisteren en uitluisteren te
  meten. Dat een show die alleen bij Spotify hoog staat "vaak een videoshow" is: niet
  vastgesteld. Exclusieve Spotify-shows staan niet in Apple's catalogus, dus die
  staan per definitie alleen bij Spotify.
- **Vergelijken bestaat buiten de apps al.** Podchaser zet Apple- en Spotify-noteringen
  in één overzicht. Podfollow toont noteringen op Apple en Spotify. Ausha Charts
  toont beide gratis. Podstatus vergelijkt overlap met concurrenten (betaald).

## Wat verder van belang is

- **Spotify leest `itunes:type`.** Op hun pagina voor makers staan drie volgordes:
  Recent, Episodic en Serial. Bij Serial staat de oudste aflevering bovenaan.
  `itunes:order` gaat voor.
- **Soundbites in de top 10: nul.** In de Podcasting 2.0-lijst van Podnews (17
  apps en diensten met soundbite) staat geen van de tien. Pocket Casts, Podcast
  Addict, AntennaPod, Apple, Podbean en Spreaker (iHeart) lezen wel andere tags. In
  de feeds: 72.031 van de 4,7 mln. (eerste onderzoek).
- **Spotify heeft wél voorproefjes:** sinds maart 2023 staan er fragmenten tot 60
  seconden in de feed van Home, met meelopende tekst. Dat is een
  aanbevelingsfeed, geen functie in een lijst.
- **Chartable stopte op 12 december 2024.** Gratis alternatieven om noteringen te
  volgen bestaan: Apple's eigen lijsten, Podcharts en Ausha Charts (zonder
  account). Podstatus en Podchaser zijn betaald.
- **Uitfaden en schudden zijn een open-app-ding.** Bij Pocket Casts en AntennaPod
  werkt het, met bekende bugs: volume dat niet terugkomt (Pocket Casts), en de
  timer die meteen stopt als de bewegingssensor geweigerd is (AntennaPod #8760,
  GrapheneOS). AntennaPod had ook een bug met de automatische timer in Android
  Auto (#7052).
- **Back-up zonder account** komt alleen bij open apps voor. Podcast Addict maakt
  een lokaal bestand en optioneel automatisch naar Google Drive. AntennaPod heeft
  database-export en sinds 3.4 een automatische back-up (nu elke 3 dagen). Er zijn
  verzoeken om meer: AntennaPod-issues #4850 en #7564.
- **Media3 Cast:** in de huidige documentatie (media3-cast 1.11.1) wikkelt
  `CastPlayer` de lokale ExoPlayer in (`setLocalPlayer`). Media3 wisselt zelf
  tussen telefoon en speaker via de Output Switcher. Toadcast zit nu op Media3 1.5.1.

## Bronnen

- Pocket Casts:
  - [Sleep Timer (helppagina)](https://support.pocketcasts.com/knowledge-base/sleep-timer/)
  - [Bookmarks (helppagina)](https://support.pocketcasts.com/knowledge-base/bookmarks/)
  - [CHANGELOG Android](https://github.com/Automattic/pocket-casts-android/blob/main/CHANGELOG.md)
  - [PR #3481: Remove audio and video clip sharing](https://github.com/Automattic/pocket-casts-android/pull/3481)
  - [Clip sharing en transcripties (9to5Google, sep. 2024)](https://9to5google.com/2024/09/20/pocket-casts-clip-sharing-transcripts/)
  - [Forum: shake to extend](https://forums.pocketcasts.com/forums/topic/turning-off-shake-to-extend-sleep-timer-feature-sound/)
- Overcast:
  - [Clip sharing (Marco Arment, 2019)](https://marco.org/2019/04/27/overcast-clip-sharing)
  - [Clip sharing (9to5Mac)](https://9to5mac.com/2019/04/28/overcast-clip-sharing/)
  - [Gebruikersgids: meldingen per podcast (Podfeet, 2024)](https://www.podfeet.com/blog/2024/05/overcast-user-guide/)
  - [Slaaptimer met uitfaden (MacRumors)](https://forums.macrumors.com/threads/overcast-podcast-app-gains-new-black-dark-theme-end-of-episode-sleep-timer-and-more.2090519/)
  - [Geen schudden om te verlengen (TidBITS Talk)](https://talk.tidbits.com/t/do-you-use-it-podcast-apps/27148?page=3)
  - [Recent gespeeld, geen log (MPU Talk)](https://talk.macpowerusers.com/t/overcast-can-it-show-recently-played-episodes/34050)
- Podcast Addict:
  - [Back-up en herstel (FAQ)](https://podcastaddict.com/faq/20)
  - [Changelog 2022.6: delen met afspeelpositie](https://podcastaddict.com/changelog/2022_6)
  - [Uitfaden bij de slaaptimer](https://podcastaddict.uservoice.com/forums/211997-general/suggestions/36999709-fade-out-ramp-for-sleep-timer)
  - [Meldingen bij nieuwe afleveringen](https://trucoteca.com/en/how-to-configure-notifications-for-new-podcasts-in-podcast-addict/)
- AntennaPod:
  - [Shake to reset (#4825)](https://github.com/AntennaPod/AntennaPod/issues/4825)
  - [Timer stopt zonder bewegingssensor (#8760)](https://github.com/AntennaPod/AntennaPod/issues/8760)
  - [Automatische slaaptimer in een tijdvak (#6122)](https://github.com/AntennaPod/AntennaPod/issues/6122)
  - [Automatische timer gaat aan in Android Auto (#7052)](https://github.com/AntennaPod/AntennaPod/issues/7052)
  - [Verzoek: automatische back-up (#4850)](https://github.com/AntennaPod/AntennaPod/issues/4850)
  - [Verzoek: back-ups inplannen (#7564)](https://github.com/AntennaPod/AntennaPod/issues/7564)
  - [Database-back-up terugzetten (documentatie)](https://antennapod.org/documentation/bugs-first-aid/database-error)
  - [Meldingen bij nieuwe afleveringen (#7170)](https://github.com/AntennaPod/AntennaPod/issues/7170)
- Spotify:
  - [Meldingen bij nieuwe afleveringen (Engadget)](https://www.engadget.com/spotify-podcast-notifications-211926761.html)
  - [Nieuwe afleveringen in de Following-feed (FAQ)](https://community.spotify.com/t5/FAQs/New-Episodes-are-now-in-the-Following-Feed-FAQ/ta-p/6989287)
  - [Podcast consumption order: serial (voor makers)](https://providersupport.spotify.com/article/podcast-consumption-order)
  - [Voorproefjes op Home (2023)](https://newsroom.spotify.com/2023-03-08/spotify-previews-clips-music-podcasts-audiobooks-home-feed/)
  - [Delen met tijdstip (iMore)](https://www.imore.com/spotify-brings-podcast-timestamped-sharing-its-ios-app)
  - [Idee "schudden om te verlengen": gesloten](https://community.spotify.com:443/t5/Closed-Ideas/Mobile-Extend-Sleep-Timer-by-shaking-the-phone/idi-p/5096307)
  - [Idee "zacht uitfaden"](https://community.spotify.com/t5/Live-Ideas/Add-a-smooth-fade-out-when-Sleep-Timer-ends/idi-p/7386757)
- Apple Podcasts:
  - [Meldingen per show (Apple Support)](https://support.apple.com/guide/iphone/follow-your-favorite-podcasts-iph92ddcc196/ios)
  - [Delen vanaf een tijdstip (iOS 18, Tom's Guide)](https://www.tomsguide.com/phones/this-ios-18-feature-lets-you-share-your-favorite-podcast-moments-heres-how-to-do-it)
  - [Recently Played (Apple Community)](https://discussions.apple.com/thread/255979431)
  - [Afleveringsnummers en serial (Podnews, 2019)](https://podnews.net/article/episode-number-support-in-podcast-apps)
- YouTube Music: [Geen meldingen bij nieuwe afleveringen (9to5Google, 2024)](https://9to5google.com/2024/04/08/google-podcast-new-episode-notifications/)
- Castbox:
  - [Meldingen bij nieuwe afleveringen](https://sites.google.com/castbox.fm/castbox-help-center/listeners/account-settings/5-how-do-i-turn-onoff-new-episode-push-and-other-notifications)
  - [Gebruikershandleiding: New Episodes, Listening History](https://helpcenter.castbox.fm/portal/en/kb/articles/castbox-user-guide)
- Podbean:
  - [Meldingen (Android)](https://help.podbean.com/support/solutions/articles/25000016760-android-notifications)
  - [App Store: speelgeschiedenis](https://apps.apple.com/us/app/podbean-podcast-app-player/id973361050)
- Podcasting 2.0:
  - [Podnews: apps per tag (pc20-support)](https://podnews.net/api/pc20-support)
  - [Soundbite-tag (specificatie)](https://podcasting2.org/docs/podcast-namespace/tags/soundbite)
- Hitlijsten voor makers:
  - [Chartable stopt (Podchaser)](https://www.podchaser.com/articles/podcast-insights/chartable-is-shutting-down-heres-how-to-keep-getting-podcast-charts)
  - [Ausha Charts (gratis)](https://charts.ausha.co/en/index.html)
  - [Podstatus (betaald)](https://podstatus.com/compare/chartable-alternative)
- Ronde 2, spoelen en koptelefoon:
  - [Apple: spoelknoppen 10–60 s (Gadget Hacks)](https://ios.gadgethacks.com/how-to/customize-back-forward-skip-button-lengths-apple-podcasts-from-10-60-seconds-0189276/)
  - [Apple: externe bediening volgende/vorige of vooruit/terug (Apple Support)](https://support.apple.com/guide/podcasts/change-settings-pod4130f48/mac)
  - [Spotify: idee "Customizable skip intervals"](https://community.spotify.com/t5/Live-Ideas/Customizable-skip-forward-and-backward-intervals-for-Podcasts/idi-p/7410104)
  - [Overcast: spoelstappen en Remote Episode Skip (Podfeet)](https://www.podfeet.com/blog/2024/05/overcast-user-guide/)
  - [Pocket Casts: Skip Controls](https://support.pocketcasts.com/article/skip-controls/)
  - [Castbox: spoeltijden wijzigen (Android)](https://sites.google.com/castbox.fm/castbox-help-center/listeners/playback/7-can-i-change-the-skip-and-rewind-times-android)
  - [Podbean: skip controls (Android)](https://help.podbean.com/support/solutions/articles/25000017104-android-skip-controls)
  - [Podcast Addict: instellingen per podcast](https://podcastaddict.com/faq/340)
  - [AntennaPod: hardwareknoppen (#2428)](https://github.com/AntennaPod/AntennaPod/issues/2428)
  - [AntennaPod: Android Auto, volgende werd spoelen (#2551)](https://github.com/AntennaPod/AntennaPod/issues/2551)
  - [AntennaPod: dubbelklik koptelefoon (forum)](https://forum.antennapod.org/t/double-click-on-headphone-button-no-longer-skips-forward/8932)
  - [YouTube: dubbeltikken om te spoelen (Technipages)](https://www.technipages.com/how-to-configure-double-tap-to-seek-in-youtube-on-android/)
- Ronde 2, hitlijsten:
  - [Apple Podcasts Charts (Apple Podcasts for Creators)](https://podcasters.apple.com/support/3146-apple-podcasts-charts)
  - [Podcast charts on Spotify (Spotify for Creators)](https://support.spotify.com/us/creators/article/podcast-charts-on-spotify/)
  - [How do the podcast charts work? (Podnews, mei 2025)](https://podnews.net/article/how-the-podcast-charts-are-calculated)
  - [Podfollow Charts: Apple en Spotify](https://podfollow.com/charts)
  - [Podstatus: concurrenten en overlap](https://podstatus.com/features/competitors)
- Android:
  - [Media3: CastPlayer maken](https://developer.android.com/media/media3/cast/create-castplayer)
  - [Auto Backup voor apps](https://developer.android.com/identity/data/autobackup)

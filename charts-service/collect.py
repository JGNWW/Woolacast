#!/usr/bin/env python3
"""
Verzamelt wat de app niet zelf kan ophalen. Twee klussen, met heel andere kosten:

  snapshot  Ranglijsten van Apple en Spotify vastleggen. Twee aanroepen per
            lijst. Dit is goedkoop en mag overal draaien. Het bestaat niet om
            de lijst zelf — die haalt de app zo op — maar om de *historie*:
            stijgers en dalers kun je niet met terugwerkende kracht bepalen.

  new       Apple's redactionele lijst "Nieuwe programma's" per land. Geen
            ranglijst maar wel een echte lijst; alleen via het webtoken van
            podcasts.apple.com te lezen. Draait ook mee in snapshot.

  episodes  Apple's afleveringenlijst per categorie. De ranglijst is publiek,
            maar geeft alleen ids, en een afleverings-id is nergens massaal op
            te lossen. Elk id kost een eigen aanroep. Dit is de dure klus en
            hoort bescheiden te blijven.

Gebruik:
    python3 collect.py snapshot --countries nl,be,de --out charts
    python3 collect.py episodes --countries nl --limit 50 --out charts
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

UA = "Woolacast-charts/1.0 (+https://github.com/JGNWW/Woolacast)"
TODAY = datetime.now(timezone.utc).strftime("%Y-%m-%d")
NOW = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

APPLE_CHARTS = "https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/charts"
APPLE_LOOKUP = "https://itunes.apple.com/lookup"
APPLE_FEED = "https://rss.marketingtools.apple.com/api/v2/{cc}/podcasts/top/{n}/podcast-episodes.json"
APPLE_EPISODE = "https://podcasts.apple.com/{cc}/podcast/id0?i={id}"
SPOTIFY_CHART = "https://podcastcharts.byspotify.com/api/charts/{cat}"

GENRES = {
    26: "Alle categorieen", 1301: "Kunst", 1321: "Zaken", 1303: "Comedy",
    1304: "Educatie", 1483: "Fictie", 1511: "Overheid", 1512: "Gezondheid & fitness",
    1487: "Geschiedenis", 1305: "Kinderen & gezin", 1502: "Vrije tijd", 1310: "Muziek",
    1489: "Nieuws", 1314: "Religie & spiritualiteit", 1533: "Wetenschap",
    1324: "Maatschappij & cultuur", 1545: "Sport", 1309: "TV & film",
    1318: "Technologie", 1488: "True crime",
}

# Spotify hanteert eigen namen, en kent lang niet overal categorieen.
SPOTIFY_SLUG = {
    1303: "comedy", 1489: "news", 1488: "true-crime", 1324: "society-culture",
    1545: "sports", 1321: "business", 1533: "science", 1487: "history",
    1512: "health-fitness", 1301: "arts", 1304: "education", 1318: "technology",
    1310: "music", 1483: "fiction", 1502: "leisure", 1314: "religion-spirituality",
    1309: "tv-film",
}
SPOTIFY_MARKETS = {
    "us", "ar", "au", "at", "br", "ca", "cl", "co", "dk", "fi", "fr", "de",
    "in", "id", "ie", "it", "jp", "mx", "nz", "no", "ph", "pl", "es", "se", "nl", "gb",
}
SPOTIFY_CATEGORY_MARKETS = {"us", "au", "br", "de", "mx", "se", "gb"}

SERVER_DATA = re.compile(
    r'<script type="application/json" id="serialized-server-data">(.*?)</script>', re.S)
SHOW_ID = re.compile(r"/id(\d+)")

ROOT_GENRE = 26
HISTORY_LONG = 60   # alle categorieen: voedt de tracker
HISTORY_SHORT = 2   # per genre: genoeg voor stijgers en dalers


_robots: dict[str, "urllib.robotparser.RobotFileParser | None"] = {}

# news.google.com verbiedt in robots.txt alles behalve de voorpagina. Die regel
# staat er voor crawlers die de site afstruinen; /rss/search is een
# syndicatie-ingang die Google zelf aanbiedt aan feedlezers, en dat is wat we
# hier zijn: een handvol verzoeken per dag, onder onze eigen naam, zonder
# browser na te doen en zonder iets achter een inlog te lezen. Het is een
# bewuste keuze van de eigenaar van deze app; wie dat anders ziet haalt deze
# regel weg en de bron valt vanzelf stil.
ROBOTS_EXCEPTIONS = {"news.google.com"}


def allowed(url: str) -> bool:
    """
    Leest robots.txt van de host en houdt zich eraan. Een onbereikbare
    robots.txt telt als toestemming (dat is de gangbare lezing); een expliciete
    Disallow is een nee waar we niet omheen gaan.
    """
    import urllib.robotparser
    parts = urllib.parse.urlsplit(url)
    if parts.netloc in ROBOTS_EXCEPTIONS:
        return True
    host = f"{parts.scheme}://{parts.netloc}"
    if host not in _robots:
        parser = urllib.robotparser.RobotFileParser()
        parser.set_url(host + "/robots.txt")
        try:
            request = urllib.request.Request(host + "/robots.txt", headers={"User-Agent": UA})
            with urllib.request.urlopen(request, timeout=15) as response:
                parser.parse(response.read().decode("utf-8", "replace").splitlines())
        except Exception:
            parser = None
        _robots[host] = parser
    parser = _robots[host]
    return True if parser is None else parser.can_fetch(UA, url)


def fetch(url: str, *, raw: bool = False, tries: int = 3, headers: dict | None = None,
          timeout: int = 30):
    if not allowed(url):
        return None
    for attempt in range(tries):
        try:
            request = urllib.request.Request(
                url, headers={"User-Agent": UA, **(headers or {})})
            with urllib.request.urlopen(request, timeout=timeout) as response:
                body = response.read()
                final = response.geturl()
            return (body.decode("utf-8", "replace"), final) if raw else json.loads(body)
        except Exception:
            if attempt == tries - 1:
                return None
            time.sleep(1.5 * (attempt + 1))
    return None


# ------------------------------------------------------ Apple "Nieuwe programma's"

APPLE_WEB = "https://podcasts.apple.com"
APPLE_AMP = "https://amp-api.podcasts.apple.com/v1"
_apple_token: str | None = None


def apple_token() -> str | None:
    """
    Het bearer-token dat podcasts.apple.com zelf gebruikt staat in zijn
    JavaScript. Het is publiek, maar verloopt en verhuist; vandaar dat we het
    elke run opnieuw uit de pagina vissen.
    """
    global _apple_token
    if _apple_token:
        return _apple_token
    page = fetch(f"{APPLE_WEB}/nl/new", raw=True, headers={"User-Agent": "Mozilla/5.0"})
    if not page:
        return None
    match = re.search(r'src="(/assets/index~[^"]+\.js)"', page[0])
    if not match:
        return None
    bundle = fetch(APPLE_WEB + match.group(1), raw=True, headers={"User-Agent": "Mozilla/5.0"})
    if not bundle:
        return None
    token = re.search(r"eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{10,}", bundle[0])
    _apple_token = token.group(0) if token else None
    return _apple_token


def apple_new_shows(country: str, limit: int = 100) -> list[dict] | None:
    """
    Apple's tabblad "Nieuw" is redactioneel: het eerste 'room'-element op de
    pagina is "Nieuwe programma's" (per land een eigen lijst). Geen ranglijst,
    wel een echte lijst — en alleen via de amp-api met het webtoken te lezen.
    """
    token = apple_token()
    if not token:
        return None
    headers = {"Authorization": f"Bearer {token}", "Origin": APPLE_WEB, "User-Agent": "Mozilla/5.0"}
    groupings = fetch(
        f"{APPLE_AMP}/editorial/{country}/groupings?platform=web&name=podcasts",
        headers=headers)
    if not groupings:
        return None

    def rooms(node):
        if isinstance(node, dict):
            attrs = node.get("attributes", {})
            if node.get("type") == "editorial-elements" and str(attrs.get("editorialElementKind")) == "260":
                yield node
            for value in node.values():
                yield from rooms(value)
        elif isinstance(node, list):
            for item in node:
                yield from rooms(item)

    room = next(iter(rooms(groupings)), None)
    if not room:
        return None

    entries = []
    label = ""
    url = (f"{APPLE_AMP}/editorial/{country}/rooms/{room['id']}"
           f"?extend%5Bpodcasts%5D=feedUrl")
    while url and len(entries) < limit:
        page = fetch(url, headers=headers)
        if not page:
            break
        node = page["data"][0] if page.get("data") else page
        label = label or node.get("attributes", {}).get("title", "")
        contents = node.get("relationships", {}).get("contents", page)
        for item in contents.get("data", []):
            if item.get("type") != "podcasts":
                continue
            attrs = item.get("attributes", {})
            art = (attrs.get("artwork") or {}).get("url", "")
            entries.append({
                "id": item["id"],
                "title": attrs.get("name", ""),
                "publisher": attrs.get("artistName", ""),
                "artworkUrl": art.replace("{w}x{h}bb.{f}", "600x600bb.jpg") if art else None,
                "feedUrl": attrs.get("feedUrl"),
                "genre": next(iter(attrs.get("genreNames") or []), None),
                "releaseDate": attrs.get("releaseDate"),
            })
        nxt = contents.get("next")
        url = (APPLE_AMP.rsplit("/v1", 1)[0] + nxt + "&extend%5Bpodcasts%5D=feedUrl") if nxt else None
    return {"label": label, "entries": entries[:limit]} if entries else None


def write_new_shows(root: pathlib.Path, country: str, limit: int) -> int:
    result = apple_new_shows(country, limit)
    if not result:
        return 0
    folder = root / "apple" / country / str(ROOT_GENRE)
    folder.mkdir(parents=True, exist_ok=True)
    ranked = [{"rank": i, **e} for i, e in enumerate(result["entries"], 1)]
    (folder / "new.json").write_text(json.dumps({
        "source": "apple", "level": "new", "country": country, "genreId": ROOT_GENRE,
        "genreLabel": result["label"], "updated": NOW, "count": len(ranked), "entries": ranked,
    }, ensure_ascii=False, separators=(",", ":")))
    return len(ranked)


# ------------------------------------------------------------------ Mediatips

# Per land de media met een bereikbare feed. "dedicated": elke post is een
# podcasttip (Guardian's Hear Here, Podcast Review). "keyword": een gewone
# cultuurfeed waaruit alleen de posts over podcasts worden gevist — dat is wat
# de meeste kranten bieden; tagpagina's zoals die van de Volkskrant zitten
# achter een firewall die automatische lezers weert.
TIP_SOURCES = {
    "nl": [
        ("VPRO Podcastgids", "https://www.vpro.nl/rss.xml", "guide-feed"),
        ("de Volkskrant", "https://www.volkskrant.nl/cultuur-media/rss.xml", "keyword"),
        ("NRC", "https://www.nrc.nl/index/podcast/rss/", "guide-feed"),
        ("NOS", "https://feeds.nos.nl/nosnieuwscultuurenmedia", "keyword"),
        ("Trouw", "https://www.trouw.nl/cultuur-media/rss.xml", "keyword"),
        ("Het Parool", "https://www.parool.nl/kunst-media/rss.xml", "keyword"),
        # Breed net: alle rubriekfeeds van de grote algemene titels.
        ("de Volkskrant", "www.volkskrant.nl", "feeds"),
        ("AD", "www.ad.nl", "feeds"),
        ("De Telegraaf", "www.telegraaf.nl", "feeds"),
    ],
    "be": [
        ("De Standaard", "https://standaard.be/podcast/rss", "dedicated"),
        ("Humo", "https://www.humo.be/rss.xml", "keyword"),
        ("De Tijd", "https://www.tijd.be/rss/cultuur.xml", "keyword"),
        ("HLN", "www.hln.be", "feeds"),
        ("De Morgen", "www.demorgen.be", "feeds"),
    ],
    "de": [
        ("WDR", "https://wdr.de/kultur/podcast", "guide:/kultur/"),
        ("NDR", "https://ndr.de/radio/podcasts", "guide:/radio/"),
        ("Süddeutsche Zeitung", "https://sueddeutsche.de/podcast-tipps", "guide:/artikel/"),
        ("Der Spiegel", "https://spiegel.de/podcast", "guide:/spiegel/"),
        ("FAZ", "https://faz.net/podcast", "guide:/podcasts/"),
        ("BR", "https://br.de/podcast", "guide:/radio/"),
        ("Tagesspiegel", "https://tagesspiegel.de/podcast", "guide:/podcasts/"),
        ("Deutschlandfunk Kultur", "https://deutschlandfunkkultur.de/podcast", "guide:/de/"),
        ("Die Zeit", "www.zeit.de", "feeds"),
        ("Süddeutsche Zeitung", "www.sueddeutsche.de", "feeds"),
    ],
    "gb": [
        ("The Guardian", "https://www.theguardian.com/tv-and-radio/series/hear-here", "guide:/tv-and-radio/20"),
        ("The Guardian", "https://www.theguardian.com/tv-and-radio/series/hear-here/rss", "dedicated"),
        ("Radio Times", "https://www.radiotimes.com/podcasts/", "guide:/audio/podcasts/"),
    ],
    "us": [
        ("Podcast Review", "https://podcastreview.org/", "guide:/list/"),
        ("Podcast Review", "https://podcastreview.org/feed/", "dedicated"),
        ("The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/Arts.xml", "keyword"),
        ("The Atlantic", "https://www.theatlantic.com/feed/all/", "keyword"),
    ],
    "fr": [
        ("Télérama", "https://telerama.fr/rss/podcast.xml", "dedicated"),
        ("Télérama", "https://www.telerama.fr/rss/radio.xml", "keyword"),
        ("Slate", "https://slate.fr/podcasts", "guide:/podcasts/"),
        ("Radio France", "https://radiofrance.fr/podcasts", "guide:/franceinter/"),
        ("Le Nouvel Obs", "https://nouvelobs.com/tag/podcast", "guide:/culture/"),
        ("Le Monde", "https://lemonde.fr/podcasts", "guide:/podcasts/"),
        ("Le Monde", "https://www.lemonde.fr/culture/rss_full.xml", "keyword"),
    ],
    "es": [
        ("El Confidencial", "https://elconfidencial.com/podcasts", "guide:/us/"),
        ("La Vanguardia", "https://lavanguardia.com/podcast", "guide:/podcast/"),
        ("Cadena SER", "https://cadenaser.com/podcast", "guide:/podcast/"),
    ],
    "it": [
        ("Corriere della Sera", "https://corriere.it/podcast/rss", "guide:/podcast/"),
        ("la Repubblica", "https://repubblica.it/podcast", "guide:/audio/"),
        ("Il Post", "https://ilpost.it/podcasts", "guide:/podcasts/"),
    ],
    "se": [("Dagens Nyheter", "https://www.dn.se/rss/kultur/", "keyword"),
           ("Svenska Dagbladet", "https://www.svd.se/feed/articles.rss", "keyword"),
           ("Dagens Nyheter", "www.dn.se", "feeds"),
           ("Sveriges Radio", "sverigesradio.se", "feeds")],
    "dk": [("Politiken", "https://politiken.dk/kultur/podcast/", "guide:/kultur/kultur_podcast/"),
           ("Politiken", "https://politiken.dk/rss/kultur.rss", "keyword"),
           ("DR", "https://www.dr.dk/nyheder/service/feeds/kultur", "keyword"),
           ("Soundvenue", "https://soundvenue.com/tag/podcast", "guide:/film/"),
           ("Radio4", "https://radio4.dk/podcasts", "guide:/podcasts/"),
           ("DR", "www.dr.dk", "feeds"),
           ("Politiken", "politiken.dk", "feeds")],
    "no": [("NRK", "https://nrk.no/podcast", "guide:/kultur/")],
    "ie": [("The Irish Times", "https://irishtimes.com/podcasts", "guide:/podcasts/"),
           ("The Irish Times", "https://www.irishtimes.com/culture/tv-radio/", "guide:/culture/tv-radio/2"),
           ("RTÉ", "https://rte.ie/topic/podcasts", "guide:/radio/"),
           ("RTÉ", "https://www.rte.ie/feeds/rss/?index=/entertainment/", "keyword"),
           ("Hot Press", "https://hotpress.com/podcast", "guide:/culture/"),
           ("The Irish Times", "www.irishtimes.com", "feeds")],
    "ca": [("The Tyee", "https://thetyee.ca/topic/podcast", "guide:/News/"),
           ("CBC", "https://www.cbc.ca/radio/podcastnews", "guide:/radio/podcastnews/"),
           ("CBC", "https://www.cbc.ca/webfeed/rss/rss-arts", "keyword"),
           ("CBC", "www.cbc.ca", "feeds")],
    "au": [
        ("Guardian Australia", "https://theguardian.com/podcasts", "guide:/news/"),
        ("Crikey", "https://crikey.com.au/tag/podcast", "guide:/2016/"),
        ("ABC", "https://www.abc.net.au/news/feed/45910/rss.xml", "keyword"),
    ],
    "br": [
        ("Folha de S.Paulo", "https://folha.uol.com.br/podcast", "guide:/colunas/"),
        ("G1", "https://g1.globo.com/podcast", "guide:/podcast/"),
    ],
    "mx": [("El Economista", "https://eleconomista.com.mx/podcasts", "guide:/podcasts/"),
           ("El Universal", "https://eluniversal.com.mx/tag/podcasts", "guide:/tag/"),
           ("Chilango", "https://chilango.com/podcast", "guide:/podcast/")],
    "jp": [("NHK", "https://www3.nhk.or.jp/rss/news/cat6.xml", "keyword")],
    "in": [("Scroll.in", "https://scroll.in/tag/podcast", "guide:/article/"),
           ("The Indian Express", "https://indianexpress.com/tag/podcasting", "guide:/article/"),
           ("Mid-Day", "https://mid-day.com/podcast/rss", "guide:/news/"),
           ("The Hindu", "https://www.thehindu.com/entertainment/feeder/default.rss", "keyword")],
}
# Geen browser nadoen: wie ons blokkeert moet dat kunnen. De naam verwijst
# naar de repo, zodat een beheerder kan zien wie er langskomt.
BROWSER_UA = UA
TIP_DAYS = 120
TIP_SEARCH = "https://itunes.apple.com/search?term={term}&country={cc}&media=podcast&entity=podcast&limit=3"


def _text(tag: str, item: str) -> str:
    m = re.search(rf"<{tag}[^>]*>(.*?)</{tag}>", item, re.S)
    if not m:
        return ""
    raw = re.sub(r"<!\[CDATA\[|\]\]>", "", m.group(1))
    raw = re.sub(r"<[^>]+>", " ", raw)
    return re.sub(r"\s+", " ", html_unescape(raw)).strip()


def html_unescape(text: str) -> str:
    import html
    return html.unescape(text)


def parse_feed(body: str) -> list[dict]:
    items = []
    for raw in re.findall(r"<(?:item|entry)\b(.*?)</(?:item|entry)>", body, re.S):
        link = _text("link", raw) or (re.search(r'<link[^>]+href="([^"]+)"', raw) or [None, ""])[1]
        date = _text("pubDate", raw) or _text("published", raw) or _text("updated", raw) or _text("dc:date", raw)
        items.append({
            "title": _text("title", raw),
            "summary": _text("description", raw) or _text("summary", raw) or _text("content", raw),
            "link": link.strip(),
            "date": parse_any_date(date),
        })
    return items


def parse_any_date(raw: str) -> str | None:
    from email.utils import parsedate_to_datetime
    if not raw:
        return None
    try:
        return parsedate_to_datetime(raw).strftime("%Y-%m-%d")
    except Exception:
        pass
    m = re.match(r"(\d{4}-\d{2}-\d{2})", raw)
    return m.group(1) if m else None


# Een podcasttitel in een krantenkop staat tussen aanhalingstekens, of vlak
# achter het woord "podcast". Alles wat daarbuiten valt is te vaag om op te
# zoeken; een citaat van een geinterviewde lijkt er anders precies op.
# Let op de afkappingsquote: in "\u2018Afrika\u2019s grootste beursgang\u2019" is de eerste
# \u2019 geen sluitteken maar een apostrof. Een sluitteken wordt niet gevolgd
# door een letter.
# Elke taal zet zijn eigen tekens om een titel: \u2018zo\u2019 en \u201czo\u201d bij ons, \u201ezo\u201c in
# het Duits, \u201dzo\u201d en \u00bbzo\u00bb in het Zweeds, \u00abzo\u00bb in het Frans. Wie die niet
# allemaal kent, mist in zo'n taal alles.
QUOTED = re.compile(
    r"[\u2018\u201c\u201d\u00ab\u00bb\u201e\u300c\u300e\"']"
    r"([^\u2018\u2019\u201c\u201d\u00ab\u00bb\u201e\u300c\u300d\u300e\u300f\"']{2,70})"
    r"[\u2019\u201d\u201c\u00bb\u00ab\u300d\u300f\"'](?![\w])")
NEAR_PODCAST = re.compile(
    r"podcast(?:serie|reeks|series)?\s+(?:van\s+de\s+week\s+)?"
    r"[\u2018\u201c\u201d\u00ab\u00bb\u201e\u300c\u300e\"']"
    r"([^\u2018\u2019\u201c\u201d\u00ab\u00bb\u201e\u300c\u300d\u300e\u300f\"']{2,70})"
    r"[\u2019\u201d\u201c\u00bb\u00ab\u300d\u300f\"']",
    re.I)
TITLE_PREFIX = re.compile(r"^podcast\s+(?:tip:?\s+)?([^:\u2013\u2014-]{3,60})[:\u2013\u2014-]", re.I)
# Woorden die verraden dat een fragment een zin is, geen titel.
SENTENCE_HINT = re.compile(
    r"\b(?:ik|we|je|hij|zij|het is|dat is|maar|omdat|zegt|vindt|denk|there|that|this|which|because|they|"
    r"ich|aber|weil|dass|c\u2019est|nous|parce)\b", re.I)


LETTERS = re.compile(r"[^\W\d_]")
# Paginameubilair dat toevallig de naam van een echte podcast kan zijn.
STOPWORDS = {
    "read more", "about us", "sign in", "subscribe", "newsletter", "follow us",
    "share", "comments", "most read", "latest", "home", "menu", "privacy",
    "cookies", "contact", "advertisement", "sponsored", "podcast", "podcasts",
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag", "zondag",
    "lees meer", "nieuwsbrief", "inloggen", "abonneren", "meest gelezen",
    "you might also like", "more from", "related", "see also", "top stories",
    "most popular", "mehr zum thema", "meistgelesen", "anzeige", "empfehlungen",
    "lo más leído", "te puede interesar", "todas las noticias", "actualidad",
}


CJK = re.compile(r"[\u4e00-\u9fff\u3040-\u30ff]")

# Japans zet een titel tussen \u300c en \u300d. Daar hoort geen regel bij over wat
# erachter komt: in "\u300c\u2026\u300dがおすすめ" is dat gewoon het volgende woord, terwijl een
# aanhalingsteken in het Latijnse schrift ook een afkappingsquote kan zijn.
CJK_QUOTED = re.compile(
    r"[\u300c\u300e]([^\u300c\u300d\u300e\u300f]{2,70})[\u300d\u300f]")


def looks_like_title(name: str) -> bool:
    name = name.strip(" .,:;!?")
    if name.lower() in STOPWORDS or re.search(r"[{}\[\]<>|]", name):
        return False
    # Japans en Chinees kennen geen hoofdletters, schrijven zonder spaties en
    # zijn korter. Die regels gelden daar dus niet, en dit moet vóór de rest:
    # zonder hoofdletters is "HELEMAAL IN HOOFDLETTERS" altijd waar.
    if re.search(r"[\u0400-\u04ff\u0600-\u06ff]", name):
        return False
    if CJK.search(name):
        return 2 <= len(name) <= 40
    # HELEMAAL IN HOOFDLETTERS is een rubriekskop, geen titel.
    if len(name) > 4 and name.upper() == name and " " not in name:
        return False
    if not (4 <= len(name) <= 60):
        return False
    # Minstens vier letters, en niet overwegend cijfers: anders glipt er
    # rommel uit de opmaak van een pagina doorheen.
    letters = LETTERS.findall(name)
    if len(letters) < 4 or len(letters) < len(name) / 2:
        return False
    words = name.split()
    if len(words) > 7:
        return False
    if name.endswith(("...", ".", "?", "!")):
        return False
    if SENTENCE_HINT.search(name):
        return False
    # Een titel begint met een hoofdletter of een cijfer.
    return name[0].isupper() or name[0].isdigit()


# De opsomming onder een tiplijst: "Met deze week: Luister Anita, Dan Taberski's
# Manifesto, Proces X en De onderwereld." Dat is de enige plek waar een feed de
# besproken podcasts bij naam noemt, zonder het artikel erbij te halen.
LIST_INTRO = re.compile(
    r"(?:met deze week|deze week|met onder meer|met o\.a\.|this week|"
    r"diese woche|esta semana|cette semaine|questa settimana)\s*:\s*(.+)", re.I)
LIST_SPLIT = re.compile(r"\s*[,;]\s*|\s+(?:en|and|y|et|und|e)\s+", re.I)

# "In de podcast Mijn taalmaatje wil naar huis volgt ..." — zonder aanhaling.
# Waar de zin ophoudt en de titel begint is van buiten niet te zien, dus
# proberen we hem woord voor woord langer en laat Apple's catalogus beslissen.
AFTER_WORD = re.compile(
    r"\bpodcast(?:serie|reeks)?\s+((?:[^\W\d_][\w'\u2019-]*\s+){1,6}[\w'\u2019-]*)")


def list_candidates(text: str) -> list[str]:
    intro = LIST_INTRO.search(text or "")
    if not intro:
        return []
    names = []
    for part in LIST_SPLIT.split(intro.group(1)):
        part = part.strip(" .")
        if looks_like_title(part):
            names.append(part)
    return names[:8]


def after_word_candidates(text: str) -> list[str]:
    names: list[str] = []
    for match in AFTER_WORD.finditer(text or ""):
        words = match.group(1).split()
        for length in range(2, min(len(words), 6) + 1):
            name = " ".join(words[:length]).strip(" .,")
            if looks_like_title(name) and name not in names:
                names.append(name)
    return names[:8]


def podcast_candidates(title: str, summary: str) -> list[str]:
    found = []

    def add(name):
        name = name.strip(" .,:;\u2013\u2014-")
        if looks_like_title(name) and name not in found:
            found.append(name)

    prefix = TITLE_PREFIX.match(title.strip())
    if prefix:
        add(prefix.group(1))
    # Recensiekoppen zetten de naam vooraan: "X is a juicy true-crime show: ..."
    lead = re.split(r"\s+(?:is|was|are|zit|blijft|klinkt)\s+|[:\u2013\u2014]", title.strip(), 1)[0]
    if lead != title.strip():
        add(lead)
    for text in (title, summary):
        for m in NEAR_PODCAST.findall(text or ""):
            add(m)
    # En elk citaat dat op een titel lijkt. Dit gebeurde alleen als er nog geen
    # kandidaat was, maar dan houdt een loze kop ("Podcastrecensie:") de echte
    # titel erachter tegen. Elke kandidaat moet verderop toch exact een show in
    # Apple's catalogus zijn, dus een citaat erbij kost geen nauwkeurigheid.
    for text in (title, summary):
        for m in QUOTED.findall(text or ""):
            add(m)
        for m in CJK_QUOTED.findall(text or ""):
            add(m)
    return found[:4]


def normalise_any(title: str) -> str:
    """
    Zoals normalise, maar houdt letters uit elk schrift. Voor tips is dat nodig:
    anders valt "Подкаст Asia Pacific" samen met "Asia Pacific".
    """
    return re.sub(r"[\W_]+", "", title.lower(), flags=re.UNICODE)


def lookup_show(name: str, country: str) -> dict | None:
    """
    Zoekt de genoemde titel in Apple's catalogus. Alleen een treffer die exact
    zo heet telt: "Laika" mag geen show opleveren die het woord toevallig in
    zijn titel heeft. Een ondertitel achter een dubbele punt mag wegvallen.
    """
    data = fetch(TIP_SEARCH.format(term=urllib.parse.quote(name), cc=country))
    if not data:
        return None
    wanted = normalise_any(name)
    if len(wanted) < 4:
        return None
    for hit in data.get("results", []):
        title = hit.get("collectionName", "")
        variants = {normalise_any(title), normalise_any(title.split(":")[0]),
                    normalise_any(title.split(" - ")[0])}
        if wanted not in variants:
            continue
        return {
            "showId": str(hit.get("collectionId")),
            "showTitle": title,
            "publisher": hit.get("artistName", ""),
            "artworkUrl": hit.get("artworkUrl600") or hit.get("artworkUrl100"),
            "feedUrl": hit.get("feedUrl"),
            "genre": hit.get("primaryGenreName"),
        }
    return None


NRC_INDEX = "https://www.nrc.nl/index/podcast/"

# Grote titels publiceren per rubriek een feed op een vast patroon. Ze allemaal
# aflopen is een breed net: de opbrengst per feed is klein, maar het is
# materiaal dat de uitgever zelf voor machines klaarzet.
FEED_SLUGS = [
    "voorpagina", "nieuws", "cultuur-media", "kunst-media", "cultuur", "media",
    "show", "showbizz", "boeken", "muziek", "film", "tv", "podcasts", "radio-tv",
    "kijkverder", "wetenschap", "tech", "lifestyle", "entertainment", "opinie",
]
FEED_PATHS = ["/rss.xml", "/rss", "/feed"]
_feeds_cache: dict[str, list[str]] = {}


def discover_feeds(host: str, limit: int = 14) -> list[str]:
    """
    Zoekt de publieke feeds van een titel: wat de homepage aankondigt, plus de
    rubriekfeeds op het vaste patroon. Alleen wat echt items teruggeeft telt.
    """
    if host in _feeds_cache:
        return _feeds_cache[host]

    base = f"https://{host}"
    candidates: list[str] = []
    page = fetch(base, raw=True, tries=1, timeout=12, headers={"Accept": "text/html"})
    if page:
        for tag in re.finditer(r'<link[^>]+type="application/(?:rss|atom)\+xml"[^>]*>', page[0], re.I):
            href = re.search(r'href="([^"]+)"', tag.group(0))
            if href:
                candidates.append(urllib.parse.urljoin(base, href.group(1)))
    candidates += [base + p for p in FEED_PATHS]
    candidates += [f"{base}/{slug}/rss.xml" for slug in FEED_SLUGS]

    working, seen = [], set()

    def probe(url: str) -> str | None:
        got = fetch(url, raw=True, tries=1, timeout=10, headers={"Accept": "*/*"})
        return url if got and ("<item" in got[0] or "<entry" in got[0]) else None

    with ThreadPoolExecutor(max_workers=8) as pool:
        for url in pool.map(probe, [c for c in candidates if not (c in seen or seen.add(c))]):
            if url:
                working.append(url)
            if len(working) >= limit:
                break
    _feeds_cache[host] = working
    return working
GUIDE_ARTICLES = 14        # hoeveel artikelen van een gids we per run lezen
GUIDE_PER_ARTICLE = 6      # een tiplijst noemt er vijf; meer is bijvangst

# Een tiplijst noemt meerdere podcasts en mag ze alle koppelen. Een recensie
# gaat over één podcast: daar telt alleen de naam uit de kop, anders koppelen
# we de kop aan een podcast die er slechts terloops in genoemd wordt.
LIST_HINT = re.compile(
    r"\b(tips?|tipps|gids|guide|beste|besten|best|migliori|mejores|meilleurs|"
    r"anbefalinger|empfehlungen|selectie|selection|lijst|list|roundup|"
    r"twee|drie|vier|vijf|zes|zeven|acht|negen|tien|two|three|four|five|six|"
    r"seven|eight|nine|ten|dieci|cinco|cinq|f\u00fcnf|drei)\b|\btop\s*\d|"
    r"\d+\s*podcast|podcasts\b", re.I | re.UNICODE)


# Bij elke tip in een gids staat een rijtje luisterknoppen en labels. Dat is
# geen zin over de podcast, dus het gaat er vooraf af. Alleen deze woorden:
# een algemene regel zou de eerste woorden van een gewone zin opeten.
FURNITURE_WORDS = (
    "spotify", "apple", "podcasts", "podcast", "google", "rss", "rss-feed",
    "feed", "npo", "luister", "podimo", "youtube", "beluister", "deel",
    "share", "nederlands", "engels", "english", "duits", "vlaams", "jeugd",
)
STARS = "\u2605\u2606\u2022|\u2013\u2014-\u2026:"


def strip_furniture(text: str) -> str:
    """Haalt luisterknoppen, sterren en taallabels van de kop van een fragment."""
    words = text.split()
    cut = 0
    for i, word in enumerate(words):
        bare = word.strip(STARS + " ,.").lower()
        if bare in FURNITURE_WORDS or not bare or all(c in STARS for c in word):
            cut = i + 1
        elif i - cut > 1:
            break
    if cut and len(words) - cut >= 6:
        words = words[cut:]
    return " ".join(words).lstrip(" ,.:;" + STARS)


def about_window(plain: str, name: str, width: int = 240) -> str:
    """
    De zin waarin de podcast genoemd wordt, zodat de tip daarover gaat. Een
    naam komt vaak meermaals voor: in de paginakop, in een menu en in de
    lopende tekst. We kiezen het fragment dat het meest op proza lijkt.
    """
    best, best_score = "", -1.0
    for m in list(re.finditer(re.escape(name), plain, re.I))[:8]:
        # Begin bij het einde van de vorige zin, maar niet meer dan een halve
        # alinea terug: anders gaat het fragment over de vorige podcast.
        start = plain.rfind(". ", max(0, m.start() - 110), m.start())
        start = start + 2 if start != -1 else max(0, m.start() - 60)
        end = plain.find(". ", m.end())
        end = end + 1 if end != -1 and end - start <= width else min(len(plain), start + width)
        text = plain[start:end].strip()
        text = strip_furniture(text)
        words = text.split()
        if len(words) < 6:
            continue
        # Proza heeft lange woorden en kleine letters; een menu heeft korte
        # woorden met hoofdletters.
        lower = sum(1 for w in words if w[:1].islower()) / len(words)
        long = sum(1 for w in words if len(w) > 3) / len(words)
        score = lower + long + (0.5 if text.endswith(".") else 0)
        # "Met deze week: A, B, C en D" is de inleiding van een tiplijst, geen
        # zin over deze podcast.
        if text.count(", ") >= 2:
            score -= 0.6
        if score > best_score:
            best_score, best = score, text
            if start > 0 and not plain[max(0, start - 2):start].strip().endswith("."):
                best = "\u2026" + text
    # Een fragment dat middenin een zin begint, begint met een leesteken; dat
    # is geen begin van een zin en hoort er niet te staan.
    best = best.lstrip(" ,.?!:;\u2013\u2014")
    if best.startswith("\u2026 "):
        best = "\u2026" + best[2:]
    return best if best_score >= 1.0 else ""


def parse_guide_index(body: str, base: str, prefix: str) -> list[str]:
    """Artikellinks op de indexpagina van een podcastgids, in paginavolgorde."""
    links, seen = [], set()
    for href in re.findall(r'href="([^"]+)"', body):
        if prefix not in href:
            continue
        url = href if href.startswith("http") else urllib.parse.urljoin(base, href)
        if url.rstrip("/").endswith(prefix.rstrip("/")) or url in seen:
            continue
        seen.add(url)
        links.append(url)
    return links[:GUIDE_ARTICLES]


def read_guide_article(url: str) -> dict | None:
    """
    Een artikel uit een podcastgids: kop, datum en de namen die erin vetgedrukt
    of aangehaald staan. Tiplijsten noemen er vijf; die worden vijf tips.
    """
    page = fetch(url, raw=True, headers={"User-Agent": BROWSER_UA, "Accept": "text/html"})
    if not page:
        return None
    body = page[0]
    for marker in ("Lees meer", "Read more", "Mest lest", "Læs også", "Meest gelezen", "Related stories"):
        cut = body.find(marker)
        if cut > 2000:
            body = body[:cut]
            break
    title = re.search(r"<title[^>]*>(.*?)</title>", body, re.S)
    title = html_unescape(re.sub(r"<[^>]+>", "", title.group(1))).split("|")[0].strip() if title else ""
    date = re.search(r'"datePublished"\s*:\s*"(\d{4}-\d{2}-\d{2})', body)
    meta = re.search(r'<meta[^>]+(?:property="og:description"|name="description")[^>]+'
                     r'content="([^"]{20,400})"', body, re.I)
    meta = html_unescape(meta.group(1)).strip() if meta else ""

    names, seen = [], set()
    # Let op de tagnaam: <b[^>]*> zou ook <body> vangen en <i[^>]*> ook <img>
    # en <iframe>. Zo'n treffer slokt een half artikel op en de namen erin
    # verdwijnen. De naam moet dus eindigen op > of op een spatie.
    # Een kop of vetgedrukte naam is de aankondiging van een eigen alinea; een
    # cursieve naam kan ook een terloopse vergelijking zijn. Dat onderscheid
    # bepaalt straks of de naam een tip mag worden, dus we houden het bij.
    headings = [("kop", t) for t in re.findall(r"<h[23](?:\s[^>]*)?>(.*?)</h[23]>", body, re.S)]
    headings += [("kop" if tag in ("strong", "b") else "terzijde", inner)
                 for tag, inner in re.findall(
                     r"<(strong|b|em|i)(?:\s[^>]*)?>(.*?)</\1>", body, re.S)]
    announced = set()
    for kind, tag in headings:
        text = re.sub(r"\s+", " ", html_unescape(re.sub(r"<[^>]+>", " ", tag))).strip()
        if not looks_like_title(text):
            continue
        if kind == "kop":
            announced.add(text.lower())
        if text.lower() not in seen:
            seen.add(text.lower())
            names.append(text)
        if len(names) >= 25:
            break
    # Scripts en opmaakblokken bevatten de kop nog eens in JSON-LD; die horen
    # niet in de lopende tekst waaruit we citeren.
    readable = re.sub(r"<(script|style|noscript)[^>]*>.*?</\1>", " ", body, flags=re.S | re.I)
    plain = re.sub(r"\s+", " ", html_unescape(re.sub(r"<[^>]+>", " ", readable)))
    return {
        "title": title,
        "summary": meta or plain[:600],
        "link": url,
        "date": date.group(1) if date else None,
        "names": names,
        "announced": announced,
        "plain": plain,
    }


def parse_nrc_index(body: str) -> list[dict]:
    """
    NRC heeft geen feed voor zijn podcastrubriek, wel een indexpagina waarop
    elk artikel zijn datum in de URL draagt.
    """
    items, seen = [], set()
    for href, inner in re.findall(r'<a[^>]+href="(/nieuws/\d{4}/\d\d/\d\d/[^"]+)"[^>]*>(.*?)</a>', body, re.S):
        head = re.search(r'headline[^>]*>(.*?)</h\d>', inner, re.S)
        if not head or href in seen:
            continue
        seen.add(href)
        items.append({
            "title": html_unescape(re.sub(r"<[^>]+>", " ", head.group(1))).strip(),
            "summary": "",
            "link": "https://www.nrc.nl" + href,
            "date": href[8:18].replace("/", "-"),
        })
    return items


def couple_article(country: str, outlet: str, article: dict, article_url: str,
                   tips: list[dict]) -> int:
    """
    Koppelt één artikel aan de podcast(s) waar het over gaat.

    Een recensie noemt onderweg andere podcasts — eerder werk van dezelfde
    makers, iets uit hetzelfde genre. Die horen er niet als losse tip in, want
    dan staat er een kop boven die er niet over gaat. Daarom telt bewijs uit de
    kop en het webadres; alleen een tiplijst mag er meerdere noemen, en dan nog
    alleen namen die met een kop of vetgedrukt worden aangekondigd, of die meer
    dan één keer vallen: wie zijn eigen alinea krijgt wordt aangekondigd of
    vaker genoemd dan wie er ter vergelijking bij staat.
    """
    plain = article.get("plain", "")
    found = 0
    seen_here: set[str] = set()

    def mentioned_as_podcast(name: str) -> bool:
        """Een losse naam moet in het artikel bij het woord 'podcast' staan."""
        if " " in name.strip():
            return True
        for m in re.finditer(re.escape(name), plain):
            window = plain[max(0, m.start() - 120):m.end() + 120].lower()
            if "podcast" in window or "podkast" in window or "pod " in window:
                return True
        return False

    title_low = article["title"].lower()
    slug = normalise_any(urllib.parse.urlsplit(article_url).path)
    subject = [n for n in article["names"]
               if n.lower() in title_low or (len(n) > 4 and normalise_any(n) in slug)]
    subject += [n for n in podcast_candidates(article["title"], "")
                if n.lower() not in {x.lower() for x in subject}]

    is_list = bool(LIST_HINT.search(article["title"]))
    if is_list:
        # De opsomming onder een tiplijst noemt de besproken podcasts bij naam,
        # ook als ze in het artikel zelf alleen cursief staan.
        blurb = f"{article['title']} {article.get('summary') or ''}"
        names = (subject + list_candidates(blurb) + article["names"]
                 + podcast_candidates(article["title"], article["summary"])
                 + after_word_candidates(blurb))
    elif subject:
        names = subject          # recensie: alleen het onderwerp zelf
    else:
        return 0                 # geen bewijs waar het stuk over gaat

    for name in names:
        if found >= (GUIDE_PER_ARTICLE if is_list else 1):
            break
        name = name.strip(" .,:;\u2013\u2014-")
        if not looks_like_title(name) or name.lower() in seen_here:
            continue
        seen_here.add(name.lower())
        if not mentioned_as_podcast(name):
            continue
        # In een tiplijst krijgt elke besproken podcast zijn eigen alinea, en
        # daarin valt de naam meer dan eens. Eén vermelding is een zijstraat.
        if (is_list and name.lower() not in title_low
                and name.lower() not in article.get("announced", set())):
            if len(re.findall(re.escape(name), plain, re.I)) < 2:
                continue
        match = lookup_show(name, country)
        if not match:
            continue
        found += 1
        tips.append({
            "outlet": outlet,
            "headline": article["title"],
            "summary": (about_window(plain, name) or article["summary"])[:300],
            "url": article_url,
            "date": article["date"],
            **match,
        })
    return found


def read_guide(country: str, outlet: str, index_url: str, index_body: str,
               prefix: str, tips: list[dict], cutoff: str) -> int:
    kept = 0
    for article_url in parse_guide_index(index_body, index_url, prefix):
        article = read_guide_article(article_url)
        if not article:
            continue
        if article["date"] and article["date"] < cutoff:
            continue
        kept += couple_article(country, outlet, article, article_url, tips)
    return kept


def read_guide_feed(country: str, outlet: str, feed_body: str,
                    tips: list[dict], cutoff: str) -> int:
    """
    Dezelfde koppeling, maar de artikelen komen uit de RSS van het medium. Dat
    is beter dan de indexpagina afzoeken: de feed geeft de kop, de datum en de
    link zoals de redactie ze bedoeld heeft, en hij is verser.
    """
    kept = 0
    for item in parse_feed(feed_body)[:GUIDE_ARTICLES]:
        if item["date"] and item["date"] < cutoff:
            continue
        if not item["link"]:
            continue
        article = read_guide_article(item["link"])
        if not article:
            continue
        # De feed weet het beter dan de paginakop.
        article["title"] = item["title"] or article["title"]
        article["date"] = item["date"] or article["date"]
        if item["summary"]:
            article["summary"] = item["summary"]
        kept += couple_article(country, outlet, article, item["link"], tips)
    return kept


# ------------------------------------------------------------- Google Nieuws

# Google Nieuws bundelt wat wij per medium moeten najagen, en komt ook binnen
# bij titels die hun artikelen achter een toestemmingsscherm zetten: de kop
# staat in de feed, en een kop is genoeg om een podcast te herkennen.
GOOGLE_NEWS = "https://news.google.com/rss/search"

# Taal, land en editie per land, zoals Google ze wil hebben.
GOOGLE_EDITION = {
    "nl": ("nl", "NL", "NL:nl"), "be": ("nl", "BE", "BE:nl"),
    "de": ("de", "DE", "DE:de"), "gb": ("en-GB", "GB", "GB:en"),
    "us": ("en-US", "US", "US:en"), "fr": ("fr", "FR", "FR:fr"),
    "es": ("es", "ES", "ES:es"), "it": ("it", "IT", "IT:it"),
    "se": ("sv", "SE", "SE:sv"), "dk": ("da", "DK", "DK:da"),
    "no": ("no", "NO", "NO:no"), "ie": ("en-IE", "IE", "IE:en"),
    "ca": ("en-CA", "CA", "CA:en"), "au": ("en-AU", "AU", "AU:en"),
    "br": ("pt-BR", "BR", "BR:pt-419"), "mx": ("es-419", "MX", "MX:es-419"),
    "jp": ("ja", "JP", "JP:ja"), "in": ("en-IN", "IN", "IN:en"),
}

# Waar een rubriek naar heet in de taal van het land. Deze zoekopdrachten gaan
# over podcasts, dus een titel in de kop is er ook een.
GOOGLE_QUERIES = {
    "nl": ['"beste podcasts"', 'podcasttips', 'podcastrecensie', '"podcasttip"', '"podcastserie"',
           '"luistertip" OR "luistertips"', '"welke podcast"', '"podcastgids"'],
    "be": ['"beste podcasts"', 'podcasttips', 'podcastrecensie', '"podcasttip"', '"podcastserie"',
           '"luistertip" OR "luistertips"', '"podcastgids"'],
    "de": ['"Podcast-Tipps"', '"Podcast der Woche"', '"Podcast-Kritik"', '"neue Podcasts"',
           '"Podcast-Kolumne"', '"Podcast-Empfehlung"', '"Podcast-Rezension"', '"Podcast Empfehlungen"'],
    "gb": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "us": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "ie": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "ca": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "au": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "in": ['"best podcasts"', '"podcast review"', '"podcasts to listen to"', '"new podcasts"',
           '"what to listen to"', '"podcast roundup"', '"listening list"', '"podcast picks"'],
    "fr": ['"meilleurs podcasts"', '"podcasts à écouter"', '"podcast à écouter"', '"sélection de podcasts"',
           '"nouveaux podcasts"', '"critique podcast"'],
    "es": ['"mejores podcasts"', '"nuevos podcasts"', '"podcasts para escuchar"', '"podcast de la semana"',
           '"selección de podcasts"', '"recomendaciones de podcasts"'],
    "mx": ['"mejores podcasts"', '"nuevos podcasts"', '"podcasts para escuchar"', '"podcast de la semana"',
           '"selección de podcasts"', '"recomendaciones de podcasts"'],
    "it": ['"migliori podcast"', '"nuovi podcast"', '"podcast da non perdere"', '"cosa ascoltare"',
           '"podcast da ascoltare"', '"podcast consigliati"'],
    "se": ['"veckans podd"', 'poddtips', '"poddar att lyssna på"', '"nya poddar"', '"veckans poddtips"',
           '"bästa poddar"', '"lyssningstips"'],
    "dk": ['"bedste podcasts"', '"ugens podcast"', 'podcastanbefalinger', '"podcastanmeldelse"',
           '"nye podcasts"', '"podcasts du skal lytte til"'],
    "no": ['"beste podkaster"', '"ukens podkast"', 'podkasttips', '"podkastanbefaling"',
           '"nye podkaster"', '"podkastanmeldelse"'],
    "br": ['"melhores podcasts"', '"podcast da semana"', '"novos podcasts"', '"podcasts para ouvir"',
           '"indicações de podcast"', '"crítica de podcast"'],
    "jp": ['ポッドキャスト おすすめ', 'ポッドキャスト 特集', 'ポッドキャスト レビュー', '今週のポッドキャスト'],
}

# In een kop uit een site-zoekopdracht moet het woord podcast zelf staan: die
# zoekopdracht levert alles van die krant op, niet alleen podcastrecensies.
PODCAST_WORD = re.compile(
    # Ook het Japanse woord, en de afkorting die er in koppen van gemaakt wordt.
    r"podcast|podkast|podd|luistertip|h\u00f6rtipp|beluister|"
    r"\u30dd\u30c3\u30c9\u30ad\u30e3\u30b9\u30c8|\u30dd\u30c3\u30c9", re.I)
# Zoveel kranten krijgen een eigen zoekopdracht per land, per dag.
GOOGLE_SITE_QUERIES = 12


def google_news(country: str, query: str) -> list[dict]:
    """Eén zoekopdracht bij Google Nieuws, in de editie van dat land."""
    edition = GOOGLE_EDITION.get(country)
    if not edition:
        return []
    hl, gl, ceid = edition
    url = (f"{GOOGLE_NEWS}?q={urllib.parse.quote(query)}"
           f"&hl={hl}&gl={gl}&ceid={ceid}")
    page = fetch(url, raw=True, tries=2, timeout=25,
                 headers={"Accept": "application/rss+xml,application/xml,*/*"})
    if not page:
        return []
    items = []
    for raw in re.findall(r"<item>(.*?)</item>", page[0], re.S):
        title = re.search(r"<title>(.*?)</title>", raw, re.S)
        link = re.search(r"<link>(.*?)</link>", raw, re.S)
        date = re.search(r"<pubDate>(.*?)</pubDate>", raw, re.S)
        source = re.search(r'<source url="([^"]+)"[^>]*>(.*?)</source>', raw, re.S)
        if not title or not link:
            continue
        items.append({
            "title": html_unescape(title.group(1)).strip(),
            "link": html_unescape(link.group(1)).strip(),
            "date": rfc_date(date.group(1)) if date else None,
            "host": urllib.parse.urlsplit(source.group(1)).netloc if source else "",
            "outlet": tidy_outlet(html_unescape(source.group(2))) if source else "",
        })
    return items


def rfc_date(text: str) -> str | None:
    """"Wed, 02 Sep 2026 07:00:00 GMT" wordt "2026-09-02"."""
    try:
        from email.utils import parsedate_to_datetime
        return parsedate_to_datetime(text.strip()).strftime("%Y-%m-%d")
    except Exception:
        return None


OUTLET_NAMES = {
    "npo": "NPO", "nporadio1": "NPO Radio 1", "standaard": "De Standaard", "ctvnews": "CTV News",
    "sz": "SZ", "hln": "HLN", "nrc": "NRC", "vrt": "VRT", "rtl": "RTL",
    "news.com": "news.com.au", "thetimes": "The Times", "bbc": "BBC",
    "abc": "ABC", "cbc": "CBC", "rte": "RT\u00c9", "nos": "NOS", "vpro": "VPRO",
}


def tidy_outlet(name: str) -> str:
    """
    "NRC - Nieuws, achtergronden en onderzoeksjournalistiek" wordt "NRC", en
    een naam die Google als kaal domeinwoord teruggeeft krijgt zijn hoofdletters
    terug: "npo" wordt "NPO".
    """
    name = re.split(r"\s+[-\u2013|]\s+", name.strip())[0]
    # Google hangt er soms het land achter ("AD.nl"). Dat mag weg, maar ".com"
    # blijft staan: "News.com" is de naam zelf, niet een aanhangsel.
    name = re.sub(r"\s*\.(nl|be|de|fr|es|it|se|dk|no|ie|ca|au|br|mx|jp|in)$",
                  "", name, flags=re.I).strip()
    if name.lower() in OUTLET_NAMES:
        return OUTLET_NAMES[name.lower()]
    if name and name == name.lower() and " " not in name:
        return name.upper() if len(name) <= 4 else name.capitalize()
    return name


def same_house(outlet: str, publisher: str) -> bool:
    """
    Een medium dat zijn eigen aflevering aankondigt geeft geen tip. "Norwich
    City: Podcast" van de BBC is de BBC, en "SZ-Podcast: Hey München" is de
    Süddeutsche. Dat herken je eraan dat de uitgever van de show en het medium
    hetzelfde huis zijn.
    """
    skip = {"de", "het", "the", "la", "le", "el", "il", "podcast", "podcasts",
            "media", "nieuws", "news", "radio", "tv", "nl", "be", "com"}

    def tokens(text: str) -> set[str]:
        parts = re.split(r"[\W_]+", text.lower())
        return {p for p in parts if len(p) > 2 and p not in skip}

    if tokens(outlet) & tokens(publisher):
        return True
    # "NPO Radio 1" en "nporadio1" zijn hetzelfde huis, maar delen geen woord.
    # Zonder spaties en leestekens vallen ze wel samen.
    flat_outlet, flat_publisher = normalise_any(outlet), normalise_any(publisher)
    if len(flat_outlet) >= 4 and len(flat_publisher) >= 4:
        return flat_outlet in flat_publisher or flat_publisher in flat_outlet
    return False


def quoted_in(headline: str, name: str) -> bool:
    """
    Staat de naam tussen aanhalingstekens? Uit een kop alleen is dat het enige
    betrouwbare teken dat het een titel is. "Creatine: is there truth behind
    the hype? - podcast" gaat over creatine, niet over een show die zo heet.
    """
    # Ook de Japanse haken: 「 titel 」.
    quotes = "‘’“”«»„「」『』\"'"
    for m in re.finditer(re.escape(name), headline, re.I):
        before = headline[max(0, m.start() - 2):m.start()].strip()
        after = headline[m.end():m.end() + 2].strip()
        if before[-1:] in quotes and after[:1] in quotes:
            return True
    return False


# Lidwoorden horen niet bij de naam van een medium: wie "de" als naam telt,
# schrapt de halve Nederlandse pers.
HOUSE_SKIP = {"de", "het", "een", "the", "la", "le", "el", "il", "los", "las",
              "les", "der", "die", "das", "van", "en", "and", "of", "nl", "be"}


# Een krant die een persbericht overneemt tipt niets; die drukt af wat een
# uitgever zelf rondstuurde. In elke taal die we raken herkenbaar aan een woord
# vooraan de kop.
PRESS_RELEASE = re.compile(
    r"\u30d7\u30ec\u30b9\u30ea\u30ea\u30fc\u30b9|press release|pressemitteilung|"
    r"persbericht|comunicado de prensa|communiqu\u00e9 de presse|comunicato stampa|"
    r"pressmeddelande|pressemeddelelse", re.I)


def own_house(headline: str, outlet: str, host: str) -> bool:
    """
    Draagt de kop de naam van het medium zelf? Dan is het een aankondiging van
    een eigen aflevering, geen tip. Op een woordgrens, anders zit "AD" in
    "advies" en "SZ" in niets bijzonders.
    """
    words = {w for w in re.split(r"[\W_]+", outlet.lower())
             if len(w) > 1 and w not in HOUSE_SKIP}
    domain = host.replace("www.", "").split(".")[0].lower()
    if domain and domain not in HOUSE_SKIP:
        words.add(domain)
    if any(re.search(rf"\b{re.escape(w)}\b", headline, re.I) for w in words):
        return True
    # Ook het domein zonder punten: "nporadio1.nl" tegenover "NPO Radio 1".
    flat = normalise_any(host.replace("www.", "").split(".")[0])
    return len(flat) >= 5 and flat in normalise_any(headline)


def near_podcast(headline: str, name: str, window: int = 50) -> bool:
    """Staat de naam in dezelfde adem als het woord podcast?"""
    for m in re.finditer(re.escape(name), headline, re.I):
        around = headline[max(0, m.start() - window):m.end() + window]
        if PODCAST_WORD.search(around):
            return True
    return False


def collect_google(country: str, cutoff: str, known_hosts: set[str]) -> list[dict]:
    """
    Podcastrubrieken uit Google Nieuws, per land in de eigen taal. Twee soorten
    zoekopdrachten: op de naam van de rubriek ("beste podcasts"), en op de grote
    titels van dat land.

    Een medium dat we zelf al lezen wordt hier niet overgeslagen, en dat is met
    reden. Trouw stond eerst op die lijst, want we lezen hun cultuurfeed. Maar
    in geen van hun acht feeds staat ooit een podcastrecensie - die publiceren
    ze wel, ze zetten ze alleen niet in een feed. Google Nieuws vindt ze wel, en
    door het overslaan gooiden we precies die weg. Dubbel ophalen kan geen
    kwaad: aan het eind blijft per medium en per show een tip over.
    """
    # Alleen de grote titels van dat land. Google Nieuws indexeert ook elke
    # blog en elke persberichtensite, en die noemen "podcast" net zo vaak
    # zonder er een te tippen.
    allowed_hosts = {h.split("/")[0].replace("www.", "") for h in MEDIA.get(country, [])}
    allowed_hosts |= known_hosts

    # Een vraag per krant, niet vijf kranten in een vraag. Google geeft er
    # hooguit honderd items op terug, en bij vijf tegelijk verdringen de grote
    # titels de kleine: Nederland ging van twee koppelingen naar tien door dit
    # los te trekken.
    queries = [(q, False) for q in GOOGLE_QUERIES.get(country, [])]
    for host in list(MEDIA.get(country, []))[:GOOGLE_SITE_QUERIES]:
        queries.append((f"site:{host.split('/')[0]} podcast", True))

    tips, seen = [], set()
    for query, strict in queries:
        for item in google_news(country, f"{query} when:{TIP_DAYS}d"):
            if item["date"] and item["date"] < cutoff:
                continue
            # De kop draagt de naam van het medium erachter; die hoort er niet
            # bij als we de titel eruit vissen.
            headline = item["title"]
            if item["outlet"] and headline.endswith(" - " + item["outlet"]):
                headline = headline[: -len(item["outlet"]) - 3]
            else:
                headline = headline.rsplit(" - ", 1)[0] if " - " in headline else headline
            host = item["host"].replace("www.", "")
            if not any(host == h or host.endswith("." + h) for h in allowed_hosts):
                continue
            # Google zoekt ook in de lopende tekst, dus ook een zoekopdracht op
            # "beste podcasts" levert stukken op die er niet over gaan. We zien
            # alleen de kop, en die moet het dus zelf zeggen: een aangehaalde
            # titel zonder het woord podcast ernaast kan net zo goed een film
            # of een tv-programma zijn.
            if not PODCAST_WORD.search(headline):
                continue
            if own_house(headline, item["outlet"], item["host"]):
                continue
            if PRESS_RELEASE.search(headline):
                continue
            match = None
            for name in podcast_candidates(headline, ""):
                if not quoted_in(headline, name) or not near_podcast(headline, name):
                    continue
                match = lookup_show(name, country)
                if match:
                    break
            if not match or same_house(item["outlet"], match.get("publisher", "")):
                continue
            key = (item["outlet"], match["showId"])
            if key in seen:
                continue
            seen.add(key)
            tips.append({
                "outlet": item["outlet"] or "Google Nieuws",
                "headline": headline,
                "summary": "",
                "url": item["link"],
                "date": item["date"],
                "host": item["host"],
                **match,
            })
    return tips


def feed_candidates(item: dict) -> list[str]:
    """Alle namen die een feeditem prijsgeeft, zonder het artikel erbij."""
    text = f"{item['title']} {item.get('summary') or ''}"
    names = list_candidates(text)
    for name in (podcast_candidates(item["title"], item.get("summary") or "")
                 + after_word_candidates(text)):
        if name not in names:
            names.append(name)
    return names[:12]


def collect_tips(country: str) -> list[dict]:
    from datetime import timedelta
    cutoff = (datetime.now(timezone.utc) - timedelta(days=TIP_DAYS)).strftime("%Y-%m-%d")
    tips = []
    for outlet, url, mode in TIP_SOURCES.get(country, []):
        page = None
        if mode != "feeds":
            page = fetch(url, raw=True, headers={"Accept": "*/*"})
            if not page:
                print(f"    {outlet}: niet bereikbaar", flush=True)
                continue
        kept = 0
        if mode == "feeds":
            for feed_url in discover_feeds(url.replace("https://", "").strip("/")):
                got = fetch(feed_url, raw=True, tries=1, timeout=12, headers={"Accept": "*/*"})
                if not got:
                    continue
                for item in parse_feed(got[0]):
                    if "podcast" not in (item["title"] + " " + item["summary"]).lower():
                        continue
                    if item["date"] and item["date"] < cutoff:
                        continue
                    match = None
                    for name in feed_candidates(item):
                        match = lookup_show(name, country)
                        if match:
                            break
                    if not match:
                        continue
                    tips.append({
                        "outlet": outlet, "headline": item["title"],
                        "summary": item["summary"][:300], "url": item["link"],
                        "date": item["date"], **match,
                    })
                    kept += 1
            print(f"    {outlet}: {kept} tips (alle feeds)", flush=True)
            continue

        if mode == "guide-feed":
            kept = read_guide_feed(country, outlet, page[0], tips, cutoff)
            print(f"    {outlet}: {kept} tips (uit de feed)", flush=True)
            continue

        if mode.startswith("guide:"):
            kept = read_guide(country, outlet, url, page[0], mode.split(":", 1)[1], tips, cutoff)
            print(f"    {outlet}: {kept} tips", flush=True)
            continue
        entries = parse_nrc_index(page[0]) if mode == "nrc-index" else parse_feed(page[0])
        for item in entries:
            blob = (item["title"] + " " + item["summary"]).lower()
            if mode == "keyword" and "podcast" not in blob:
                continue
            if item["date"] and item["date"] < cutoff:
                continue
            match = None
            for name in feed_candidates(item):
                match = lookup_show(name, country)
                if match:
                    break
            if not match:
                continue
            tips.append({
                "outlet": outlet,
                "headline": item["title"],
                "summary": item["summary"][:300],
                "url": item["link"],
                "date": item["date"],
                **(match or {}),
            })
            kept += 1
        print(f"    {outlet}: {kept} tips", flush=True)
    # Wat we zelf rechtstreeks lezen hoeft Google niet nog eens aan te dragen.
    known = {urllib.parse.urlsplit(u if "//" in u else "//" + u).netloc.replace("www.", "")
             for _o, u, _m in TIP_SOURCES.get(country, [])}
    google = collect_google(country, cutoff, known)
    if google:
        print(f"    Google Nieuws: {len(google)} tips "
              f"({len({g['outlet'] for g in google})} media)", flush=True)
    tips += google

    tips.sort(key=lambda t: t.get("date") or "", reverse=True)
    # Dezelfde link twee keer (twee feeds van één medium) is één tip.
    seen, unique = set(), []
    for tip in tips:
        key = (tip["outlet"], tip.get("showId") or tip["url"])
        if key in seen:
            continue
        seen.add(key)
        unique.append(tip)
    return unique


# ------------------------------------------------------------------ prospector

# De grote media per land, om af te zoeken op een podcastrubriek. Dit is de
# enige handmatige lijst die overblijft: welke titels ertoe doen in een land
# valt niet af te leiden, de rest wel.
MEDIA = {
    "nl": ["vpro.nl", "nos.nl", "nrc.nl", "volkskrant.nl", "trouw.nl", "parool.nl",
           "ad.nl", "telegraaf.nl", "nporadio1.nl", "npo.nl", "vn.nl", "groene.nl"],
    "be": ["standaard.be", "demorgen.be", "hln.be", "vrt.be", "humo.be", "knack.be",
           "tijd.be", "nieuwsblad.be", "bruzz.be", "radio1.be", "mo.be", "vrt.be/vrtnws"],
    "de": ["zeit.de", "spiegel.de", "sueddeutsche.de", "faz.net", "tagesspiegel.de",
           "deutschlandfunk.de", "deutschlandfunkkultur.de", "br.de", "ndr.de", "wdr.de",
           "stern.de", "taz.de", "detektor.fm", "podwatch.io"],
    "gb": ["theguardian.com", "radiotimes.com", "bbc.co.uk", "independent.co.uk",
           "telegraph.co.uk", "thetimes.com", "standard.co.uk", "inews.co.uk",
           "newstatesman.com", "spectator.co.uk", "nme.com", "ft.com"],
    "us": ["podcastreview.org", "vulture.com", "nytimes.com", "theatlantic.com", "npr.org",
           "time.com", "washingtonpost.com", "theverge.com", "wired.com",
           "rollingstone.com", "avclub.com", "slate.com"],
    "fr": ["telerama.fr", "lemonde.fr", "liberation.fr", "radiofrance.fr", "lesinrocks.com",
           "franceinfo.fr", "lefigaro.fr", "nouvelobs.com", "slate.fr", "lesechos.fr",
           "lepoint.fr", "ouest-france.fr"],
    "es": ["elpais.com", "elmundo.es", "rtve.es", "eldiario.es", "lavanguardia.com",
           "elconfidencial.com", "abc.es", "20minutos.es", "cadenaser.com", "elespanol.com",
           "elperiodico.com", "publico.es"],
    "it": ["ilpost.it", "repubblica.it", "corriere.it", "internazionale.it", "rainews.it",
           "lastampa.it", "wired.it", "ilsole24ore.com", "rollingstone.it",
           "fanpage.it", "linkiesta.it", "esquire.it"],
    "se": ["dn.se", "svd.se", "sverigesradio.se", "svt.se", "aftonbladet.se", "expressen.se",
           "gp.se", "etc.se", "sydsvenskan.se", "poddtoppen.se", "dagensmedia.se"],
    "dk": ["politiken.dk", "dr.dk", "berlingske.dk", "information.dk", "jyllands-posten.dk",
           "soundvenue.com", "zetland.dk", "radio4.dk", "kristeligt-dagblad.dk", "bt.dk"],
    "no": ["nrk.no", "aftenposten.no", "vg.no", "dagbladet.no", "morgenbladet.no",
           "klassekampen.no", "nettavisen.no", "bt.no", "adressa.no", "dn.no"],
    "ie": ["irishtimes.com", "rte.ie", "independent.ie", "thejournal.ie",
           "irishexaminer.com", "hotpress.com", "businesspost.ie", "thecurrency.news",
           "newstalk.com", "breakingnews.ie", "gcn.ie", "irishmirror.ie"],
    "ca": ["cbc.ca", "theglobeandmail.com", "thestar.com", "macleans.ca",
           "nationalpost.com", "thewalrus.ca", "thetyee.ca", "cbc.ca/listen", "ctvnews.ca",
           "montrealgazette.com", "vancouversun.com", "quillandquire.com"],
    "au": ["abc.net.au", "smh.com.au", "theguardian.com", "theage.com.au", "news.com.au",
           "afr.com", "crikey.com.au", "themonthly.com.au", "abc.net.au/listen",
           "sbs.com.au", "theconversation.com", "junkee.com"],
    "br": ["folha.uol.com.br", "g1.globo.com", "estadao.com.br", "uol.com.br",
           "oglobo.globo.com", "terra.com.br", "veja.abril.com.br", "nexojornal.com.br",
           "tecmundo.com.br", "r7.com", "gauchazh.clicrbs.com.br"],
    "mx": ["eluniversal.com.mx", "milenio.com", "eleconomista.com.mx", "reforma.com",
           "sopitas.com", "chilango.com", "eluniversal.com.mx/techbit", "excelsior.com.mx",
           "animalpolitico.com", "elpais.com/mexico"],
    "jp": ["nhk.or.jp", "asahi.com", "yomiuri.co.jp", "nikkei.com", "mainichi.jp",
           "sankei.com", "natalie.mu", "itmedia.co.jp", "gizmodo.jp", "cinra.net"],
    "in": ["thehindu.com", "indianexpress.com", "hindustantimes.com", "scroll.in",
           "thewire.in", "livemint.com", "mid-day.com", "deccanherald.com", "theprint.in",
           "firstpost.com", "news18.com", "thequint.com"],
}

# Paden waarachter een podcastrubriek pleegt te zitten, in de talen die we raken.
PROSPECT_PATHS = [
    "/podcast", "/podcasts", "/tag/podcast", "/tag/podcasts", "/thema/podcast",
    "/thema/podcastgids", "/onderwerp/podcast", "/tags/podcasts", "/topic/podcasts",
    "/podcast-tipps", "/thema/podcast-tipps", "/kultur/podcast", "/cultuur/podcast",
    "/podcasttips", "/podkast", "/poddar", "/poddradio", "/podcasts-tips",
    "/culture/podcasts", "/arts/podcasts", "/radio/podcasts", "/audio/podcasts",
    "/podd", "/poddtips", "/poddradio-tips", "/emne/podcast", "/emner/podcast",
    "/lyd/podcast", "/podcast-anbefalinger", "/tema/podcast", "/kultur/podkast",
    "/topic/podcast", "/tag/podcasting", "/podcasts-recommendations",
    "/entertainment/podcasts", "/tv-radio/podcasts", "/listen", "/podcast-reviews",
    "/rss/podcast.xml", "/podcast/rss", "/feed/podcast",
]
# Aan een link naar een artikel herken je waar de stukken staan.
ARTICLE_HINT = re.compile(r"/(?:artikel|artikelen|nieuws|news|article|story|kultur|kultuur|"
                          r"culture|cultuur|podcast|podcasts|audio|radio|20\d\d)/", re.I)


def prospect_page(country: str, url: str) -> tuple[int, str | None]:
    """
    Kijkt of achter een adres een bruikbare rubriek zit: hoeveel podcasts uit
    Apple's catalogus worden er genoemd, en achter welk soort artikellink.
    """
    # Een verkenner mag niet blijven hangen: één poging, korte tijdslimiet.
    page = fetch(url, raw=True, tries=1, timeout=10,
                 headers={"Accept": "text/html,application/xml,*/*"})
    if not page:
        return 0, None
    body = page[0]
    if "<item" in body or "<entry" in body:
        hits = 0
        for item in parse_feed(body)[:20]:
            for name in podcast_candidates(item["title"], item["summary"]):
                if lookup_show(name, country):
                    hits += 1
                    break
        return hits, "feed"

    # Welk padvoorvoegsel komt het vaakst terug in artikellinks?
    prefixes: dict[str, int] = {}
    for href in re.findall(r'href="([^"?#]+)"', body):
        path = re.sub(r"^https?://[^/]+", "", href)
        if not ARTICLE_HINT.search(path):
            continue
        parts = [p for p in path.split("/") if p]
        if len(parts) < 2:
            continue
        prefixes["/" + parts[0] + "/"] = prefixes.get("/" + parts[0] + "/", 0) + 1
    if not prefixes:
        return 0, None
    prefix = max(prefixes, key=lambda k: prefixes[k])

    hits = 0
    for article_url in parse_guide_index(body, url, prefix)[:4]:
        article = read_guide_article(article_url)
        if not article:
            continue
        for name in (article["names"] + podcast_candidates(article["title"], article["summary"]))[:8]:
            if lookup_show(name, country):
                hits += 1
                break
    return hits, f"guide:{prefix}"


def run_prospect(countries: list[str]) -> None:
    for country in countries:
        print(f"\n{country}:", flush=True)
        def scan(domain: str):
            best = (0, None, None)
            for path in PROSPECT_PATHS:
                url = f"https://{domain}{path}"
                if not allowed(url):
                    continue
                hits, mode = prospect_page(country, url)
                if hits > best[0]:
                    best = (hits, url, mode)
                if best[0] >= 3:
                    break
            return domain, best

        found = []
        with ThreadPoolExecutor(max_workers=6) as pool:
            for domain, best in pool.map(scan, MEDIA.get(country, [])):
                if best[0]:
                    found.append(best)
                    print(f"  {domain:24} {best[0]:2} tips  {best[2]:22} {best[1]}", flush=True)
                else:
                    print(f"  {domain:24}  -", flush=True)
        found.sort(reverse=True)
        if found:
            print("  --- toe te voegen aan TIP_SOURCES ---")
            for hits, url, mode in found:
                print(f'        ("?", "{url}", "{mode}"),   # {hits} tips', flush=True)


# ------------------------------------------------------------------- logo's

# Het beeldmerk van een medium staat op zijn eigen site: als apple-touch-icon
# (dat is er juist om als tegel getoond te worden) of als icoon. We halen het
# één keer op en zetten het bij de gegevens, zodat de app het niet bij elke
# lezer opnieuw bij de uitgever hoeft op te halen.
LOGO_LINKS = re.compile(
    r'<link[^>]+rel="[^"]*(?:apple-touch-icon|icon)[^"]*"[^>]*>', re.I)
LOGO_HREF = re.compile(r'href="([^"]+)"', re.I)
LOGO_SIZES = re.compile(r'sizes="(\d+)x\d+"', re.I)
LOGO_TYPES = {b"\x89PNG": "png", b"\xff\xd8\xff": "jpg", b"RIFF": "webp"}


def logo_slug(outlet: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", outlet.lower().replace("é", "e").replace("ü", "u"))
    return slug.strip("-") or "medium"


def fetch_bytes(url: str, timeout: int = 15) -> bytes | None:
    if not allowed(url):
        return None
    try:
        request = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "image/*"})
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.read(400_000)
    except Exception:
        return None


def find_logo(host: str) -> tuple[bytes, str] | None:
    """Zoekt het beeldmerk van een site; groter icoon gaat voor."""
    host = host.strip("/")
    # Sommige sites zetten hun icoon alleen op de www-variant, of juist niet.
    other = host[4:] if host.startswith("www.") else "www." + host
    for candidate in (host, other):
        found = _logo_from(candidate)
        if found:
            return found
    return None


def _logo_from(host: str) -> tuple[bytes, str] | None:
    base = f"https://{host.strip('/')}"
    candidates: list[tuple[int, str]] = []
    page = fetch(base + "/", raw=True, tries=1, timeout=15, headers={"Accept": "text/html"})
    if page:
        for tag in LOGO_LINKS.findall(page[0]):
            href = LOGO_HREF.search(tag)
            if not href:
                continue
            url = urllib.parse.urljoin(page[1], html_unescape(href.group(1)))
            if url.lower().split("?")[0].endswith((".svg", ".ico")):
                continue
            size = LOGO_SIZES.search(tag)
            rank = int(size.group(1)) if size else (120 if "apple-touch" in tag.lower() else 32)
            candidates.append((rank, url))
    candidates.append((100, base + "/apple-touch-icon.png"))
    candidates.append((90, base + "/apple-touch-icon-precomposed.png"))
    candidates.sort(reverse=True)

    seen = set()
    for _, url in candidates:
        if url in seen:
            continue
        seen.add(url)
        data = fetch_bytes(url)
        if not data or len(data) < 200:
            continue
        for magic, ext in LOGO_TYPES.items():
            if data.startswith(magic):
                return data, ext
    return None


def write_logos(root: pathlib.Path, outlets: dict[str, str]) -> dict[str, str]:
    """Haalt per medium één beeldmerk op. Bestaat het al, dan blijft het staan."""
    folder = root / "logos"
    folder.mkdir(parents=True, exist_ok=True)
    names: dict[str, str] = {}
    for outlet, host in sorted(outlets.items()):
        slug = logo_slug(outlet)
        existing = sorted(folder.glob(f"{slug}.*"))
        if existing:
            names[outlet] = existing[0].name
            continue
        found = find_logo(host)
        if not found:
            print(f"    logo {outlet}: niet gevonden ({host})", flush=True)
            continue
        data, ext = found
        (folder / f"{slug}.{ext}").write_bytes(data)
        names[outlet] = f"{slug}.{ext}"
        print(f"    logo {outlet}: {len(data) // 1024 or 1} kB", flush=True)
    return names


def source_hosts(country: str) -> dict[str, str]:
    hosts: dict[str, str] = {}
    for outlet, url, _mode in TIP_SOURCES.get(country, []):
        host = urllib.parse.urlsplit(url if "//" in url else "//" + url).netloc or url.split("/")[0]
        hosts.setdefault(outlet, host)
    return hosts


# ------------------------------------------------- catalogus voor de app zelf

# De app kan zelf feeds lezen: dat houdt de tips vers tussen twee ronden van de
# verzamelaar door. Wat hij niet kan is een artikel uit elkaar halen — daar zit
# het meeste vakwerk en dat verandert het vaakst. Dus schrijven we op wáár hij
# moet kijken, en blijft het lezen van artikelen hier.
#
#   guide   de feed van een podcastrubriek: alles erin gaat al over podcasts
#   news    een gewone nieuwsfeed: daar moet een tipwoord bij staan
#   google  een zoekopdracht bij Google Nieuws, met de strengste regels
FEEDS_PER_COUNTRY = 24
FEEDS_PER_HOST = 2

# Zoveel kranten krijgen een eigen zoekopdracht in de catalogus van de app.
GOOGLE_SITE_FEEDS = 10

# De app kijkt drie maanden terug. Verder terug hoeft niet: wat langer geleden
# getipt is staat al in de gegevens van de verzamelaar.
CATALOGUE_DAYS = 90


def feed_catalogue(country: str, productive: set[str] | None = None) -> list[dict]:
    """De feeds die de app zelf kan lezen, op volgorde van opbrengst."""
    guides, news, discovered = [], [], []
    for outlet, url, mode in TIP_SOURCES.get(country, []):
        if mode in ("guide-feed", "dedicated"):
            guides.append({"outlet": outlet, "url": url, "kind": "guide"})
        elif mode == "keyword":
            news.append({"outlet": outlet, "url": url, "kind": "news"})
        elif mode == "feeds":
            host = url.replace("https://", "").strip("/")
            for feed_url in discover_feeds(host)[:FEEDS_PER_HOST]:
                discovered.append({"outlet": outlet, "url": feed_url, "kind": "news"})

    google = []
    edition = GOOGLE_EDITION.get(country)
    if edition:
        hl, gl, ceid = edition

        def vraag(query: str) -> dict:
            return {
                "outlet": "Google Nieuws",
                "url": (f"{GOOGLE_NEWS}?q={urllib.parse.quote(query + f' when:{CATALOGUE_DAYS}d')}"
                        f"&hl={hl}&gl={gl}&ceid={ceid}"),
                "kind": "google",
            }

        for query in GOOGLE_QUERIES.get(country, []):
            google.append(vraag(query))
        # En een eigen vraag per krant. Vijf kranten in een vraag verdringen
        # elkaar; los van elkaar vindt Google er meer. Wie deze ronde iets
        # opleverde gaat voor, maar de rest hoort er ook bij: een krant die nu
        # niets heeft kan morgen een recensie plaatsen.
        gedaan = set()
        for host in sorted(productive or ()):
            gedaan.add(host)
            google.append(vraag(f"site:{host} podcast"))
        for host in MEDIA.get(country, []):
            host = host.split("/")[0].replace("www.", "")
            if host in gedaan or len(gedaan) >= GOOGLE_SITE_FEEDS:
                continue
            gedaan.add(host)
            google.append(vraag(f"site:{host} podcast"))

    entries, seen = [], set()
    for entry in guides + google + news + discovered:
        if entry["url"] in seen:
            continue
        seen.add(entry["url"])
        entries.append(entry)
        if len(entries) >= FEEDS_PER_COUNTRY:
            break
    return entries


def write_feeds(root: pathlib.Path, country: str, logos: dict[str, str],
                productive: set[str] | None = None) -> int:
    # Zonder opgave van zaken: pak wat de vorige ronde opleverde, zodat de
    # catalogus ook los van een ophaalronde bij te werken is.
    if productive is None:
        previous = root / "tips" / f"{country}.json"
        if previous.exists():
            productive = set(json.loads(previous.read_text()).get("googleHosts", []))
    entries = feed_catalogue(country, productive)
    for entry in entries:
        if logos.get(entry["outlet"]):
            entry["logo"] = logos[entry["outlet"]]
    folder = root / "feeds"
    folder.mkdir(parents=True, exist_ok=True)
    # De media die in dit land meetellen. Een zoekopdracht bij Google Nieuws
    # levert ook blogs en persberichtensites op; die noemen "podcast" net zo
    # vaak zonder er een te tippen.
    hosts = sorted({h.split("/")[0].replace("www.", "") for h in MEDIA.get(country, [])}
                   | {urllib.parse.urlsplit(u if "//" in u else "//" + u).netloc.replace("www.", "")
                      for _o, u, _m in TIP_SOURCES.get(country, [])} - {""})
    # De editie van Google Nieuws voor dit land, zodat de app zelf een vraag kan
    # stellen over een losse podcast zonder dat wij hem hoeven voor te kauwen.
    hl, gl, ceid = GOOGLE_EDITION.get(country, ("en-US", "US", "US:en"))
    (folder / f"{country}.json").write_text(json.dumps({
        "country": country, "updated": NOW, "count": len(entries),
        "hosts": hosts,
        "search": f"{GOOGLE_NEWS}?hl={hl}&gl={gl}&ceid={ceid}&q=",
        "entries": entries,
    }, ensure_ascii=False, separators=(",", ":")))
    return len(entries)


def write_tips(root: pathlib.Path, country: str) -> int:
    tips = collect_tips(country)
    # Media uit Google Nieuws staan niet in TIP_SOURCES; hun adres komt uit de
    # feed mee, zodat ook zij een beeldmerk krijgen.
    hosts = {o: h for o, h in source_hosts(country).items()
             if o in {t["outlet"] for t in tips}}
    for tip in tips:
        if tip.get("host"):
            hosts.setdefault(tip["outlet"], tip["host"])
    logos = write_logos(root, hosts)
    # Kranten die deze ronde iets opleverden krijgen in de catalogus een eigen
    # zoekopdracht mee, zodat de app ze ook los bevraagt. Dit moet vóór het
    # opruimen van het adres gebeuren, anders is de lijst leeg.
    productive = {t["host"].replace("www.", "") for t in tips if t.get("host")}
    for tip in tips:
        tip.pop("host", None)
        if logos.get(tip["outlet"]):
            tip["logo"] = logos[tip["outlet"]]
    print(f"    catalogus: {write_feeds(root, country, logos, productive)} feeds voor de app",
          flush=True)
    folder = root / "tips"
    folder.mkdir(parents=True, exist_ok=True)
    (folder / f"{country}.json").write_text(json.dumps({
        "country": country, "updated": NOW, "count": len(tips),
        "outlets": sorted({t["outlet"] for t in tips}),
        # Welke kranten Google Nieuws hier iets opleverde: daarmee blijft de
        # catalogus van de app bij te werken zonder alles opnieuw op te halen.
        "googleHosts": sorted(productive),
        "entries": tips,
    }, ensure_ascii=False, separators=(",", ":")))
    return len(tips)


# ---------------------------------------------------------------- Apple shows

def apple_shows(country: str, genre_id: int, limit: int) -> list[dict] | None:
    query = urllib.parse.urlencode(
        {"cc": country, "g": genre_id, "name": "Podcasts", "limit": limit})
    ids = (fetch(f"{APPLE_CHARTS}?{query}") or {}).get("resultIds", [])
    if not ids:
        return None

    lookup = urllib.parse.urlencode({"id": ",".join(ids), "country": country})
    results = (fetch(f"{APPLE_LOOKUP}?{lookup}") or {}).get("results", [])
    by_id = {str(r.get("collectionId")): r for r in results}

    out = []
    for show_id in ids:
        show = by_id.get(show_id)
        if not show:
            continue
        out.append({
            "id": show_id,
            "title": show.get("collectionName") or "",
            "publisher": show.get("artistName") or "",
            "artworkUrl": show.get("artworkUrl600") or show.get("artworkUrl100"),
            "feedUrl": show.get("feedUrl"),
            "showId": show_id,
        })
    return out


def apple_episodes_all(country: str, limit: int) -> list[dict] | None:
    payload = fetch(APPLE_FEED.format(cc=country, n=min(limit, 100)))
    results = (payload or {}).get("feed", {}).get("results")
    if not results:
        return None
    return [{
        "id": r["id"],
        "title": r.get("name") or "",
        "publisher": r.get("artistName") or "",
        "artworkUrl": (r.get("artworkUrl100") or "").replace("100x100bb", "600x600bb") or None,
        "feedUrl": None,
        "showId": (SHOW_ID.search(r.get("url") or "") or [None, None])[1]
                  if SHOW_ID.search(r.get("url") or "") else None,
    } for r in results]


# -------------------------------------------------------------- Apple episodes

def episode_header(payload):
    stack = [payload]
    while stack:
        node = stack.pop()
        if isinstance(node, dict):
            if node.get("$kind") == "EpisodeHeader":
                return node
            stack.extend(node.values())
        elif isinstance(node, list):
            stack.extend(node)
    return None


def resolve_episode(country: str, episode_id: str) -> dict | None:
    result = fetch(APPLE_EPISODE.format(cc=country, id=episode_id), raw=True)
    if not result:
        return None
    html, final_url = result
    match = SERVER_DATA.search(html)
    if not match:
        return None
    header = episode_header(json.loads(match.group(1)))
    if not header or not header.get("title"):
        return None

    show_match = SHOW_ID.search(urllib.parse.urlparse(final_url).path)
    duration = header.get("duration")
    return {
        "id": episode_id,
        "title": header["title"],
        "showId": show_match.group(1) if show_match else None,
        "durationMs": int(duration) * 1000 if isinstance(duration, (int, float)) else None,
        "releaseDate": (header.get("releaseDate") or "")[:10] or None,
    }


def apple_episodes_by_genre(country: str, genre_id: int, limit: int, workers: int):
    query = urllib.parse.urlencode(
        {"cc": country, "g": genre_id, "name": "PodcastEpisodes", "limit": limit})
    ids = (fetch(f"{APPLE_CHARTS}?{query}") or {}).get("resultIds", [])
    if not ids:
        return None

    with ThreadPoolExecutor(max_workers=workers) as pool:
        resolved = [e for e in pool.map(lambda i: resolve_episode(country, i), ids) if e]

    show_ids = sorted({e["showId"] for e in resolved if e["showId"]})
    shows = {}
    for start in range(0, len(show_ids), 100):
        query = urllib.parse.urlencode(
            {"id": ",".join(show_ids[start:start + 100]), "country": country})
        for r in (fetch(f"{APPLE_LOOKUP}?{query}") or {}).get("results", []):
            shows[str(r.get("collectionId"))] = r

    out = []
    for episode in resolved:
        show = shows.get(episode["showId"] or "", {})
        out.append({
            **episode,
            "publisher": show.get("collectionName") or "",
            "artworkUrl": show.get("artworkUrl600") or show.get("artworkUrl100"),
            "feedUrl": show.get("feedUrl"),
        })
    return out


# ------------------------------------------------------------------- Spotify

def spotify_chart(country: str, category: str, limit: int) -> list[dict] | None:
    query = urllib.parse.urlencode({"region": country, "limit": limit})
    items = fetch(f"{SPOTIFY_CHART.format(cat=category)}?{query}",
                  headers={"Accept": "application/json"})
    if not items:
        return None

    episodes = category == "top-episodes"
    out = []
    for item in items:
        uri = item.get("episodeUri") if episodes else item.get("showUri")
        if not uri:
            continue
        out.append({
            "id": uri,
            "title": (item.get("episodeName") if episodes else item.get("showName")) or "",
            "publisher": (item.get("showName") if episodes else item.get("showPublisher")) or "",
            "artworkUrl": (item.get("episodeImageUrl") if episodes else None) or item.get("showImageUrl"),
            "feedUrl": None,
            "showId": item.get("showUri"),
        })
    return out


# --------------------------------------------------------------- wegschrijven

def record(root: pathlib.Path, source: str, country: str, genre_id: int,
           level: str, entries: list[dict], store_list: bool) -> dict:
    """
    Legt de rangen van vandaag vast en berekent de beweging ten opzichte van de
    laatste eerdere dag. Die historie is het enige wat je niet terug kunt halen:
    wie gisteren niet vastlegde, heeft gisteren niet.

    De lijst zelf wordt alleen bewaard als de app hem niet zelf kan ophalen —
    in de praktijk alleen Apple's afleveringen per categorie.
    """
    folder = root / source / country / str(genre_id)
    history_path = folder / f"{level}.history.json"

    days = json.loads(history_path.read_text())["days"] if history_path.exists() else {}
    earlier = [d for d in sorted(days) if d != TODAY]
    previous = days[earlier[-1]] if earlier else {}

    ranked = []
    for rank, entry in enumerate(entries, 1):
        before = previous.get(entry["id"])
        ranked.append({
            "rank": rank,
            **entry,
            "previousRank": before,
            "move": None if before is None else before - rank,
        })

    days[TODAY] = {e["id"]: e["rank"] for e in ranked}
    # Lange historie alleen waar de tracker hem gebruikt: de lijst over alle
    # categorieen. Per genre is twee dagen genoeg om beweging te tonen.
    keep = HISTORY_LONG if genre_id == ROOT_GENRE else HISTORY_SHORT
    for day in sorted(days)[:-keep]:
        days.pop(day)

    folder.mkdir(parents=True, exist_ok=True)
    history_path.write_text(json.dumps(
        {"source": source, "country": country, "genreId": genre_id,
         "level": level, "days": days},
        ensure_ascii=False, separators=(",", ":")))

    chart = {
        "source": source, "level": level, "country": country,
        "genreId": genre_id, "genreLabel": GENRES.get(genre_id, str(genre_id)),
        "updated": NOW, "count": len(ranked), "entries": ranked,
    }
    if store_list:
        (folder / f"{level}.json").write_text(
            json.dumps(chart, ensure_ascii=False, separators=(",", ":")))
    return chart


def write_movers(root: pathlib.Path, country: str, charts: list[dict]) -> None:
    """De grootste stijgers van vandaag, over alle verzamelde lijsten van dit land."""
    movers = []
    for chart in charts:
        for entry in chart["entries"]:
            if entry.get("move") and entry["move"] > 0:
                movers.append({
                    "id": entry["id"], "title": entry["title"],
                    "publisher": entry["publisher"], "artworkUrl": entry["artworkUrl"],
                    "feedUrl": entry.get("feedUrl"), "showId": entry.get("showId"),
                    "rank": entry["rank"], "previousRank": entry["previousRank"],
                    "move": entry["move"], "source": chart["source"],
                    "level": chart["level"], "genreLabel": chart["genreLabel"],
                })

    best = {}
    for mover in sorted(movers, key=lambda m: -m["move"]):
        best.setdefault((mover["source"], mover["id"]), mover)

    target = root / "movers" / f"{country}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(
        {"country": country, "updated": NOW,
         "entries": sorted(best.values(), key=lambda m: -m["move"])[:40]},
        ensure_ascii=False, separators=(",", ":")))


def normalise(title: str) -> str:
    """Genoeg om dezelfde show bij Apple en Spotify te herkennen."""
    return re.sub(r"[^a-z0-9]+", "", title.lower())


def write_shows(root: pathlib.Path, country: str, charts: list[dict]) -> None:
    """
    Waar noteert een show, op welke plek, bij welke bron. Dat is wat de tracker
    nodig heeft en het enige wat je niet uit één lijst kunt afleiden.

    Zo krap mogelijk: alleen land, bron en rang. Titel en artwork weet de app
    al, en dit bestand wordt elke dag opnieuw geschreven — wat er niet in staat,
    hoeft ook niet elke dag door de geschiedenis van de repo.

    Verdeeld over honderd bestanden op de laatste twee cijfers van het id, zodat
    de app er één van een paar kilobyte ophaalt in plaats van alles.

    Apple en Spotify delen geen id, dus die worden op naam gekoppeld.
    """
    apple_by_name, spotify_by_name = {}, {}
    for chart in charts:
        if chart["level"] != "shows":
            continue
        target = apple_by_name if chart["source"] == "apple" else spotify_by_name
        for entry in chart["entries"]:
            key = normalise(entry["title"])
            if key and (key not in target or entry["rank"] < target[key]["rank"]):
                target[key] = entry

    shards: dict[str, dict] = {}

    def shard_for(show_id: str) -> dict:
        name = show_id[-2:].rjust(2, "0")
        if name not in shards:
            path = root / "shows" / f"{name}.json"
            shards[name] = json.loads(path.read_text()) if path.exists() else {}
        return shards[name]

    for key, entry in apple_by_name.items():
        shard = shard_for(entry["id"])
        record = shard.get(entry["id"]) or {}
        positions = [p for p in record.get("p", []) if p[0] != country]
        positions.append([country, "a", entry["rank"]])

        twin = spotify_by_name.get(key)
        if twin:
            positions.append([country, "s", twin["rank"]])
            # De Spotify-uri is nodig om diens historie op te kunnen zoeken.
            record["u"] = twin["id"]

        record["p"] = sorted(positions, key=lambda p: (p[2], p[0]))
        shard[entry["id"]] = record

    folder = root / "shows"
    folder.mkdir(parents=True, exist_ok=True)
    for name, shard in shards.items():
        (folder / f"{name}.json").write_text(
            json.dumps(shard, ensure_ascii=False, separators=(",", ":")))


def write_index(root: pathlib.Path) -> None:
    charts = []
    for path in sorted(root.rglob("*.history.json")):

        data = json.loads(path.read_text())
        folder = path.parent
        listing = folder / f"{data['level']}.json"
        charts.append({
            "source": data["source"], "country": data["country"],
            "genreId": data["genreId"],
            "genreLabel": GENRES.get(data["genreId"], str(data["genreId"])),
            "level": data["level"], "days": sorted(data.get("days", {})),
            "hasList": listing.exists(),
            "path": str(path.relative_to(root)),
        })
    (root / "index.json").write_text(json.dumps(
        {"updated": NOW, "charts": charts}, ensure_ascii=False, indent=1))


# ------------------------------------------------------------------- klussen

def run_snapshot(countries: list[str], limit: int, root: pathlib.Path) -> None:
    for country in countries:
        collected = []
        for genre_id in GENRES:
            shows = apple_shows(country, genre_id, limit)
            if shows:
                collected.append(record(root, "apple", country, genre_id, "shows", shows, store_list=False))
                print(f"  apple   {country} {genre_id:5} shows      {len(shows):3}", flush=True)

        episodes = apple_episodes_all(country, min(limit, 100))
        if episodes:
            collected.append(record(root, "apple", country, ROOT_GENRE, "episodes", episodes, store_list=False))
            print(f"  apple   {country}    26 episodes   {len(episodes):3}", flush=True)

        if country in SPOTIFY_MARKETS:
            for category, genre_id, level in [("top-podcasts", 26, "shows"),
                                              ("top-episodes", 26, "episodes")]:
                rows = spotify_chart(country, category, limit)
                if rows:
                    collected.append(record(root, "spotify", country, genre_id, level, rows, store_list=False))
                    print(f"  spotify {country}    26 {level:10} {len(rows):3}", flush=True)

            if country in SPOTIFY_CATEGORY_MARKETS:
                for genre_id, slug in SPOTIFY_SLUG.items():
                    rows = spotify_chart(country, slug, limit)
                    if rows:
                        collected.append(record(root, "spotify", country, genre_id, "shows", rows, store_list=False))
                        print(f"  spotify {country} {genre_id:5} shows      {len(rows):3}", flush=True)

        # Apple's redactionele "Nieuwe programma's": drie aanroepen, dus dit mag mee.
        got = write_new_shows(root, country, 100)
        print(f"  apple   {country}    26 new        {got:3}", flush=True)

        if collected:
            write_movers(root, country, collected)
            write_shows(root, country, collected)


def run_episodes(countries: list[str], limit: int, workers: int, root: pathlib.Path) -> None:
    for country in countries:
        for genre_id in GENRES:
            if genre_id == 26:
                continue  # die haalt de app zelf op
            started = time.time()
            entries = apple_episodes_by_genre(country, genre_id, limit, workers)
            if not entries:
                print(f"  {country} {genre_id:5} {GENRES[genre_id]:24} geen lijst", flush=True)
                continue
            record(root, "apple", country, genre_id, "episodes", entries, store_list=True)
            print(f"  {country} {genre_id:5} {GENRES[genre_id]:24} "
                  f"{len(entries):3}/{limit:3}  {time.time() - started:.0f}s", flush=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="job", required=True)

    snap = sub.add_parser("snapshot", help="goedkoop; legt ranglijsten vast voor de historie")
    snap.add_argument("--countries", default="nl")
    snap.add_argument("--limit", type=int, default=100)
    snap.add_argument("--out", default="charts")

    new = sub.add_parser("new", help="Apple's redactionele lijst Nieuwe programma's per land")
    new.add_argument("--countries", default="nl")
    new.add_argument("--limit", type=int, default=100)
    new.add_argument("--out", default="charts")

    pro = sub.add_parser("prospect", help="zoekt per land welke media een leesbare podcastrubriek hebben")
    pro.add_argument("--countries", default="nl")
    pro.add_argument("--out", default="charts")

    tips = sub.add_parser("tips", help="podcasttips uit de feeds van kranten en omroepen")
    tips.add_argument("--countries", default="nl")
    tips.add_argument("--out", default="charts")

    eps = sub.add_parser("episodes", help="duur; lost afleverings-ids op")
    eps.add_argument("--countries", default="nl")
    eps.add_argument("--limit", type=int, default=50)
    eps.add_argument("--workers", type=int, default=6)
    eps.add_argument("--out", default="charts")

    args = parser.parse_args()
    root = pathlib.Path(args.out)
    countries = [c.strip().lower() for c in args.countries.split(",") if c.strip()]

    if args.job == "snapshot":
        run_snapshot(countries, args.limit, root)
    elif args.job == "prospect":
        run_prospect(countries)
        return 0
    elif args.job == "tips":
        for country in countries:
            print(f"  {country}:", flush=True)
            print(f"  tips    {country}  {write_tips(root, country):3}", flush=True)
    elif args.job == "new":
        for country in countries:
            print(f"  apple   {country}    26 new        {write_new_shows(root, country, args.limit):3}", flush=True)
    else:
        run_episodes(countries, args.limit, args.workers, root)

    write_index(root)
    return 0


if __name__ == "__main__":
    sys.exit(main())

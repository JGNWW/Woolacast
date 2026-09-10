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


def allowed(url: str) -> bool:
    """
    Leest robots.txt van de host en houdt zich eraan. Een onbereikbare
    robots.txt telt als toestemming (dat is de gangbare lezing); een expliciete
    Disallow is een nee waar we niet omheen gaan.
    """
    import urllib.robotparser
    parts = urllib.parse.urlsplit(url)
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
        ("VPRO Podcastgids", "https://www.vpro.nl/thema/podcastgids", "guide:/artikelen/"),
        ("de Volkskrant", "https://www.volkskrant.nl/cultuur-media/rss.xml", "keyword"),
        ("NRC", "https://www.nrc.nl/index/podcast/", "nrc-index"),
        ("NOS", "https://feeds.nos.nl/nosnieuwscultuurenmedia", "keyword"),
        ("Trouw", "https://www.trouw.nl/cultuur-media/rss.xml", "keyword"),
        ("Het Parool", "https://www.parool.nl/kunst-media/rss.xml", "keyword"),
    ],
    "be": [
        ("De Standaard", "https://standaard.be/podcast/rss", "dedicated"),
        ("Humo", "https://www.humo.be/rss.xml", "keyword"),
        ("De Tijd", "https://www.tijd.be/rss/cultuur.xml", "keyword"),
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
        ("Télérama", "https://www.telerama.fr/rss/radio.xml", "keyword"),
        ("Le Monde", "https://www.lemonde.fr/culture/rss_full.xml", "keyword"),
        ("Libération", "https://www.liberation.fr/arc/outboundfeeds/rss/category/culture/", "keyword"),
    ],
    "es": [
        ("El Confidencial", "https://elconfidencial.com/podcasts", "guide:/us/"),
        ("La Vanguardia", "https://lavanguardia.com/podcast", "guide:/podcast/"),
    ],
    "it": [
        ("Corriere della Sera", "https://corriere.it/podcast/rss", "guide:/podcast/"),
        ("la Repubblica", "https://repubblica.it/podcast", "guide:/audio/"),
        ("Il Post", "https://ilpost.it/podcasts", "guide:/podcasts/"),
    ],
    "se": [("Dagens Nyheter", "https://www.dn.se/rss/kultur/", "keyword"),
           ("Svenska Dagbladet", "https://www.svd.se/feed/articles.rss", "keyword")],
    "dk": [("Politiken", "https://politiken.dk/kultur/podcast/", "guide:/kultur/kultur_podcast/"),
           ("Politiken", "https://politiken.dk/rss/kultur.rss", "keyword"),
           ("DR", "https://www.dr.dk/nyheder/service/feeds/kultur", "keyword")],
    "no": [("NRK", "https://nrk.no/podcast", "guide:/kultur/")],
    "ie": [("The Irish Times", "https://www.irishtimes.com/culture/tv-radio/", "guide:/culture/tv-radio/2"),
           ("RTÉ", "https://www.rte.ie/feeds/rss/?index=/entertainment/", "keyword")],
    "ca": [("CBC", "https://www.cbc.ca/radio/podcastnews", "guide:/radio/podcastnews/"),
           ("CBC", "https://www.cbc.ca/webfeed/rss/rss-arts", "keyword")],
    "au": [
        ("Guardian Australia", "https://theguardian.com/podcasts", "guide:/news/"),
        ("ABC", "https://www.abc.net.au/news/feed/45910/rss.xml", "keyword"),
    ],
    "br": [
        ("Folha de S.Paulo", "https://folha.uol.com.br/podcast", "guide:/colunas/"),
        ("G1", "https://g1.globo.com/podcast", "guide:/podcast/"),
    ],
    "jp": [("NHK", "https://www3.nhk.or.jp/rss/news/cat6.xml", "keyword")],
    "in": [("The Hindu", "https://www.thehindu.com/entertainment/feeder/default.rss", "keyword")],
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
QUOTED = re.compile(r"[\u2018\u201c\u00ab\u201e\"']([^\u2018\u2019\u201c\u201d\u00ab\u00bb\u201e\"']{3,70})[\u2019\u201d\u00bb\"']")
NEAR_PODCAST = re.compile(
    r"podcast(?:serie|reeks|series)?\s+(?:van\s+de\s+week\s+)?"
    r"[\u2018\u201c\u00ab\u201e\"']([^\u2018\u2019\u201c\u201d\u00ab\u00bb\u201e\"']{3,70})[\u2019\u201d\u00bb\"']",
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


def looks_like_title(name: str) -> bool:
    name = name.strip(" .,:;!?")
    if name.lower() in STOPWORDS or re.search(r"[{}\[\]<>|]", name):
        return False
    # HELEMAAL IN HOOFDLETTERS is een rubriekskop, geen titel. En een naam in
    # een ander schrift dan de pagina hoort er niet: dat is menu of advertentie.
    if len(name) > 4 and name.upper() == name and " " not in name:
        return False
    if re.search(r"[\u0400-\u04ff\u4e00-\u9fff\u3040-\u30ff\u0600-\u06ff]", name):
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
    # Pas als de directe aanwijzingen niets geven: elk citaat dat op een titel lijkt.
    if not found:
        for text in (title, summary):
            for m in QUOTED.findall(text or ""):
                add(m)
    return found[:3]


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

    names, seen = [], set()
    tags = (re.findall(r"<h[23][^>]*>(.*?)</h[23]>", body, re.S)
            + re.findall(r"<(?:strong|b|em|i)[^>]*>(.*?)</(?:strong|b|em|i)>", body, re.S))
    for tag in tags:
        text = re.sub(r"\s+", " ", html_unescape(re.sub(r"<[^>]+>", " ", tag))).strip()
        if looks_like_title(text) and text.lower() not in seen:
            seen.add(text.lower())
            names.append(text)
        if len(names) >= 25:
            break
    plain = re.sub(r"\s+", " ", html_unescape(re.sub(r"<[^>]+>", " ", body)))
    return {
        "title": title,
        "summary": plain[:600],
        "link": url,
        "date": date.group(1) if date else None,
        "names": names,
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


def read_guide(country: str, outlet: str, index_url: str, index_body: str,
               prefix: str, tips: list[dict], cutoff: str) -> int:
    kept = 0
    for article_url in parse_guide_index(index_body, index_url, prefix):
        article = read_guide_article(article_url)
        if not article:
            continue
        if article["date"] and article["date"] < cutoff:
            continue
        found = 0
        seen_here = set()
        plain = article.get("plain", "")

        def mentioned_as_podcast(name: str) -> bool:
            """Een losse naam moet in het artikel bij het woord 'podcast' staan."""
            if " " in name.strip():
                return True
            for m in re.finditer(re.escape(name), plain):
                window = plain[max(0, m.start() - 120):m.end() + 120].lower()
                if "podcast" in window or "podkast" in window or "pod " in window:
                    return True
            return False

        for name in article["names"] + podcast_candidates(article["title"], article["summary"]):
            if found >= GUIDE_PER_ARTICLE:
                break
            name = name.strip(" .,:;\u2013\u2014-")
            if not looks_like_title(name) or name.lower() in seen_here:
                continue
            seen_here.add(name.lower())
            if not mentioned_as_podcast(name):
                continue
            match = lookup_show(name, country)
            if not match:
                continue
            # De podcast moet ook echt in het artikel besproken worden, niet
            # alleen in een verwijzing naar een ander stuk.
            found += 1
            kept += 1
            tips.append({
                "outlet": outlet,
                "headline": article["title"],
                "summary": article["summary"][:300],
                "url": article_url,
                "date": article["date"],
                **match,
            })
    return kept


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
                    for name in podcast_candidates(item["title"], item["summary"]):
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
            for name in podcast_candidates(item["title"], item["summary"]):
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
    "be": ["standaard.be", "demorgen.be", "hln.be", "vrt.be", "humo.be", "knack.be", "tijd.be"],
    "de": ["zeit.de", "spiegel.de", "sueddeutsche.de", "faz.net", "tagesspiegel.de",
           "deutschlandfunk.de", "deutschlandfunkkultur.de", "br.de", "ndr.de", "wdr.de",
           "stern.de", "taz.de", "detektor.fm", "podwatch.io"],
    "gb": ["theguardian.com", "radiotimes.com", "bbc.co.uk", "independent.co.uk", "telegraph.co.uk"],
    "us": ["podcastreview.org", "vulture.com", "nytimes.com", "theatlantic.com", "npr.org", "time.com"],
    "fr": ["telerama.fr", "lemonde.fr", "liberation.fr", "radiofrance.fr", "lesinrocks.com", "franceinfo.fr"],
    "es": ["elpais.com", "elmundo.es", "rtve.es", "eldiario.es", "lavanguardia.com", "elconfidencial.com"],
    "it": ["ilpost.it", "repubblica.it", "corriere.it", "internazionale.it", "rainews.it"],
    "se": ["dn.se", "svd.se", "sverigesradio.se", "svt.se", "aftonbladet.se", "expressen.se"],
    "dk": ["politiken.dk", "dr.dk", "berlingske.dk", "information.dk", "jyllands-posten.dk"],
    "no": ["nrk.no", "aftenposten.no", "vg.no", "dagbladet.no", "morgenbladet.no"],
    "ie": ["irishtimes.com", "rte.ie", "independent.ie", "thejournal.ie"],
    "ca": ["cbc.ca", "theglobeandmail.com", "thestar.com", "macleans.ca"],
    "au": ["abc.net.au", "smh.com.au", "theguardian.com", "theage.com.au", "news.com.au"],
    "br": ["folha.uol.com.br", "g1.globo.com", "estadao.com.br", "uol.com.br", "oglobo.globo.com"],
    "mx": ["eluniversal.com.mx", "milenio.com", "eleconomista.com.mx", "reforma.com"],
    "jp": ["nhk.or.jp", "asahi.com", "yomiuri.co.jp", "nikkei.com"],
    "in": ["thehindu.com", "indianexpress.com", "hindustantimes.com", "scroll.in"],
}

# Paden waarachter een podcastrubriek pleegt te zitten, in de talen die we raken.
PROSPECT_PATHS = [
    "/podcast", "/podcasts", "/tag/podcast", "/tag/podcasts", "/thema/podcast",
    "/thema/podcastgids", "/onderwerp/podcast", "/tags/podcasts", "/topic/podcasts",
    "/podcast-tipps", "/thema/podcast-tipps", "/kultur/podcast", "/cultuur/podcast",
    "/podcasttips", "/podkast", "/poddar", "/poddradio", "/podcasts-tips",
    "/culture/podcasts", "/arts/podcasts", "/radio/podcasts", "/audio/podcasts",
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


def write_tips(root: pathlib.Path, country: str) -> int:
    tips = collect_tips(country)
    folder = root / "tips"
    folder.mkdir(parents=True, exist_ok=True)
    (folder / f"{country}.json").write_text(json.dumps({
        "country": country, "updated": NOW, "count": len(tips),
        "outlets": sorted({t["outlet"] for t in tips}),
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

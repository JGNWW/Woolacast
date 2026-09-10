#!/usr/bin/env python3
"""
Verzamelt wat de app niet zelf kan ophalen. Twee klussen, met heel andere kosten:

  snapshot  Ranglijsten van Apple en Spotify vastleggen. Twee aanroepen per
            lijst. Dit is goedkoop en mag overal draaien. Het bestaat niet om
            de lijst zelf — die haalt de app zo op — maar om de *historie*:
            stijgers en dalers kun je niet met terugwerkende kracht bepalen.

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


def fetch(url: str, *, raw: bool = False, tries: int = 3, headers: dict | None = None):
    for attempt in range(tries):
        try:
            request = urllib.request.Request(
                url, headers={"User-Agent": UA, **(headers or {})})
            with urllib.request.urlopen(request, timeout=30) as response:
                body = response.read()
                final = response.geturl()
            return (body.decode("utf-8", "replace"), final) if raw else json.loads(body)
        except Exception:
            if attempt == tries - 1:
                return None
            time.sleep(1.5 * (attempt + 1))
    return None


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

        if collected:
            write_movers(root, country, collected)


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
    else:
        run_episodes(countries, args.limit, args.workers, root)

    write_index(root)
    return 0


if __name__ == "__main__":
    sys.exit(main())

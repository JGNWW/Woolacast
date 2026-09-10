#!/usr/bin/env python3
"""
Haalt Apple's echte afleveringen-ranglijst per categorie op en schrijft die weg
als JSON.

Waarom dit bestaat: de ranglijst zelf is publiek en sleutelloos, maar geeft
alleen ids, en afleverings-ids zijn nergens in bulk op te lossen. Elk id kost
één aanroep. Een telefoon kan dat niet per lijst doen; een klusje dat één keer
per dag draait wel.

Gebruik:
    python3 collect.py --country nl --limit 50 --out charts
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
CHARTS = "https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/charts"
LOOKUP = "https://itunes.apple.com/lookup"
EPISODE = "https://podcasts.apple.com/{cc}/podcast/id0?i={id}"

# Genre-ids van de iTunes-catalogus; 26 is de wortel, oftewel alle categorieen.
GENRES = {
    26: "Alle categorieen", 1301: "Kunst", 1321: "Zaken", 1303: "Comedy",
    1304: "Educatie", 1483: "Fictie", 1511: "Overheid", 1512: "Gezondheid & fitness",
    1487: "Geschiedenis", 1305: "Kinderen & gezin", 1502: "Vrije tijd", 1310: "Muziek",
    1489: "Nieuws", 1314: "Religie & spiritualiteit", 1533: "Wetenschap",
    1324: "Maatschappij & cultuur", 1545: "Sport", 1309: "TV & film",
    1318: "Technologie", 1488: "True crime",
}

SERVER_DATA = re.compile(
    r'<script type="application/json" id="serialized-server-data">(.*?)</script>', re.S)
SHOW_ID = re.compile(r"/id(\d+)")


def fetch(url: str, *, raw: bool = False, tries: int = 3):
    for attempt in range(tries):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(request, timeout=30) as response:
                body = response.read()
                final = response.geturl()
            return (body.decode("utf-8", "replace"), final) if raw else json.loads(body)
        except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError, TimeoutError):
            if attempt == tries - 1:
                return None
            time.sleep(1.5 * (attempt + 1))
    return None


def episode_header(payload) -> dict | None:
    """Zoekt het EpisodeHeader-blok in de paginadata."""
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
    """Eén aanroep levert titel, duur, datum én — via de redirect — het show-id."""
    result = fetch(EPISODE.format(cc=country, id=episode_id), raw=True)
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
        "episodeNumber": header.get("episodeNumber"),
    }


def shows_by_id(country: str, show_ids: list[str]) -> dict:
    """Eén batch-lookup per honderd shows geeft naam, artwork en feed-URL."""
    out = {}
    for start in range(0, len(show_ids), 100):
        batch = show_ids[start:start + 100]
        query = urllib.parse.urlencode({"id": ",".join(batch), "country": country})
        payload = fetch(f"{LOOKUP}?{query}")
        for result in (payload or {}).get("results", []):
            out[str(result.get("collectionId"))] = {
                "showTitle": result.get("collectionName"),
                "publisher": result.get("artistName"),
                "artworkUrl": result.get("artworkUrl600") or result.get("artworkUrl100"),
                "feedUrl": result.get("feedUrl"),
            }
    return out


def collect(country: str, genre_id: int, limit: int, workers: int) -> dict | None:
    query = urllib.parse.urlencode(
        {"cc": country, "g": genre_id, "name": "PodcastEpisodes", "limit": limit})
    ids = (fetch(f"{CHARTS}?{query}") or {}).get("resultIds", [])
    if not ids:
        return None

    with ThreadPoolExecutor(max_workers=workers) as pool:
        resolved = list(pool.map(lambda i: resolve_episode(country, i), ids))

    episodes = [(rank, ep) for rank, ep in enumerate(resolved, 1) if ep]
    shows = shows_by_id(country, sorted({e["showId"] for _, e in episodes if e["showId"]}))

    entries = []
    for rank, episode in episodes:
        show = shows.get(episode["showId"] or "", {})
        entries.append({
            "rank": rank,
            **episode,
            "showTitle": show.get("showTitle"),
            "publisher": show.get("publisher"),
            "artworkUrl": show.get("artworkUrl"),
            "feedUrl": show.get("feedUrl"),
        })

    return {
        "source": "apple",
        "level": "episodes",
        "country": country,
        "genreId": genre_id,
        "genreLabel": GENRES.get(genre_id, str(genre_id)),
        "updated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "requested": len(ids),
        "resolved": len(entries),
        "entries": entries,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--country", default="nl")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--genres", default="all", help="'all' of komma-lijst met genre-ids")
    parser.add_argument("--out", default="charts")
    args = parser.parse_args()

    wanted = list(GENRES) if args.genres == "all" else [int(g) for g in args.genres.split(",")]
    root = pathlib.Path(args.out)
    manifest = []

    for genre_id in wanted:
        started = time.time()
        chart = collect(args.country, genre_id, args.limit, args.workers)
        if not chart:
            print(f"  {genre_id:5} {GENRES.get(genre_id, ''):24} geen lijst", flush=True)
            continue

        target = root / args.country / f"{genre_id}" / "episodes.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(chart, ensure_ascii=False, separators=(",", ":")))

        manifest.append({
            "country": args.country, "genreId": genre_id,
            "genreLabel": chart["genreLabel"], "level": "episodes",
            "path": f"{args.country}/{genre_id}/episodes.json",
            "resolved": chart["resolved"], "updated": chart["updated"],
        })
        print(f"  {genre_id:5} {chart['genreLabel']:24} "
              f"{chart['resolved']:3}/{chart['requested']:3}  {time.time() - started:.0f}s", flush=True)

    if not manifest:
        print("niets verzameld", file=sys.stderr)
        return 1

    index = root / "index.json"
    existing = json.loads(index.read_text())["charts"] if index.exists() else []
    keep = [c for c in existing
            if not any(c["country"] == m["country"] and c["genreId"] == m["genreId"]
                       and c["level"] == m["level"] for m in manifest)]
    index.write_text(json.dumps(
        {"updated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
         "charts": sorted(keep + manifest, key=lambda c: (c["country"], c["genreId"]))},
        ensure_ascii=False, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Hoeveel podcasts in de Apple-top-200 leveren transcripties en hoofdstukken in hun feed?

Dat is de meting die de criticus vroeg voordat meelezen gebouwd werd: de app
transcribeert niet zelf, dus meelezen kan alleen waar de maker tekst meelevert.

    python3 tools/extras_coverage.py nl us

Per land: de top 200 van Apple (alle categorieën), de feed van elke show, en van
de nieuwste vijf afleveringen of ze podcast:transcript, podcast:chapters of
Podlove-hoofdstukken (psc:chapters) hebben. Hoofdstukken in de ID3-kop van het
mp3-bestand telt dit niet mee; die ziet de app pas bij het afspelen.
"""
import json
import sys
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

UA = {"User-Agent": "Toadcast-charts/1.0 (+https://github.com/jgnww/Woolacast)"}
PODCAST_NS = "https://podcastindex.org/namespace/1.0"
PSC_NS = "http://podlove.org/simple-chapters"
NEWEST = 5


def get(url, timeout=20):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=timeout) as r:
        return r.read()


def top_feeds(country):
    ids = json.loads(get(f"https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/charts?cc={country}&g=26&name=Podcasts&limit=200"))["resultIds"]
    feeds = []
    for i in range(0, len(ids), 100):
        batch = ",".join(ids[i:i + 100])
        results = json.loads(get(f"https://itunes.apple.com/lookup?id={batch}&country={country}"))["results"]
        feeds += [r["feedUrl"] for r in results if r.get("feedUrl")]
    return ids, feeds


def extras(feed):
    """(heeft transcript, heeft hoofdstukken) per nieuwste aflevering, of None als de feed niet te lezen is."""
    try:
        root = ET.fromstring(get(feed))
    except Exception:
        return None
    items = root.findall("./channel/item")[:NEWEST]
    out = []
    for item in items:
        transcript = item.find(f"{{{PODCAST_NS}}}transcript") is not None
        chapters = item.find(f"{{{PODCAST_NS}}}chapters") is not None or item.find(f"{{{PSC_NS}}}chapters") is not None
        out.append((transcript, chapters))
    return out


def pct(n, d):
    return f"{100 * n / d:.0f}%" if d else "–"


def main(countries):
    print("| Land | Shows met feed | Gelezen | Show met transcript (nieuwste afl.) | Afleveringen met transcript | Show met hoofdstukken | Afleveringen met hoofdstukken |")
    print("| --- | --- | --- | --- | --- | --- | --- |")
    for country in countries:
        ids, feeds = top_feeds(country)
        with ThreadPoolExecutor(12) as pool:
            results = [r for r in pool.map(extras, feeds) if r]
        episodes = [e for r in results for e in r]
        shows_t = sum(1 for r in results if r and r[0][0])
        shows_c = sum(1 for r in results if r and r[0][1])
        eps_t = sum(1 for t, _ in episodes if t)
        eps_c = sum(1 for _, c in episodes if c)
        print(f"| {country.upper()} | {len(feeds)} van {len(ids)} | {len(results)} | {shows_t} ({pct(shows_t, len(results))}) | "
              f"{eps_t} van {len(episodes)} ({pct(eps_t, len(episodes))}) | {shows_c} ({pct(shows_c, len(results))}) | "
              f"{eps_c} van {len(episodes)} ({pct(eps_c, len(episodes))}) |")


if __name__ == "__main__":
    main(sys.argv[1:] or ["nl", "us"])

#!/usr/bin/env python3
"""
Tekent de knoppen van de mediamelding als VectorDrawables: snelheid (één per
stap uit SPEEDS), terug, afspelen/pauzeren, vooruit en de ster.

De maten zijn afgenomen van een schermafbeelding van de mediabediening op een
Samsung-toestel (One UI) en omgerekend naar een vak van 24 dp: open ronde
pijlen zonder getal erin, twee afgeronde strepen voor pauze, een pentagram als
ster en de snelheid als kale tekst ("1x").

De cijfers op de snelheidsknop worden uit Inter Bold gehaald en als contouren
in de XML gebakken; het lettertype zelf gaat niet mee in de app.

Gebruik:  python3 tools/notification_icons.py pad/naar/Inter-Bold.ttf
          (Inter: https://fonts.google.com/specimen/Inter, gewicht 700)
"""
import math
import pathlib
import sys

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res" / "drawable"

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Gegenereerd door tools/notification_icons.py — daar aanpassen, niet hier. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
"""

# Eén lijndikte voor alles wat een lijn is, zoals op de voorbeeldmelding.
STROKE = 2.0


def fmt(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s == "-0" else s


def stroked(d, width=STROKE, join="round"):
    return (
        "    <path\n"
        '        android:strokeColor="#FFFFFFFF"\n'
        f'        android:strokeWidth="{fmt(width)}"\n'
        '        android:strokeLineCap="round"\n'
        f'        android:strokeLineJoin="{join}"\n'
        f'        android:pathData="{d}" />\n'
    )


def filled(d, even_odd=False):
    rule = '        android:fillType="evenOdd"\n' if even_odd else ""
    return (
        "    <path\n"
        '        android:fillColor="#FFFFFFFF"\n'
        f"{rule}"
        f'        android:pathData="{d}" />\n'
    )


def write(name, body):
    (RES / f"{name}.xml").write_text(HEADER + body + "</vector>\n")
    print("  ", name)


# ---- terug en vooruit ------------------------------------------------------
# Een open cirkel van driekwart die bovenin overgaat in een korte schacht met
# een pijlpunt: geen getal erin.

RING_CX, RING_CY, RING_R = 12.0, 13.8, 7.3
TIP_X = 8.1            # punt van de pijl
ARM_X, ARM_DY = 11.7, 3.6  # uiteinden van de pijlpunt


def skip_path(mirror):
    def x(v):
        return 24 - v if mirror else v

    top = RING_CY - RING_R
    left = RING_CX - RING_R
    sweep = 1 if mirror else 0
    return (
        f"M{fmt(x(left))},{fmt(RING_CY)}"
        f"A{fmt(RING_R)},{fmt(RING_R)} 0 1,{sweep} {fmt(x(RING_CX))},{fmt(top)}"
        f"L{fmt(x(TIP_X))},{fmt(top)}"
        f"M{fmt(x(ARM_X))},{fmt(top - ARM_DY)}"
        f"L{fmt(x(TIP_X))},{fmt(top)}"
        f"L{fmt(x(ARM_X))},{fmt(top + ARM_DY)}"
    )


# ---- afspelen en pauzeren --------------------------------------------------
# Het systeem tekent afspelen/pauzeren vanaf Android 13 zelf; deze twee zijn
# voor de melding op oudere toestellen, en tekenen hetzelfde als daar.

PAUSE_CY = 11.8
PAUSE_HEIGHT = 15.55


def pause_path():
    bar_w, gap, height = 3.65, 4.65, PAUSE_HEIGHT
    half = (gap + bar_w) / 2
    top, bottom = PAUSE_CY - height / 2, PAUSE_CY + height / 2
    r = bar_w / 2
    parts = []
    for cx in (12 - half, 12 + half):
        l, rr = cx - r, cx + r
        parts.append(
            f"M{fmt(l)},{fmt(top + r)}"
            f"A{fmt(r)},{fmt(r)} 0 0,1 {fmt(rr)},{fmt(top + r)}"
            f"L{fmt(rr)},{fmt(bottom - r)}"
            f"A{fmt(r)},{fmt(r)} 0 0,1 {fmt(l)},{fmt(bottom - r)}Z"
        )
    return "".join(parts)


def play_path():
    # Zelfde hoogte als de pauzestrepen, met de hoeken even rond als hun kop.
    # Het zwaartepunt ligt iets rechts van het midden, zoals bij elke speler.
    h = PAUSE_HEIGHT - STROKE
    w = h * math.sqrt(3) / 2
    x0 = 12 - w / 2 + 0.9
    return (
        f"M{fmt(x0)},{fmt(PAUSE_CY - h / 2)}"
        f"L{fmt(x0 + w)},{fmt(PAUSE_CY)}"
        f"L{fmt(x0)},{fmt(PAUSE_CY + h / 2)}Z"
    )


# ---- de ster ---------------------------------------------------------------
# Een regelmatige ster van vijf punten, alleen de omtrek, met scherpe punten.

STAR_R = 10.85
STAR_CY = 11.95
STAR_LINE = 1.75
# Iets voller dan een zuiver pentagram, zoals op de voorbeeldmelding.
STAR_INNER = 0.45


def star_points(radius, inner_ratio):
    pts = []
    for i in range(10):
        ang = math.radians(-90 + i * 36)
        r = radius if i % 2 == 0 else radius * inner_ratio
        pts.append((12 + r * math.cos(ang), STAR_CY + r * math.sin(ang)))
    return pts


def inset(poly, d):
    """Schuift elke zijde van een (met de klok mee lopende) veelhoek d naar binnen."""
    n = len(poly)
    lines = []
    for i in range(n):
        (x1, y1), (x2, y2) = poly[i], poly[(i + 1) % n]
        dx, dy = x2 - x1, y2 - y1
        ln = math.hypot(dx, dy)
        nx, ny = -dy / ln, dx / ln  # naar binnen bij kloksgewijs in y-omlaag
        lines.append(((x1 + nx * d, y1 + ny * d), (dx, dy)))
    out = []
    for i in range(n):
        (p, r), (q, s) = lines[i - 1], lines[i]
        cross = r[0] * s[1] - r[1] * s[0]
        t = ((q[0] - p[0]) * s[1] - (q[1] - p[1]) * s[0]) / cross
        out.append((p[0] + t * r[0], p[1] + t * r[1]))
    return out


def poly_path(pts):
    head, *rest = pts
    return f"M{fmt(head[0])},{fmt(head[1])}" + "".join(
        f"L{fmt(x)},{fmt(y)}" for x, y in rest) + "Z"


def star_outline():
    outer = star_points(STAR_R, STAR_INNER)
    return poly_path(outer) + poly_path(inset(outer, STAR_LINE))


def star_solid():
    return poly_path(star_points(STAR_R, STAR_INNER))


# ---- snelheid --------------------------------------------------------------
# Kale tekst, links in het vak zoals op de voorbeeldmelding. Brede labels
# ("1,5x") krimpen tot ze passen.

CAP_HEIGHT = 9.85
BASELINE = 18.05
TEXT_LEFT = 0.0
MAX_WIDTH = 23.6
SPEEDS = [("0_8", "0,8x"), ("1_0", "1x"), ("1_2", "1,2x"),
          ("1_5", "1,5x"), ("1_8", "1,8x"), ("2_0", "2x")]


def text_path(font, text):
    cmap = font.getBestCmap()
    glyphs = font.getGlyphSet()
    hmtx = font["hmtx"]
    cap = font["OS/2"].sCapHeight
    names = [cmap[ord(c)] for c in text]

    # Breedte van de inkt, niet van de voortgang: de eerste en laatste letter
    # tellen vanaf hun eigen rand.
    advance = sum(hmtx[n][0] for n in names)
    lsb_first = hmtx[names[0]][1]
    glyf = font["glyf"]
    last = glyf[names[-1]]
    ink = advance - lsb_first - (hmtx[names[-1]][0] - last.xMax)

    scale = CAP_HEIGHT / cap
    if ink * scale > MAX_WIDTH:
        scale = MAX_WIDTH / ink
    x = TEXT_LEFT / scale - lsb_first
    pen = SVGPathPen(glyphs, ntos=fmt)
    for n in names:
        tp = TransformPen(pen, (scale, 0, 0, -scale, x * scale, BASELINE))
        glyphs[n].draw(tp)
        x += hmtx[n][0]
    return pen.getCommands()


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    font = TTFont(sys.argv[1])

    write("ic_notif_skip_back", stroked(skip_path(mirror=False)))
    write("ic_notif_skip_forward", stroked(skip_path(mirror=True)))
    write("ic_notif_pause", filled(pause_path()))
    write("ic_notif_play", filled(play_path()) + stroked(play_path(), STROKE))
    write("ic_notif_star", filled(star_outline(), even_odd=True))
    write("ic_notif_star_filled", filled(star_solid()))
    for key, label in SPEEDS:
        write(f"ic_notif_speed_{key}", filled(text_path(font, label)))


if __name__ == "__main__":
    main()

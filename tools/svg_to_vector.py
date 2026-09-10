#!/usr/bin/env python3
"""
Zet de mascotte uit design/icon/parts/*.svg om naar Android VectorDrawables
voor het adaptive icon. Bakt de schaal naar de veilige zone (66 dp van 108 dp)
in de coordinaten, zodat er in de XML geen group-transformaties nodig zijn.

Gebruik:  python3 tools/svg_to_vector.py
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
PARTS = ROOT / "design" / "icon" / "parts"
RES = ROOT / "app" / "src" / "main" / "res" / "drawable"

# Overall factor 0.72 houdt de breedste punten (de oorkussens) binnen de
# gegarandeerde cirkel van 66 dp; dy = -3 zet het geheel weer in het midden.
SCALE, DX, DY = 0.72, 0.0, -3.0
CENTER = 256.0

PALETTE = {
    "--m-body": "#C9946E", "--m-body2": "#E7C9A9", "--m-wart": "#B27E5B",
    "--m-line": "#96603E", "--m-iris": "#E8B84B", "--m-pupil": "#4A3324",
    "--m-eye": "#FFFDFA", "--m-cup": "#9AC2E7", "--m-cup2": "#86B0DA",
    "--m-pad": "#F7EFE2", "--m-band": "#A9CCBE", "--m-band2": "#C6DFD4",
    # de monochrome laag wordt door de launcher zelf ingekleurd
    "--mo-fg": "#FFFFFF", "--mo-bg": "#00000000",
}

BACKGROUND = "#2C4A46"


def tx(x):
    return (float(x) - CENTER) * SCALE + CENTER + DX


def ty(y):
    return (float(y) - CENTER) * SCALE + CENTER + DY


def num(value):
    return f"{value:.2f}".rstrip("0").rstrip(".")


def transform_path(d):
    """Alle padcommando's in de bron zijn absoluut (M, C, L, Z)."""
    out = []
    for token in re.findall(r"[MCLZmclz]|-?\d*\.?\d+", d):
        out.append(token)
    result, index, axis_x = [], 0, True
    while index < len(out):
        token = out[index]
        if token in "MCLZmclz":
            result.append(token)
            axis_x = True
            index += 1
            continue
        result.append(num(tx(token) if axis_x else ty(token)))
        axis_x = not axis_x
        index += 1
    # commando's en getallen weer aan elkaar met spaties
    text = ""
    for token in result:
        text += token if token in "MCLZmclz" else " " + token
    return text.strip()


def circle_path(cx, cy, r):
    return ellipse_path(cx, cy, r, r)


def ellipse_path(cx, cy, rx, ry):
    x, y = tx(cx), ty(cy)
    a, b = float(rx) * SCALE, float(ry) * SCALE
    return (
        f"M{num(x - a)},{num(y)}"
        f"a{num(a)},{num(b)} 0 1,0 {num(2 * a)},0"
        f"a{num(a)},{num(b)} 0 1,0 {num(-2 * a)},0Z"
    )


def color(raw):
    match = re.match(r"var\((--[\w-]+)\)", raw or "")
    return PALETTE.get(match.group(1), "#000000") if match else (raw or "#000000")


def parse(svg_text):
    """Levert de tekenopdrachten in bronvolgorde."""
    shapes = []
    for tag in re.findall(r"<(path|circle|ellipse)\b([^>]*)/>", svg_text):
        name, attrs = tag
        get = lambda key: (re.search(rf'{key}="([^"]*)"', attrs) or [None, None])[1]
        if name == "path":
            data = transform_path(get("d"))
        elif name == "circle":
            data = circle_path(get("cx"), get("cy"), get("r"))
        else:
            data = ellipse_path(get("cx"), get("cy"), get("rx"), get("ry"))
        role = (re.search(r'data-role="([^"]*)"', attrs) or [None, None])[1]
        shapes.append({
            "role": role,
            "d": data,
            "fill": get("fill"),
            "stroke": get("stroke"),
            "width": get("stroke-width"),
            "cap": get("stroke-linecap"),
        })
    return shapes


def path_xml(shape):
    lines = [f'        android:pathData="{shape["d"]}"']
    if shape["fill"] and shape["fill"] != "none":
        lines.insert(0, f'        android:fillColor="{color(shape["fill"])}"')
    if shape["stroke"]:
        lines.append(f'        android:strokeColor="{color(shape["stroke"])}"')
        lines.append(f'        android:strokeWidth="{num(float(shape["width"]) * SCALE)}"')
        if shape["cap"]:
            lines.append(f'        android:strokeLineCap="{shape["cap"]}"')
    return "    <path\n" + "\n".join(lines) + " />"


def vector(paths, size="108dp"):
    body = "\n".join(paths)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{size}"\n'
        f'    android:height="{size}"\n'
        '    android:viewportWidth="512"\n'
        '    android:viewportHeight="512">\n'
        f"{body}\n"
        "</vector>\n"
    )


def build_foreground():
    shapes = parse((PARTS / "mascot.svg").read_text())
    return vector([path_xml(s) for s in shapes])


def build_monochrome():
    """
    Een thema-icoon wordt door de launcher ingekleurd; alleen transparantie
    blijft transparant. De gaten (ogen, ring om de schelpen) moeten daarom
    echte gaten zijn: een enkel pad met evenOdd, niet vlakken in de
    achtergrondkleur er overheen. data-role in de bron zegt wat wat is.
    """
    shapes = parse((PARTS / "mono.svg").read_text())
    by_role = lambda role: [s for s in shapes if s["role"] == role]

    head = by_role("head")[0]["d"]
    holes = "".join(s["d"] for s in by_role("hole"))

    paths = [path_xml({**by_role("band")[0], "fill": None})]
    paths.append(
        '    <path\n'
        '        android:fillColor="#FFFFFFFF"\n'
        '        android:fillType="evenOdd"\n'
        f'        android:pathData="{head}{holes}" />'
    )
    paths += [
        f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{s["d"]}" />'
        for s in by_role("cup")
    ]
    return vector(paths)


def build_background():
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="512"\n'
        '    android:viewportHeight="512">\n'
        f'    <path\n        android:fillColor="{BACKGROUND}"\n'
        '        android:pathData="M0,0h512v512h-512z" />\n'
        "</vector>\n"
    )


def main():
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_background.xml").write_text(build_background())
    (RES / "ic_launcher_foreground.xml").write_text(build_foreground())
    (RES / "ic_launcher_monochrome.xml").write_text(build_monochrome())
    print("geschreven naar", RES)


if __name__ == "__main__":
    main()

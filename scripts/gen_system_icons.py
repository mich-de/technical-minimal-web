#!/usr/bin/env python3
"""Icone di sistema di LeanBox in stile Technical Minimal (stesso significato delle originali).

- Icone 24x24: tratto 1.6 a punte arrotondate, dettagli a punto pieno, bianche (il codice le tinge
  col colore del tema).
- Banner degli ingressi TV 320x180: pittogramma hairline arancio + etichetta a matrice di punti.
- Icona dell'app: "L" a matrice di punti + LED arancio su fondo scuro (adaptive icon).

Uso: python3 -I gen_system_icons.py --out <app>/src/main/res/drawable
Il set ICONS è quello di LeanBox: per un'altra app copia lo script e cambia ICONS/BANNERS/FONT,
le primitive (dot, rrect, circle, gear, wifi, stroke_path, fill_path, dot_text) restano uguali.
"""
import math
import os
import sys

RES = sys.argv[sys.argv.index("--out") + 1] if "--out" in sys.argv else "drawable"
INK = "#FFFFFFFF"
ACCENT = "#FFFF5A1F"
SW = 1.6


def dot(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0Z"


def rrect(x, y, w, h, r):
    return (f"M{x + r},{y}h{w - 2 * r}a{r},{r} 0 0,1 {r},{r}v{h - 2 * r}a{r},{r} 0 0,1 {-r},{r}"
            f"h{-(w - 2 * r)}a{r},{r} 0 0,1 {-r},{-r}v{-(h - 2 * r)}a{r},{r} 0 0,1 {r},{-r}Z")


def circle(cx, cy, r):
    return dot(cx, cy, r)


def gear():
    pts = []
    for i in range(16):
        a0 = math.radians(i * 22.5 - 6)
        a1 = math.radians(i * 22.5 + 6)
        r = 9 if i % 2 == 0 else 6.8
        pts.append((12 + r * math.cos(a0), 12 + r * math.sin(a0)))
        pts.append((12 + r * math.cos(a1), 12 + r * math.sin(a1)))
    return "M" + "L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + "Z"


def wifi(slashed):
    arcs = ("M3.5,10.5A12,12 0 0,1 20.5,10.5 M6.3,13.3A8,8 0 0,1 17.7,13.3 M9.2,16.2A4,4 0 0,1 14.8,16.2"
            + (" M4,3.5L20,19.5" if slashed else ""))
    return arcs, [dot(12, 19, 1.4)]


# nome: (tratti, punti pieni)
ICONS = {
    "ic_action_se_all_apps": ("", [dot(x, y, 1.7) for y in (6, 12, 18) for x in (6, 12, 18)]),
    "ic_tool_backup": ("M4,14v4a2,2 0 0,0 2,2h12a2,2 0 0,0 2,-2v-4 M12,4v11 M7.5,8.5L12,4L16.5,8.5", []),
    "ic_sb_connection_eth": (rrect(4, 6, 16, 11, 1.5) + " M9,20h6 M9,17v3 M15,17v3 M9,10v2.5 M12,10v2.5 M15,10v2.5", []),
    "ic_action_se_and_menu": ("M4,7h16 M4,12h16 M4,17h16", [dot(9, 7, 2.2), dot(15, 12, 2.2), dot(7, 17, 2.2)]),
    "ic_action_di_settings": (rrect(3, 4.5, 18, 12, 1.5) + " M9,20h6 M12,16.5v3.5 " + circle(12, 10.5, 2.4), []),
    "ic_action_ap_remote": (rrect(7, 2.5, 10, 19, 2.5) + " " + circle(12, 8, 2.6),
                            [dot(10, 13.5, 1), dot(14, 13.5, 1), dot(10, 17, 1), dot(14, 17, 1)]),
    "ic_tool_wallpaper": (rrect(3, 5, 18, 14, 1.5) + " M3.5,17l5,-5l4,4l3,-3l5,5", [dot(16, 9, 1.5)]),
    "ic_tool_security": ("M12,3l7,3v5c0,4.5 -3,8 -7,10c-4,-2 -7,-5.5 -7,-10v-5z M12,12.5v3", [dot(12, 11, 1.4)]),
    "ic_sb_connection_wifi_no_internet": wifi(True),
    "ic_sb_connection_wifi_4": wifi(False),
    "ic_app_uninstall": ("M4.5,7h15 M10,7V5h4v2 M6.5,7l1,13h9l1,-13 M10.5,11v5 M13.5,11v5", []),
    "ic_action_sy_props": (circle(12, 12, 9) + " M12,11v6", [dot(12, 7.8, 1.2)]),
    "ic_action_se_settings": (gear() + " " + circle(12, 12, 2.8), []),
    "ic_action_in_src": ("M8,5h10a2,2 0 0,1 2,2v10a2,2 0 0,1 -2,2h-10 M3,12h10 M9.5,8.5L13,12L9.5,15.5", []),
    "ic_tool_display": (rrect(3, 4.5, 18, 12, 1.5) + " M9,20h6 M12,16.5v3.5 " + circle(12, 10.5, 3.4),
                        ["M12,7.1v6.8a3.4,3.4 0 0,0 0,-6.8z"]),
    "ic_input_av": (circle(6, 12, 2.7) + " " + circle(12, 12, 2.7) + " " + circle(18, 12, 2.7),
                    [dot(6, 12, 0.9), dot(12, 12, 0.9), dot(18, 12, 0.9)]),
    "ic_app_hide": ("M2.5,12c2.5,-4.5 5.8,-6.5 9.5,-6.5s7,2 9.5,6.5c-2.5,4.5 -5.8,6.5 -9.5,6.5s-7,-2 -9.5,-6.5z "
                    + circle(12, 12, 2.8) + " M4,4l16,16", []),
    "ic_action_sy_accessibility": (circle(12, 12, 9.5) + " M7.5,10h9 M12,10v4 M12,14l-2.5,4 M12,14l2.5,4",
                                   [dot(12, 7.2, 1.3)]),
    "ic_action_se_eng_menu": (rrect(7, 7, 10, 10, 1.5) + " M10,4v3 M14,4v3 M10,17v3 M14,17v3 M4,10h3 M4,14h3 M17,10h3 M17,14h3",
                              [dot(12, 12, 1.4)]),
    "ic_action_se_categories": ("M9,7h11 M9,12h11 M9,17h11", [dot(5, 7, 1.4), dot(5, 12, 1.4), dot(5, 17, 1.4)]),
    "ic_action_se_additional_menu": (rrect(3, 7, 18, 10, 2.5), [dot(8, 12, 1.4), dot(12, 12, 1.4), dot(16, 12, 1.4)]),
}

FONT = {
    "A": "01110 10001 10001 11111 10001 10001 10001", "C": "01110 10001 10000 10000 10000 10001 01110",
    "D": "11110 10001 10001 10001 10001 10001 11110", "H": "10001 10001 10001 11111 10001 10001 10001",
    "I": "01110 00100 00100 00100 00100 00100 01110", "L": "10000 10000 10000 10000 10000 10000 11111",
    "M": "10001 11011 10101 10101 10001 10001 10001", "R": "11110 10001 10001 11110 10100 10010 10001",
    "S": "01111 10000 10000 01110 00001 00001 11110", "V": "10001 10001 10001 10001 10001 01010 00100",
    "1": "00100 01100 00100 00100 00100 00100 01110", "2": "01110 10001 00001 00010 00100 01000 11111",
    "3": "11111 00010 00100 00010 00001 10001 01110", " ": "000 000 000 000 000 000 000",
}

# Pittogrammi dei banner (viewport 320x180, tratto arancio)
HDMI_PORT = "M118,26h84v20l-12,14h-60l-12,-14z M134,40h52"
AV_JACKS = " ".join(circle(x, 42, 14) for x in (124, 160, 196))
SRC_ARROW = "M138,24h40a8,8 0 0,1 8,8v24a8,8 0 0,1 -8,8h-40 M120,44h40 M148,32L160,44L148,56"
BANNERS = {
    "banner_hdmi1": ("HDMI 1", HDMI_PORT), "banner_hdmi2": ("HDMI 2", HDMI_PORT),
    "banner_hdmi3": ("HDMI 3", HDMI_PORT), "banner_av": ("AV", AV_JACKS), "banner_src": ("SRC", SRC_ARROW),
}


def vector(w, h, vw, vh, body):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- Generato da tools/gen_system_icons.py: non modificare a mano -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{w}dp" android:height="{h}dp"\n'
            f'    android:viewportWidth="{vw}" android:viewportHeight="{vh}">\n{body}</vector>\n')


def stroke_path(d, color, width):
    return (f'    <path android:pathData="{d}" android:strokeColor="{color}" android:strokeWidth="{width}"\n'
            f'        android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"/>\n')


def fill_path(d, color):
    return f'    <path android:pathData="{d}" android:fillColor="{color}"/>\n'


def dot_text(text, cx, top, pitch, r):
    glyphs = [FONT[c].split() for c in text]
    cols = sum(len(g[0]) for g in glyphs) + len(glyphs) - 1
    x = cx - (cols - 1) * pitch / 2
    out = []
    for g in glyphs:
        for j, row in enumerate(g):
            for i, bit in enumerate(row):
                if bit == "1":
                    out.append(dot(x + i * pitch, top + j * pitch, r))
        x += (len(g[0]) + 1) * pitch
    return "".join(out)


def main():
    os.makedirs(RES, exist_ok=True)
    for name, (strokes, dots) in ICONS.items():
        body = (stroke_path(strokes, INK, SW) if strokes else "") + "".join(fill_path(d, INK) for d in dots)
        with open(os.path.join(RES, f"{name}.xml"), "w") as f:
            f.write(vector(48, 48, 24, 24, body))

    for name, (label, picto) in BANNERS.items():
        body = (stroke_path(picto, ACCENT, 4) + fill_path(dot_text(label, 160, 96, 8, 3), INK))
        with open(os.path.join(RES, f"{name}.xml"), "w") as f:
            f.write(vector(320, 180, 320, 180, body))

    # Icona dell'app (adaptive): fondo scuro, "L" a punti, LED arancio
    with open(os.path.join(RES, "ic_launcher_background.xml"), "w") as f:
        f.write(vector(108, 108, 108, 108, fill_path("M0,0h108v108h-108z", "#FF0E0E0E")))
    with open(os.path.join(RES, "ic_launcher_foreground.xml"), "w") as f:
        f.write(vector(108, 108, 108, 108,
                       fill_path(dot_text("L", 52, 33, 7, 2.6), INK) + fill_path(dot(70, 36, 3), ACCENT)))
    print(f"{len(ICONS)} icone, {len(BANNERS)} banner, icona app")


if __name__ == "__main__":
    main()

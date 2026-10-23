#!/usr/bin/env python3
"""Icon pack "LeanBox Icons": icone vere delle app ridisegnate pixel per pixel in stile Technical Minimal.

Sorgenti (iconpack/tools/source):
- icone esportate dal box da ExportActivity (<pacchetto>__<activity>.png + components.tsv);
- icone delle app TV diffuse scaricate dal Play Store (play__<pacchetto>.png), con --fetch.

Per ogni icona: riconosce il colore di fondo (il più frequente sul bordo della forma), stacca il logo
con bordi morbidi, lo ricolora solo se serve (bianco su fondo colorato -> colore del marchio, scuro ->
chiaro) e lo posa su un riquadro scuro con hairline, trama a punti e LED arancio. Output PNG 432 px in
res/drawable-nodpi, più appfilter.xml e drawable.xml.

Uso (Pillow e numpy):
  python -I style_app_icons.py --source <dir sorgenti> --res <iconpack>/src/main/res [--fetch] [--mode faithful|extract]
Le sorgenti del box si ottengono con ExportActivity (templates/iconpack):
  adb shell am start -n <pack>/.ExportActivity
  adb pull /sdcard/Android/data/<pack>/files/export <dir sorgenti>
"""
import colorsys
import os
import re
import sys
import urllib.request

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def _arg(name, default):
    return sys.argv[sys.argv.index(name) + 1] if name in sys.argv else default


SOURCE = _arg("--source", "source")
# faithful = icona originale intatta + tocco di stile (predefinito); extract = logo staccato su riquadro scuro
MODE = _arg("--mode", "faithful")
RES = _arg("--res", "res")
S = 432

TILE = (20, 20, 20)
LINE = (51, 51, 51)
GRID = (30, 30, 30)
INK = (236, 235, 232)
LED = (255, 90, 31)

# App TV diffuse non installate: pacchetto -> activity principale nota (LeanBox abbina comunque per pacchetto)
POPULAR = {
    "com.google.android.youtube.tv": "com.google.android.apps.youtube.tv.activity.ShellActivity",
    "com.google.android.youtube.tvkids": "com.google.android.apps.youtube.tvkids.activity.MainActivity",
    "com.google.android.youtube.tvmusic": "com.google.android.apps.youtube.tvmusic.activity.MainActivity",
    "com.disney.disneyplus": "com.bamtechmedia.dominguez.main.MainActivity",
    "com.apple.atve.androidtv.appletv": "com.apple.atve.androidtv.appletv.MainActivity",
    "com.cbs.ott": "com.cbs.app.tv.ui.activity.SplashActivity",
    "com.dazn": "com.dazn.splash.view.SplashActivity",
    "tv.pluto.android": "tv.pluto.android.EntryPointActivity",
    "com.plexapp.android": "com.plexapp.plex.activities.SplashActivity",
    "org.videolan.vlc": "org.videolan.vlc.StartActivity",
    "com.mxtech.videoplayer.ad": "com.mxtech.videoplayer.ad.ActivityMediaList",
    "com.stremio.one": "com.stremio.tv.MainActivity",
    "org.jellyfin.androidtv": "org.jellyfin.androidtv.ui.startup.StartupActivity",
    "tv.emby.embyatv": "tv.emby.embyatv.startup.StartupActivity",
    "tv.twitch.android.app": "tv.twitch.android.app.core.LandingActivity",
    "com.crunchyroll.crunchyroid": "com.ellation.crunchyroll.presentation.startup.StartupActivity",
    "ar.tvplayer.tv": "ar.tvplayer.tv.ui.MainActivity",
    "com.nst.iptvsmarterstvbox": "com.nst.iptvsmarterstvbox.view.activity.SplashActivity",
    "com.spotify.tv.android": "com.spotify.tv.android.SpotifyTVActivity",
    "deezer.android.tv": "deezer.android.tv.MainActivity",
    "com.aspiro.tidal": "com.aspiro.wamp.MainActivity",
    "com.valvesoftware.steamlink": "com.valvesoftware.steamlink.SteamShellActivity",
    "com.limelight": "com.limelight.PcView",
    "com.nvidia.geforcenow": "com.nvidia.geforcenow.MallActivity",
    "com.retroarch": "com.retroarch.browser.mainmenu.MainMenuActivity",
    "com.lonelycatgames.Xplore": "com.lonelycatgames.Xplore.Browser",
    "com.yablio.sendfilestotv": "com.yablio.sendfilestotv.ui.MainActivity",
    "com.phlox.tvwebbrowser": "com.phlox.tvwebbrowser.activity.main.MainActivity",
    "com.surfshark.vpnclient.android": "com.surfshark.vpnclient.android.StartActivity",
    "com.anydesk.anydeskandroid": "com.anydesk.anydeskandroid.gui.activity.MainActivity",
    "org.zwanoo.android.speedtest": "com.ookla.mobile4.screens.main.MainActivity",
    "com.teslacoilsw.launcher": "com.teslacoilsw.launcher.NovaLauncher",
    "ch.rsi.player": "ch.srg.srgplayer.tv.MainActivity",
    "com.huhuitatv.app": "com.huhuitatv.app.MainActivity",
    "com.google.android.apps.tv.launcherx": "com.google.android.apps.tv.launcherx.home.HomeActivity",
}

SKIP = {"com.google.android.tv.axel"}  # schermata licenze, non un'app


def fetch_play():
    for pkg in POPULAR:
        dst = os.path.join(SOURCE, f"play__{pkg}.png")
        if os.path.exists(dst):
            continue
        try:
            req = urllib.request.Request(f"https://play.google.com/store/apps/details?id={pkg}&hl=it",
                                         headers={"User-Agent": "Mozilla/5.0"})
            html = urllib.request.urlopen(req, timeout=15).read().decode("utf-8", "ignore")
            m = re.search(r'property="og:image" content="([^"]+)"', html)
            if not m:
                print(f"  play: nessuna icona per {pkg}")
                continue
            url = re.sub(r"=s\d+.*$", "", m.group(1)) + "=s512"
            with urllib.request.urlopen(url, timeout=15) as r, open(dst, "wb") as f:
                f.write(r.read())
            print(f"  play: {pkg}")
        except Exception as e:  # pacchetto non sul Play Store o rete assente
            print(f"  play: {pkg} non scaricata ({e.__class__.__name__})")


def sources():
    """(componente, file) per ogni icona disponibile."""
    out = {}
    tsv = os.path.join(SOURCE, "components.tsv")
    if os.path.exists(tsv):
        for line in open(tsv, encoding="utf-8"):
            comp = line.split("\t")[0].strip()
            pkg, cls = comp.split("/")
            path = os.path.join(SOURCE, f"{pkg}__{cls}.png")
            if pkg not in SKIP and os.path.exists(path):
                out[pkg] = (comp, path)
    for pkg, cls in POPULAR.items():
        path = os.path.join(SOURCE, f"play__{pkg}.png")
        if pkg not in out and os.path.exists(path):
            out[pkg] = (f"{pkg}/{cls}", path)
    return out


def load(path):
    img = Image.open(path).convert("RGBA").resize((S, S), Image.LANCZOS)
    if img.getpixel((1, 1))[3] > 230:
        # Icone quadrate a tutto campo (Play Store, icone legacy): ritagliate a squircle come quelle di sistema
        mask = Image.new("L", (S * 4, S * 4), 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, S * 4 - 1, S * 4 - 1), radius=int(S * 4 * 0.24), fill=255)
        img.putalpha(Image.fromarray(np.minimum(np.array(img.getchannel("A")),
                                                np.array(mask.resize((S, S), Image.LANCZOS)))))
    return np.asarray(img).astype(np.float32) / 255.0


def lum(rgb):
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]


def sat(rgb):
    return colorsys.rgb_to_hsv(*rgb)[1]


def background(px):
    """Colore di fondo: il più frequente sull'anello esterno della forma opaca (None se non dominante)."""
    alpha = Image.fromarray((px[..., 3] * 255).astype(np.uint8))
    opaque = np.asarray(alpha) > 200
    # Erosione di ~20 px fatta su un'immagine ridotta (MinFilter grande è lento)
    small = alpha.resize((S // 4, S // 4), Image.BILINEAR).filter(ImageFilter.MinFilter(11))
    inner = np.asarray(small.resize((S, S), Image.NEAREST)) > 200
    ring = opaque & ~inner
    if ring.sum() < 50:
        return None
    colors = px[ring][:, :3]
    q = (colors * 15).round().astype(int)
    keys = q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2]
    values, counts = np.unique(keys, return_counts=True)
    best = values[counts.argmax()]
    if counts.max() < 0.5 * len(keys):
        return None
    return colors[keys == best].mean(axis=0)


def extract_mark(px):
    """Alfa del logo (bordi morbidi) e colore con cui disegnarlo, pixel per pixel."""
    alpha = px[..., 3]
    rgb = px[..., :3]
    bg = background(px)
    if bg is None:
        return alpha, rgb, None
    dist = np.sqrt(((rgb - bg) ** 2).sum(axis=-1))
    mark_alpha = np.clip((dist - 0.12) / 0.18, 0, 1) * alpha
    coverage = mark_alpha.sum() / max(alpha.sum(), 1)
    if coverage < 0.02 or coverage > 0.85:
        return alpha, rgb, None  # logo e fondo non separabili: icona intera
    weights = mark_alpha[..., None]
    mean = (rgb * weights).sum(axis=(0, 1)) / max(weights.sum(), 1e-6)
    color = rgb.copy()
    bg_vivid = sat(bg) > 0.35 and 0.12 < lum(bg) < 0.9
    if lum(mean) > 0.8 and sat(mean) < 0.2 and bg_vivid:
        tint = np.array(bg)
        if lum(tint) < 0.35:  # marchio troppo scuro sul riquadro: schiarito
            h, s, v = colorsys.rgb_to_hsv(*tint)
            tint = np.array(colorsys.hsv_to_rgb(h, s, max(v, 0.85)))
        color[:] = tint
    else:
        # Parti scure e poco sature del logo (es. scritte nere) illeggibili sul riquadro scuro: schiarite pixel per pixel
        l = 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]
        s = rgb.max(axis=-1) - rgb.min(axis=-1)
        dark = (l < 0.28) & (s < 0.35)
        color[dark] = np.array(INK) / 255.0
    return mark_alpha, color, bg


_TILE = None


def tile():
    """Riquadro di base (uguale per tutte le icone): calcolato una volta sola."""
    global _TILE
    if _TILE is None:
        _TILE = _draw_tile()
    return _TILE.copy()


def _draw_tile():
    big = S * 4
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    box = (int(big * 0.02), int(big * 0.02), int(big * 0.98), int(big * 0.98))
    d.rounded_rectangle(box, radius=int(big * 0.22), fill=TILE + (255,), outline=LINE + (255,), width=10)
    grid = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid)
    step = big // 18
    for y in range(step // 2, big, step):
        for x in range(step // 2, big, step):
            gd.ellipse((x - 9, y - 9, x + 9, y + 9), fill=GRID + (255,))
    inner = Image.new("L", (big, big), 0)
    pad = 40
    ImageDraw.Draw(inner).rounded_rectangle((box[0] + pad, box[1] + pad, box[2] - pad, box[3] - pad),
                                            radius=int(big * 0.22) - pad, fill=255)
    grid.putalpha(Image.fromarray(np.minimum(np.asarray(grid.getchannel("A")), np.asarray(inner))))
    img = Image.alpha_composite(img, grid)
    return img.resize((S, S), Image.LANCZOS)


def led(img):
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cx, cy, r = S * 0.80, S * 0.20, S * 0.024
    ImageDraw.Draw(glow).ellipse((cx - r * 2.4, cy - r * 2.4, cx + r * 2.4, cy + r * 2.4), fill=LED + (90,))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(S * 0.02)))
    big = Image.new("RGBA", (S * 4, S * 4), (0, 0, 0, 0))
    ImageDraw.Draw(big).ellipse(((cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4), fill=LED + (255,))
    return Image.alpha_composite(img, big.resize((S, S), Image.LANCZOS))


def faithful(path):
    """Icona originale intatta: squircle uniforme, fondo scuro solo se l'icona non riempie la forma,
    hairline e LED arancio. Il nome dell'app lo mostra il launcher."""
    big = S * 4
    shape = Image.new("L", (big, big), 0)
    ImageDraw.Draw(shape).rounded_rectangle((0, 0, big - 1, big - 1), radius=int(big * 0.24), fill=255)
    shape = shape.resize((S, S), Image.LANCZOS)
    src = Image.fromarray((load(path) * 255).astype(np.uint8), "RGBA")
    inside = np.asarray(shape) > 128
    fill = (np.asarray(src.getchannel("A"))[inside] > 200).mean()
    if fill > 0.9:
        out = src  # l'icona riempie già la forma: ritaglio a squircle
    else:
        out = Image.new("RGBA", (S, S), TILE + (255,))
        inner = src.resize((round(S * 0.8), round(S * 0.8)), Image.LANCZOS)
        out.alpha_composite(inner, ((S - inner.width) // 2, (S - inner.height) // 2))
    out.putalpha(Image.fromarray(np.minimum(np.asarray(out.getchannel("A")), np.asarray(shape))))
    # Hairline interna chiara e sottile, a 4x per bordi puliti
    ring = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    ImageDraw.Draw(ring).rounded_rectangle((3, 3, big - 4, big - 4), radius=int(big * 0.24) - 3,
                                           outline=(255, 255, 255, 46), width=8)
    out = Image.alpha_composite(out, ring.resize((S, S), Image.LANCZOS))
    # LED con anello scuro, leggibile su qualsiasi fondo
    cx, cy, r = S * 0.82, S * 0.18, S * 0.026
    led_layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(led_layer)
    d.ellipse(((cx - r * 1.9) * 4, (cy - r * 1.9) * 4, (cx + r * 1.9) * 4, (cy + r * 1.9) * 4), fill=(14, 14, 14, 230))
    d.ellipse(((cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4), fill=LED + (255,))
    return Image.alpha_composite(out, led_layer.resize((S, S), Image.LANCZOS))


def style(path):
    if MODE == "faithful":
        return faithful(path)
    px = load(path)
    mark_alpha, color, bg = extract_mark(px)
    mark = Image.fromarray((np.dstack([color, mark_alpha]) * 255).clip(0, 255).astype(np.uint8), "RGBA")
    box = mark.getchannel("A").point(lambda a: 255 if a > 12 else 0).getbbox()
    if box:
        mark = mark.crop(box)
    # Logo staccato: grande nel riquadro; icona intera (non separabile): un po' più piccola
    limit = S * (0.58 if bg is not None else 0.66)
    scale = limit / max(mark.size)
    mark = mark.resize((max(1, round(mark.width * scale)), max(1, round(mark.height * scale))), Image.LANCZOS)
    out = tile()
    out.alpha_composite(mark, ((S - mark.width) // 2, (S - mark.height) // 2))
    return led(out)


def main():
    os.makedirs(SOURCE, exist_ok=True)
    if "--fetch" in sys.argv:
        fetch_play()
    out_dir = os.path.join(RES, "drawable-nodpi")
    os.makedirs(out_dir, exist_ok=True)
    os.makedirs(os.path.join(RES, "xml"), exist_ok=True)
    for f in os.listdir(out_dir):
        if f.startswith("ic_app_"):
            os.remove(os.path.join(out_dir, f))
    filters, items = [], []
    for pkg, (comp, path) in sorted(sources().items()):
        name = "ic_app_" + re.sub(r"[^a-z0-9]", "_", pkg.lower())
        style(path).save(os.path.join(out_dir, f"{name}.png"), optimize=True)
        filters.append(f'    <item component="ComponentInfo{{{comp}}}" drawable="{name}" />')
        items.append(f'    <item drawable="{name}" />')
    with open(os.path.join(RES, "xml", "appfilter.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + "\n".join(filters) + "\n</resources>\n")
    with open(os.path.join(RES, "xml", "drawable.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <version>2</version>\n'
                + "\n".join(items) + "\n</resources>\n")
    print(f"{len(items)} icone")


if __name__ == "__main__":
    main()

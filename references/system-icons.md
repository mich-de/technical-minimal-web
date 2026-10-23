# System / UI icons (vector)

For the app's OWN icons (settings, status bar, toolbar, inputs). Same meaning as the icon being
replaced — restyle, don't reinvent.

## Spec
- 24x24 viewport, stroke 1.6, round caps and joins, no fill except small solid dots
  (r 1.0–2.2) used as signature details (slider knobs, LED, list bullets, keyholes).
- Pure white (`#FFFFFFFF`); the UI tints them with the theme text colour (`tintIfVector`).
- Rounded rects r 1.5–2.5; geometric, no perspective, no gradients.
- Banners (16:9, 320x180): pictogram as 4px accent stroke + label in 5x7 dot-matrix white.
- Launcher icon: adaptive; background `#0E0E0E`; foreground dot-matrix initial + orange LED.

## Generator
`python3 -I <skill-dir>/scripts/gen_system_icons.py --out <app>/src/main/res/drawable`
Primitives: `dot`, `circle`, `rrect`, `gear` (16-point toothed polygon), `wifi(slashed)`,
`stroke_path`, `fill_path`, `dot_text` (5x7 font). Edit the `ICONS` / `BANNERS` dicts per app.
Back up originals first (e.g. `design-backup/`); raster sources (webp/png) with the same name in
density folders shadow vectors — move them out.

## Shipped set (LeanBox)
grid of 9 dots (all apps), trash, ethernet port, wifi (+ slashed), sliders (Android settings),
monitor + circle (display), monitor + half-filled circle (calibration), remote, image frame
(theme/wallpaper), shield + keyhole, tray + up arrow (backup), info circle, gear, input arrow,
three RCA jacks (AV/audio), eye slashed, accessibility figure, chip (engineering menu), bulleted
list (categories), three dots in pill (more).

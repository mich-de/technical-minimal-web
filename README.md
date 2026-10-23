# technical-minimal-web

Agent skill for the **Technical Minimal** design system (Nothing / Teenage Engineering inspired,
from [mich-de.github.io](https://mich-de.github.io)) — one design language for many surfaces:

| Surface | Reference | Assets |
| --- | --- | --- |
| Websites, portfolios, showcases | `references/web.md` | `scripts/transform.js`, `scripts/dot-matrix.js`, `templates/web/` |
| Android / Android TV UI (Kotlin) | `references/android-ui.md` | `templates/android/` |
| TV launchers, remote / HDMI-CEC UX | `references/tv-launcher.md` | `templates/android/RemoteKeys.kt`, `AllAppsPanel.kt`, `BootSplash.kt` |
| System / UI icons (vector) | `references/system-icons.md` | `scripts/gen_system_icons.py` |
| App icon packs (PNG from real icons) | `references/app-icons.md` | `scripts/style_app_icons.py`, `templates/iconpack/` |
| Accepted / rejected choices | `references/taste.md` | — |

Core: Doto + Geist + Geist Mono, dark / light / OLED tokens, 1px hairlines, radial dot grid,
OKLCH per-section hues, orange LED accent. See `SKILL.md`.

## Install

```bash
git clone https://github.com/mich-de/technical-minimal-web ~/.claude/skills/technical-minimal-web
```

## Scripts

```bash
node scripts/dot-matrix.js "HELLO."                                   # 5x7 dot-matrix SVG
node scripts/transform.js showcase.config.json index.html             # static showcase page
python3 -I scripts/gen_system_icons.py --out app/src/main/res/drawable # vector system icons
python -I scripts/style_app_icons.py --source src --res pack/src/main/res --fetch  # icon pack (Pillow, numpy)
```

Templates under `templates/android` and `templates/iconpack` come from the LeanBox Android TV
launcher: change the `package` line and resource names before use.

## License

MIT

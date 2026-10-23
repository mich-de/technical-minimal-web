# App icon pack (third-party apps)

Rule: icons must stay recognisable as the real app. Start from the REAL icon and process it
pixel by pixel (PNG); never redraw logos as glyphs/letters or low-res LED dots (rejected).

## Pipeline
1. **Sources**
   - Installed apps: `templates/iconpack/ExportActivity.kt` renders each launcher activity icon at
     `DENSITY_XXXHIGH`, 432px, plus `components.tsv`. Needs `<queries><intent><action MAIN/></intent></queries>`.
     `adb shell am start -n <pack>/.ExportActivity` → `adb pull /sdcard/Android/data/<pack>/files/export <src>`.
   - Not installed: Play Store `og:image` of `play.google.com/store/apps/details?id=<pkg>` →
     replace size suffix with `=s512` (`--fetch`; `POPULAR` dict = package → main activity).
2. **Style** — `python -I <skill-dir>/scripts/style_app_icons.py --source <src> --res <pack>/src/main/res [--fetch] [--mode faithful|extract]`
   (Pillow + numpy).
   **`faithful` (default, approved)**: the original icon untouched (same background, colours, logo),
   clipped to a uniform squircle (r 24%); icons that don't fill the shape (round / transparent) sit on
   a dark `#141414` squircle at 80%; light inner hairline (white 18%) + orange LED with a dark ring
   at top-right. The launcher writes the app name under the icon.
   **`extract`** (alternative):
   - full-square sources are clipped to a squircle first;
   - background = most frequent quantised colour on the outer ring of the shape (≥ 50%);
   - logo alpha = smoothstep of colour distance from background (soft edges); if not separable
     (coverage < 2% or > 85%) keep the whole icon smaller;
   - recolour only when needed: white logo on vivid background → brand colour; dark, unsaturated
     pixels → ink `#ECEBE8` (per pixel, so mixed logos keep their colours);
   - compose: dark rounded tile `#141414` + hairline `#333`, faint dot grid, logo ≤ 58%, orange LED
     with soft glow at top-right. Base tile cached (rendering it per icon is very slow).
   - Output `res/drawable-nodpi/ic_app_<pkg>.png`, `res/xml/appfilter.xml`, `res/xml/drawable.xml`.
3. **Pack app** (`templates/iconpack/`): `PickerActivity` with intent filters
   `org.adw.launcher.icons.ACTION_PICK_ICON`, `org.adw.launcher.THEMES`, `com.novalauncher.THEME`,
   MAIN + `com.anddoes.launcher.THEME` / `com.teslacoilsw.launcher.THEME`; returns `icon` bitmap,
   `org.adw.launcher.icons.extra.ICON_BITMAP`, `...ICON_RESOURCE` and `EXTRA_SHORTCUT_ICON_RESOURCE`.
4. **Review**: contact sheet original | styled side by side before installing.

## In the launcher
When a pack icon replaces a TV banner, show icon + app name (name under the icon, one line,
12sp) — an icon alone on a 16:9 card is not enough to recognise the app.

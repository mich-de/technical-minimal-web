# Android / Android TV UI (Kotlin)

Reference implementation: LeanBox launcher (`templates/android/*.kt`, taken verbatim; change the
`package` line and `R` references). Works with plain Views; for Compose map the same tokens to a
`ColorScheme` / `Typography` and keep the drawables.

## Token owner — `ThemeManager.kt`
- `ThemePreset(id, name, light, bg, surface, line, lineStrong, text, muted, accentColor, onAccent, dot)`;
  presets `tm_dark` (default), `tm_light`, `tm_oled`; optional auto day/night via `uiMode`.
- `getFontTypeface(bold)` → Geist (`R.font.geist_regular/semibold`), `getMonoTypeface(medium)`,
  `getDisplayTypeface()` → Doto. Load with `resources.getFont(id)` (API 26+).
- `hue(context, h)` → OKLCH → sRGB conversion in code (dark L=.76 C=.12, light L=.48 C=.14);
  `sectionHue(i, n)`.
- `mix(color, base, pct)` (color-mix), `withAlpha`, `dimColor(percent)` (wallpaper dim uses bg, not black).
- `createPatternBackground()` → `LayerDrawable(bg, dot tile)`; dot tile = 16dp bitmap with one
  1.2dp circle, `TileMode.REPEAT` (one tiny bitmap, cheap on weak GPUs).
- `tintIfVector(drawable, color)` → tint ONLY own white vector icons, never app icons.
- `migrate()` with `theme_version` pref when presets change (old ids → new defaults).

## Components
- **Status bar**: LED dot + `LEANBOX` wordmark in Doto (left); weather chips, 48dp hairline-circle
  buttons (icon 26dp; focused = accent fill + onAccent icon, scale 1.1), Doto clock 34sp with mono
  date under it (right); hairline under the bar.
- **Hero**: mono index label in section hue, Geist 600 title 40sp with clip-bounds wipe
  (ValueAnimator → `clipBounds`, 420ms ease-out-expo), mono uppercase meta, preview card with
  hairline foreground.
- **Rows**: header = hue swatch dot + `01 // TITLE` mono + count; idle muted → focused hue.
- **Cards**: keep the app's real banner/icon; surface + hairline for icon-only cards; icons
  normalised (`IconNormalizer.kt`: equal visible area, Launcher3 principle).
- **Focus**: scale 1.06–1.08 + coloured glow = `outlineAmbientShadowColor` = icon colour (API 28+,
  spot shadow transparent), `translationZ` 16dp with a slow shared pulse animator (cancel on
  pause/refresh), theme `ambientShadowAlpha`/`spotShadowAlpha` = 0.999; focus ring = icon colour
  mixed 55% with white, 2.5dp.
- **Side panel** (`SidePanel.kt`): right 400dp dialog, surface 0xF5 + hairline left edge, mono
  numbered headers in accent, rows with "kinetic" background (`ClipDrawable` level 0→10000, 450ms)
  + 3dp accent bar, LED dot instead of ✓, hairlines between rows, mono values.
- **Boot splash** (`BootSplash.kt`): 5x7 dot-matrix word, staggered dots with deterministic
  jitter, segmented progress bar in section hues, LED blinking → steady green READY, dismiss on
  any key, blur (API 31+) + scale fade-out; only on cold process start.
- **Weather icons** (`WeatherIcon.kt`): drawn in code on a 24 grid, monochrome, sun in accent.
- Dialog/AlertDialog: theme is static → recolor title/buttons after `show()`.

## Gotchas learned
- `KeyEvent.isConfirmKey` is hidden API → own `CONFIRM` set.
- `android.R.id.alertTitle` is internal → `getIdentifier("alertTitle", "id", "android")`.
- `pkill -f name` in a shell whose command line contains `name` kills the shell itself.
- Device without battery reports capacity `Integer.MIN_VALUE` → hide battery when not 1..100.
- Rebuild offline: `./gradlew --offline assembleDebug`; install with `adb install -r` (debug key).

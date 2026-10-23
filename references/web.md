# Web (sites, portfolios, showcases)

Tokens: see SKILL.md core. Fonts via Google Fonts link below.


## When to Apply

Apply this skill whenever:
- The user asks to style, transform, or modernize a website in the style of `mich-de.github.io`.
- Building a technical portfolio, project index, hobby showcase, developer catalog, or directory.
- Transforming a basic HTML page into a sleek industrial/hardware-inspired aesthetic.
- Adding a dot-matrix boot splash screen, theme switch, or hairline grid layout.

---

## Design System Tokens & Specifications

### 1. Typography Stack
Always import Google Fonts `Doto`, `Geist`, and `Geist Mono`:
```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Doto:wght@700;900&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500&display=swap" rel="stylesheet">
```
- **Display / Splash / Hero Header**: `Doto, "Geist Mono", monospace` (weight 700 / 900).
- **Body & Section Titles**: `Geist, system-ui, sans-serif` (weight 400 / 500 / 600).
- **Metadata, Counters, Tags, Slugs, Status Bar**: `"Geist Mono", ui-monospace, monospace` with `font-variant-numeric: tabular-nums`.

### 2. Dual-Theme Palette & Hairline Grids
```css
:root, [data-theme="light"] {
  --bg: #f3f2ef;
  --surface: #fbfaf8;
  --line: #d9d6d0;
  --line-strong: #bdb9b1;
  --text: #141413;
  --muted: #5f5b55;
  --accent: #e5481a;
  --accent-ink: #b3360f;
  --on-accent: #fff;
  --dot: #cfcbc4;
  --band: #ecebe6;
}
[data-theme="dark"] {
  --bg: #0e0e0e;
  --surface: #151515;
  --line: #262626;
  --line-strong: #3a3a3a;
  --text: #ecebe8;
  --muted: #9a968f;
  --accent: #ff5a1f;
  --accent-ink: #ff7a45;
  --on-accent: #0e0e0e;
  --dot: #2b2b2b;
  --band: #131313;
}
```

### 3. Per-Section OKLCH Hue Wheel
Each section receives a distinct hue angle (`--h: 0` to `360`).
In light mode, it is rendered at `oklch(0.48 0.14 var(--h))` to ensure strict 4.5:1 contrast against light backgrounds.
In dark mode, it shifts to `oklch(0.76 0.12 var(--h))`:
```css
[style*="--h"] { --hc: oklch(.48 .14 var(--h)); }
[data-theme="dark"] [style*="--h"] { --hc: oklch(.76 .12 var(--h)); }
```

### 4. Background Radial Dot Pattern
Applied to the hero banner and splash screen:
```css
background-image: radial-gradient(var(--dot) 1.2px, transparent 1.2px);
background-size: 16px 16px;
background-position: -8px -8px;
```

---

## Core Structural Components

### 1. Interactive Dot-Matrix Boot Splash Screen
An animated SVG screen displayed once per session:
- **SVG Pattern Matrix**: 5x7 dot-matrix letters where lit dots (`<circle class="on">`) have a staggered `--d` delay calculated with deterministic spatial jitter.
- **Segmented Hue Progress Bar**: Each segment corresponds to a section's hue (`--h`) and animates in sequence.
- **Blinking LED**: Blinks during boot, turns steady when `Ready`.
- **Session & Accessibility**: Stored in `sessionStorage('splash-seen')`. Automatically bypassed on deep links (`location.hash`), repeated visits, or `prefers-reduced-motion`.
- **Dismissal**: Dismisses on any key press, pointer down, touch, or scroll with a smooth blur/scale fadeout.

Use the bundled generator script:
```bash
node <skill-dir>/scripts/dot-matrix.js "MYPROJECT."
```

### 2. Sticky Translucent Navigation Bar
- `position: sticky; top: 0; backdrop-filter: blur(10px);`
- Left: Pulsating green/orange LED handle with GitHub/identity link.
- Right: Tabular status counter (`X sections · Y items`) and Theme Toggle pill.
- Accessible 44px min-touch hit area via pseudo-element (`.toggle::after { inset: -7px -4px; }`).

### 3. Hero Section with Text Wipe Animation
- Oversized Doto title: `font-size: clamp(48px, 8.2vw, 108px)`.
- Orange accent `.hot` word.
- Dynamic clip-path entrance animation:
```css
@keyframes wipe {
  from { clip-path: inset(0 100% 0 0); filter: blur(6px); }
  to { clip-path: inset(0 0 0 0); filter: blur(0); }
}
```

### 4. Index Quick-Nav Grid
- Grid of section links with hairline borders on right and bottom.
- Dot color swatch indicator (`.sw { background: var(--hc); }`).
- Right-aligned item count in tabular monospace.

### 5. Two-Column Section Layout
- Desktop: 2-column grid (`minmax(0, 340px) minmax(0, 1fr)`).
- Left column: Sticky sidebar (`position: sticky; top: 96px;`) featuring technical index number (`01 // CATEGORY`), SVG category icon, section title, and description.
- Right column: Interactive row list.
- Alternating subtle band contrast (`.section-block:nth-of-type(even)`).

### 6. Kinetic Interactive Rows
Each row features:
- Monospace slug (e.g. repo name or short ID).
- Title and subtitle description.
- Status/Kind badge (`.k.mono`, colored with section hue if actionable/play).
- Diagonal arrow (`#i-arrow`) shifting `translate(3px, -3px)` on hover.
- Animated background hover expansion using CSS pseudo-element:
```css
.row::before {
  content: ""; position: absolute; inset: 0 0 0 24px; z-index: -1;
  background: color-mix(in oklch, var(--hc) 9%, var(--surface));
  transform: scaleX(0); transform-origin: left;
  transition: transform .45s cubic-bezier(.16, 1, .3, 1);
}
.row:hover::before, .row:focus-visible::before { transform: scaleX(1); }
```

---

## Transformation Workflow: From Basic Webpage to Technical Minimal

When given an existing plain HTML webpage or asked to convert a site:

### Step 1: Extract Existing Data
Extract the core entities:
1. **Site identity / User**: Name, handle, tagline.
2. **Sections / Categories**: Topic groupings (e.g. Projects, Tools, Writings, Hobbies, Skills).
3. **Items**: Title, URL, description, category, and badge type (e.g. `REPO`, `DEMO`, `LIVE`, `DOCS`, `PLAY`).

### Step 2: Create Configuration or Use Scaffold
Create a `showcase.config.json` (or pass directly to the transformer):
```json
{
  "title": "My Showcase",
  "entityName": "developer",
  "handleLink": "https://github.com/developer",
  "heroTitle": "Crafted with",
  "heroHotTitle": "discipline.",
  "heroLead": "Engineering projects and tools built for resilience and speed.",
  "splashText": "SHOWCASE.",
  "sections": [
    {
      "id": "apps",
      "title": "Applications",
      "description": "Full-stack and desktop applications.",
      "hue": 35,
      "icon": "#i-cube",
      "items": [
        {
          "slug": "app-alpha",
          "title": "Alpha App",
          "description": "Real-time collaborative whiteboard.",
          "url": "https://example.com/alpha",
          "tag": "LIVE"
        }
      ]
    }
  ]
}
```

### Step 3: Run Transformer
Run the built-in transformer script to produce the standalone `index.html`:
```bash
node <skill-dir>/scripts/transform.js showcase.config.json index.html
```

Or copy and customize the canonical template directly from:
`<skill-dir>/templates/web/template.html`

### Step 4: Verify Checklist
- [ ] Dot-matrix splash screen operates cleanly on cold load and closes on click/key.
- [ ] Theme switcher toggles light/dark mode and updates `<meta name="theme-color">`.
- [ ] Tabular numbers align correctly in monospace fonts.
- [ ] Row hover animations slide from left with cubic-bezier easing.
- [ ] Colors maintain >= 4.5:1 contrast against light and dark backgrounds.
- [ ] Zero external JavaScript frameworks (vanilla static JS only).

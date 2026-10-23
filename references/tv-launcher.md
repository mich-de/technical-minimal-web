# TV launcher & remote-control UX

## Layout (1920x1080 framebuffer, ~320dpi → density 2)
Status bar → hero (focused app, 272dp preview) → vertical list of horizontal rows (focused row
scrolls to top, focused card anchored at the left gutter 48dp) → settings row as pill chips.
Dot grid background; ambient radial glow tinted by the focused app (alpha ~70 dark / 40 light).

## Reaching things with a D-pad only
- Top bar first focusable = most used action (one ▲ from the first row lands on it).
- Every destructive or secondary action must be reachable WITHOUT long-press: colour keys,
  a top-bar icon, or an item in the options panel.
- "All apps" (`AllAppsPanel.kt`): alphabetical with letter headers, ◀ ▶ jump letter, ▲ ▼ scroll,
  OK open, Red/Menu options. Typing on TV is slow → jump, don't search.
- Options panel: "Apri" first, "Disinstalla" second; for system apps open
  `ACTION_APPLICATION_DETAILS_SETTINGS` (Disable / Uninstall updates) — they can't be uninstalled.

## HDMI-CEC (TV remote, e.g. LG Magic Remote) — measured facts
- Arrive: arrows, OK, Back (Exit), 4 colour keys (`KEYCODE_PROG_*`), media keys. TV keeps:
  Home, numbers, Settings, "…", CH±, volume. A launcher can't capture TV Settings/Home.
- Holding OK = burst of separate DOWN/UP presses (repeatCount 0): first repeat after 385–505ms,
  then every 160–311ms. Native long-press never fires; repeats spill into the next dialog
  (danger: uninstall confirmations).
- Events are injected by `HdmiControlService` (deviceId `VIRTUAL_KEYBOARD`) → accessibility key
  filtering does NOT see them; global remapping is impossible without root.
- `RemoteKeys.kt`: `continuesBurst(event)` per key with `BURST_GAP_MS = 650`; on cards a CEC OK
  waits the gap before clicking, a continuing burst opens the options menu; panels click only
  after the burst ends and ignore bursts started elsewhere. Colour keys: count only the first
  press of a burst. Read `dumpsys hdmi_control` (message history) to see what a remote sends;
  add a "key test" panel (log key name, gap ms, source) to calibrate.

## Extras that fit the style
- Weather chips (Open-Meteo, no key; geocoding search for the location; cache 30 min).
- Icon pack support: parse `appfilter.xml` (res/xml or assets), match by component then package;
  manual picks override; ignore a selected pack that is no longer installed.
- Set as default home over adb: `cmd role add-role-holder android.app.role.HOME <pkg> 0`.
- After `adb install -r` of the default launcher Android kills it and the previously used app
  comes back in front; on a CEC-only setup the user is stuck there (no Home key). Always send
  `adb shell input keyevent KEYCODE_HOME` right after reinstalling the launcher.
- Before injecting keys check the foreground app and `dumpsys hdmi_control` history: the owner
  may be pressing the TV remote at the same time and the two inputs interleave.

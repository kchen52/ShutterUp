# ShutterUp — Design Brief

Companion to [SPEC.md](SPEC.md). SPEC decides *what* the app does; this document decides *how it looks, moves, and speaks*. Anything not covered here follows Material 3 defaults.

---

## 1. Personality

**Quiet. Editorial. Warm.**

ShutterUp is a photography journal, not a game. The photos are the colour; the interface is the paper they're printed on. Gamification exists, but it whispers: a streak count in the corner, a badge that arrives like a postcard, never a fanfare.

Reference points for the implementer:

- **VSCO / Glass** — restraint, photos first, generous whitespace.
- **A well-set print magazine** — one serif headline per page, everything else small and quiet.
- **Google Photos "Memories"** — how full-bleed imagery and soft typography sit together.
- **Not:** Duolingo, Strava, or anything with streak fire, confetti storms, or red warning colours.

Three tests for any screen: *Does the photo win? Is there exactly one headline? Would this look fine printed?*

---

## 2. Foundations

### 2.1 Colour

Two layers:

1. **System scheme — Material You dynamic colour.** All surfaces, text, nav, buttons, and states come from the M3 dynamic scheme (`dynamicLightColorScheme` / `dynamicDarkColorScheme`), dark mode following the system. Fallback seed if dynamic colour is unavailable: `#6B5B4E` (warm taupe), which yields a paper-like neutral scheme.
2. **Theme tint — one hue per prompt theme.** Each prompt theme string maps deterministically to a hue (`hue = (stableHash(theme.lowercase()) mod 360)`), then is **harmonized** toward the scheme's `primary` (M3 `harmonize`, 15 %). The tint is used at low alpha only:
   - Today card / theme chips / Themes screen group headers: tint at **12 %** over `surfaceContainerLow` (light) or **18 %** over `surfaceContainer` (dark).
   - A 4 dp accent bar or dot where a chip is too much (calendar legend, feed card header).
   - Never used for text or for anything needing contrast; `onSurface` sits over the tinted surface and remains AA-compliant because the alpha is low.

Result: every theme has a colour identity that recurs across Home, Feed, Themes, and the widget, but the app never looks "coloured".

Semantic rules:

- **No red anywhere** for missed/skipped days. Missing a day is neutral, rendered in `outlineVariant`.
- `primary` is reserved for the single primary action on screen (Shoot) and today's ring in the calendar.
- `tertiary` is used for freeze tokens and the frozen-day snowflake.
- Photos are never tinted, dimmed, or overlaid with gradients except the Completion screen's bottom scrim (see §4.3).

### 2.2 Typography

Two families:

| Role | Face | Notes |
|---|---|---|
| Display / Headline (prompt titles, screen titles, badge names, streak number) | **Fraunces** (variable, Google Fonts, OFL) | Bundled in `res/font` — no downloadable fonts because the app has no network. Use optical size axis: `opsz` 144 for display, 48 for headline. Soft serif, slightly warm. |
| Body / Label / Title-small | **System default** (Roboto / One UI font) | Respects the user's Samsung font choice. |

M3 type scale overrides:

- `displaySmall` 36/40, Fraunces, weight 500, letter-spacing −0.5 — prompt title on Home card and Prompt Detail.
- `headlineMedium` 28/34, Fraunces, weight 500 — screen titles, Completion title.
- `headlineSmall` 24/30, Fraunces, weight 500 — Day screen prompt title, badge names.
- `titleLarge` 20/26, Fraunces, weight 500 — Themes headers, widget title (system serif in Glance).
- Everything smaller: system font, default M3 sizes.
- Prompt titles wrap up to 3 lines at 200 % font scale, then ellipsize. Never shrink text to fit.

Theme labels and dates are `labelMedium`, **uppercase**, letter-spacing +1.0 — the one place with tracking, giving the "magazine kicker" feel: `TUESDAY · REFLECTIONS`.

### 2.3 Shape

- Cards: **28 dp** (`extraLarge`).
- Photos inside cards and grid thumbnails: **16 dp**.
- Photos in full-bleed detail: **0 dp** top corners, screen edge to edge.
- Chips: full pill.
- Buttons: full pill (M3 default). The Shoot button is a `FilledButton` with an aperture icon, 56 dp tall on Home / Prompt Detail (`ExtendedFloatingActionButton` semantics, but placed inline in the card, not floating).
- Badge emblems: circle.

### 2.4 Spacing and grid

- 4 dp base grid.
- Screen margins: **16 dp** compact, **24 dp** expanded.
- Card inner padding: **20 dp**; 24 dp on expanded.
- Vertical rhythm between sections: 24 dp; between related items: 8 dp.
- Lists and grids: 8 dp gutters; calendar cells 4 dp gutters.

### 2.5 Iconography

Material Symbols **Rounded**, weight 400, optical size 24. Filled variant for the selected navigation item only. Custom icons (aperture for Shoot, snowflake for freeze) are drawn as vectors matching that style — 2 dp stroke, rounded caps.

### 2.6 Elevation and surfaces

Flat. Use tonal surface containers, not shadows. Only the Badge unlock sheet and dialogs get M3 elevation level 3. Photos get a 1 dp `outlineVariant` hairline at 30 % alpha when sitting on a same-tone surface, so light photos don't bleed into light cards.

### 2.7 Photos

- Display in **native aspect ratio** everywhere except the Calendar (1:1 centre crop) and Home "Recent" strip (1:1).
- Feed: staggered two-column grid on expanded, single column native aspect on compact.
- Loading: thumbnail as placeholder → crossfade 200 ms to original.
- Missing original: thumbnail with a small `broken_image` glyph chip in the corner, "Original missing".

---

## 3. Navigation shell

- **Compact (cover screen):** bottom `NavigationBar`, four destinations: **Today · Calendar · Feed · Badges**. Settings via top-right icon on Today. Themes is reached from Feed via a filter chip row, not a top-level destination.
- **Expanded (inner screen):** `NavigationRail` on the left with the same four, Settings at the bottom of the rail. Content uses `ListDetailPaneScaffold`.
- Top bars: `LargeTopAppBar` with Fraunces title on Calendar/Feed/Badges/Settings, collapsing to `TopAppBar`. Today has **no** top bar; the card is the header.
- Edge-to-edge; content draws behind the status bar with a `surface` scrim at 60 %.

---

## 4. Screens

Wireframes are low-fidelity: proportions matter, exact pixels don't.

### 4.1 Today (Home)

Compact:

```
┌──────────────────────────────┐
│ ShutterUp              [gear]│  labelLarge, system font, quiet
│                              │
│ ┌──────────────────────────┐ │  theme-tinted card, 28dp radius
│ │ TUESDAY · REFLECTIONS    │ │  kicker, uppercase
│ │                          │ │
│ │ Find the sky             │ │  displaySmall, Fraunces
│ │ in a puddle              │ │
│ │                          │ │
│ │ Turn the world upside    │ │  bodyLarge, onSurfaceVariant
│ │ down using any reflective│ │
│ │ surface you pass today.  │ │
│ │                          │ │
│ │ [ ◎ Shoot ]     Details →│ │  filled pill + text button
│ └──────────────────────────┘ │
│                              │
│  14 days       ✻ 2      ◔ 18/19  streak · freezes · month ring
│                              │
│ RECENT                       │  kicker
│ [■][■][■][■][■]  →           │  1:1 thumbs, 72dp, horizontal
│                              │
├──────────────────────────────┤
│  Today   Calendar  Feed  Badges │
└──────────────────────────────┘
```

Rules:

- The pending / skipped / paused card wraps its content with the standard 20 dp inner padding so unused space sits below the card, not inside it. It never scrolls internally. When today is **COMPLETED**, the photo card still uses a 55 % viewport minimum so the image has room.
- Status row: streak number in `headlineSmall` Fraunces, label in `labelMedium`. Freeze count uses the snowflake glyph in `tertiary`. Month ring is a 28 dp `CircularProgressIndicator` (track `outlineVariant`, progress `primary`) with `completed/eligible` beside it.
- When today is **COMPLETED**: the card becomes the photo — full-bleed inside the card with a bottom scrim, kicker and title over it in `inverseOnSurface`, and the button row becomes `Add a note` / `Retake`.
- When today is **SKIPPED**: the card keeps the kicker, shows the title struck through in `onSurfaceVariant`, and the body reads "Skipped — see you tomorrow." (`MISSED` only ever applies to past days, so it never appears on this card.)
- **PAUSED**: card reads "Paused" with a `Resume` button; kicker shows the resume hint.
- **Library prompt**: a tiny `labelSmall` chip "From the library" in the kicker line, `outline` colour.
- **Series (opt-in):** when today's prompt belongs to a seven-day series, the kicker carries the series title and position instead of weekday · theme: `A WEEK OF HANDS · 3 OF 7`. A quiet seven-dot row sits above the Shoot row: completed days filled with `primary` at low emphasis, the rest `outlineVariant`, the current day a ring rather than a fill. Never red, never animated beyond the card-reveal. Missing a day leaves that dot unfilled. When Series is off, this screen renders exactly as the wireframe above.
- **AI downloading**: a slim `LinearProgressIndicator` under the status row with "Preparing on-device AI · 43 %". Disappears when done.
- **Notification permission denied**: an M3 `Card` (tonal, `errorContainer` is *not* used; use `secondaryContainer`) under the status row: "Turn on notifications to get your daily prompt" with an `Open settings` text button.

Expanded: list-detail. Left pane (40 %) = the compact Today content minus the card's detail text; right pane (60 %) = Prompt Detail (§4.2) for today, always visible.

### 4.2 Prompt Detail

Compact:

```
┌──────────────────────────────┐
│ ←                            │
│                              │
│ TUESDAY 19 SEPTEMBER         │  kicker
│ REFLECTIONS · FROM NANO      │  kicker (source shown only in debug)
│                              │
│ Find the sky                 │  displaySmall
│ in a puddle                  │
│                              │
│ Turn the world upside down   │  bodyLarge
│ using any reflective surface │
│ you pass today.              │
│                              │
│ HOW TO APPROACH IT           │  kicker
│ Look down, not up. Puddles,  │  bodyMedium
│ car roofs, and shop windows  │
│ all hold a second sky...     │
│                              │
│ TIPS                         │
│ · Tap to focus on the        │  bullet list, bodyMedium
│   reflection, not the water. │
│ · Try it after rain.         │
│                              │
│ ┌ CONSTRAINT ──────────────┐ │  outlined card, tinted
│ │ Don't rotate the photo   │ │
│ │ afterwards.              │ │
│ └──────────────────────────┘ │
│                              │
│ 9 hours left today           │  labelMedium, onSurfaceVariant
│                              │
│ [ ◎ Shoot ]          Reroll  │  sticky bottom bar
│                       Skip   │
└──────────────────────────────┘
```

- Bottom bar is a `BottomAppBar`-like surface with Shoot as the filled action and Reroll / Skip as text buttons. Reroll disabled with "Rerolled" label once used.
- When the day belongs to a series, the date kicker stays first and the series line takes the theme's slot — two tight lines, same as today, so the Fraunces title is still the first thing the eye lands on:

```
│ TUESDAY 19 SEPTEMBER              │  date kicker, unchanged
│ A WEEK OF HANDS · 3 OF 7  [tag]   │  series replaces theme; library tag inline
│ ·· ○ ····                         │  seven-dot row, then the title
```

  If the series title already contains the theme (case-insensitive), drop the theme. Otherwise append ` · THEME` only when the line still fits the one-line kicker budget at 200% font scale; if it cannot fit, drop the theme. When Series is off, this screen is unchanged.
- "N hours left" turns to "N minutes left" under an hour; never coloured.
- Deep-linked with `autoLaunchCamera` → the camera launches immediately; this screen is what the user returns to if they cancel.

Expanded: same content in the right pane; bottom bar becomes an inline button row under the constraint card.

### 4.3 Completion

The reward moment. Restraint, but a moment.

```
┌──────────────────────────────┐
│                              │
│                              │
│         [ photo,             │  native aspect, max 60% height,
│           16dp radius ]      │  centred, hairline outline
│                              │
│                              │
│ REFLECTIONS · DAY 15         │  kicker
│ Find the sky in a puddle     │  headlineMedium
│                              │
│ ┌──────────────────────────┐ │
│ │ Add a note…              │ │  outlined text field, 2 lines
│ └──────────────────────────┘ │
│                              │
│ 15 days ✻ 2                  │  streak number rolls 14 → 15
│                              │
│ [ Done ]     Share     Retake   │
└──────────────────────────────┘
```

- On entry: photo scales from 0.92 → 1.0 with a spring (stiffness medium-low, damping 0.8) while fading in over 350 ms; text fades in 150 ms later; streak number rolls up (§6).
- If a badge unlocked: after the streak roll finishes (~800 ms), a `ModalBottomSheet` rises with the emblem (§5), name in `headlineSmall`, one-line description, and a `Nice` dismiss button. One badge per sheet; multiple unlocks queue.
- When the day belongs to a series, the kicker may carry the series name where it currently carries the theme (`A WEEK OF HANDS · DAY 15`). No extra praise copy, no series-complete fanfare.
- Expanded: photo left (55 %), text/note/actions right, vertically centred.
- **Share** is a quiet text button in the action row, offered only when the day has a photo. See §4.11.

### 4.4 Calendar

```
┌──────────────────────────────┐
│ September 2026     ‹  ›      │  headlineMedium Fraunces
│                              │
│  M   T   W   T   F   S   S   │  labelSmall
│  ·   ·   ·   1   2   3   4   │
│ [■] [■] [■] [■]  ○  [■] [■]  │
│  5   6   7   8   9  10  11   │
│ [■]  ⊘  [■] [■] [■] [■] [■]  │
│ 12  13  14  15  16  17  18   │
│ [■] [■] [■] [■] [■] [■] [■]  │
│ 19  20  21  22  23  24  25   │
│ (◎) 20  21  22  23  24  25   │
│                              │
│ ◔ 18 of 19 days this month   │  labelMedium + ring
│ Longest streak 22            │
└──────────────────────────────┘
```

Day cell (44 dp square, 12 dp radius), states:

| Status | Rendering |
|---|---|
| Completed | 1:1 thumbnail, 12 dp radius, hairline outline |
| Completed, no photo | `secondaryContainer` fill, check glyph in `onSecondaryContainer` |
| Skipped | `outlineVariant` 1.5 dp ring, small horizontal dash inside |
| Missed | 6 dp dot in `outlineVariant`, centred, day number in `outline` |
| Frozen (skipped/missed + freeze) | as above plus a 12 dp snowflake badge top-right in `tertiary` |
| Paused | day number only, `outline` colour, 60 % alpha |
| Today, pending | 2 dp `primary` ring, day number in `primary` |
| Future | day number in `onSurfaceVariant` |

- Swipe horizontally between months; `‹ ›` also work. Month title crossfades.
- Tap a completed cell → shared-element transition of the thumbnail into the Day screen (§6).
- Tap a non-completed past cell → Day screen showing the prompt with its status in the kicker: `SKIPPED · REFLECTIONS`.
- Expanded: calendar left (45 %), Day screen right for the selected date; today selected by default.

### 4.5 Day

Compact: photo full-bleed at top (edge-to-edge, 0 dp top radius, native aspect capped at 70 % height, `pinch to zoom` opens a full-screen viewer), then the same content stack as Prompt Detail (kicker with date + status, title, one-liner, details, tips, constraint), then **Note** (editable inline, autosave), then a quiet action row: `Delete`, `Share` when a photo exists, and `Retake` if today. Badges earned that day appear as small emblems (32 dp) under the kicker.

Expanded: right pane, photo top with the text below; the pane scrolls.

### 4.6 Feed

Compact: single column of cards, each a photo in native aspect (16 dp radius) with a kicker (`12 SEP · REFLECTIONS`) and the title in `titleLarge` beneath. Infinite scroll, newest first. A filter chip row at top: `All` + one chip per theme (theme-tinted when selected). Non-completed days do not appear.

Expanded: two-column staggered grid, same cards.

### 4.7 Badges

Grid of emblems (3 columns compact, 5 expanded), 96 dp each, name below in `titleSmall` system font so the grid stays quiet and the emblem is the hero. Locked badges: outline emblem at 40 % alpha with the symbol blurred (4 dp blur) and the name replaced by a short hint ("Complete 30 days in a row"). Tap → dialog with large emblem (160 dp), name in `headlineSmall` Fraunces, description, unlock date.

Sections in order: **Streaks · Count · Explorer · Time of day · Special**. Top of screen: current streak and longest streak in a two-cell stat row with Fraunces numbers.

### 4.8 Settings

Standard M3 list, grouped:

- **Daily prompt** — Notification time (time picker), Precise timing (switch + explanatory supporting text + status line "Allowed" / "Needs permission → Open settings"), Pause (switch).
- **Prompts** — Series (switch + supporting text "Some weeks arrive as a set of seven related prompts instead of seven separate ones."), Theme focus (text field, supporting text "Leave blank to be surprised"), On-device AI status (read-only row: "Ready · Gemini Nano v2" / "Downloading 43 %" / "Unavailable — using the built-in library").
- **Photos** — Save location (read-only "Pictures/ShutterUp"), Storage used.
- **About** — version, "Everything stays on your phone. ShutterUp has no internet access.", licences.
- **Debug** (debug builds only) — Use fake AI (switch), Force day rollover, Seed 60 days of history, Reset all data.

### 4.9 Onboarding

Four full-screen pages, single column, max width 480 dp, swipe or `Next`:

1. Wordmark + one sentence. Background: a very soft theme-tinted gradient that slowly shifts hue (20 s loop, respects reduced motion).
2. Notifications: illustration is a mock notification card (built in Compose, not an image), request button.
3. Time picker inline + theme focus field.
4. AI status with progress; `Get my first prompt` button.

Illustrations throughout the app are **composed from UI elements and shapes**, never raster art, so they follow dynamic colour and dark mode.

### 4.10 Empty and edge states

| Situation | Treatment |
|---|---|
| Calendar/Feed with no history | Centre-aligned Fraunces headline "Your first photo goes here." + bodyMedium "Come back after today's prompt." No illustration needed. |
| Badges, none unlocked | Grid of locked badges — the grid *is* the empty state. |
| Original missing | See §2.7. |
| Camera failed twice | Inline card under the Shoot button: "Camera didn't return a photo. You can pick one you took today instead." + `Choose from Gallery`. |
| Photo Picker date rejected | Snackbar: "That one's from another day — only today's photos count." |
| Storage error on save | Dialog with `Try again`; never silently mark completed. |
| Share card failed | Snackbar: "The card didn't come together. Try again." |

### 4.11 Share card

A composed still that leaves the app through the system share sheet. Chrome, not a call to action: a `Share` text button in the Day and Completion action rows, only when the day has a photo. The photo still wins; this is the paper around it.

Rendered with the real design-system components (theme tint, `Kicker`, Fraunces headline) to an offscreen PNG, 1080 px wide. Light or dark matches the user's current appearance. The note is not on the card.

```
┌──────────────────────────┐
│                          │
│      [ the photograph,   │  native aspect
│        16 dp radius ]    │
│                          │
│ 19 SEPTEMBER · REFLECTIONS│ kicker, uppercase, +1.0 tracking
│ Find the sky in a puddle │  Fraunces, the one headline
│                          │
│              ShutterUp   │  small, quiet wordmark
└──────────────────────────┘
```

Rules:

- Card surface uses the prompt's theme tint (same 12 % / 18 % overlay as Today / chips).
- Photo hairline: 1 dp `outlineVariant` at 30 % alpha (see §2.6).
- Title wraps up to 3 lines, then ellipsizes. Never shrink to fit.
- Wordmark is Fraunces at `labelMedium`, `onSurfaceVariant`, end-aligned. Quiet enough that it reads as a signature, not a second headline.
- No series name, no streak, no badge. Date, theme, title, photo, wordmark.
- Font scale is locked at 1.0 on the exported bitmap so a 200 % system font does not blow up a card headed for a messaging thread.

---

## 5. Badge emblem system

Enamel-pin look: flat, two-tone, geometric, no gradients, no drop shadows. All drawn with Compose `Canvas`/vector paths so they scale and theme.

Anatomy (at 96 dp):

- Outer ring: 3 dp, family colour.
- Inner disc: family colour at 20 % over `surfaceContainerHigh`.
- Symbol: family colour, 2.5 dp stroke or solid geometric fill, fits a 52 dp box.
- Tier marker (for numbered badges): 1–4 small dots on the ring's lower arc.

Family colours are the M3 scheme roles so they adapt to dynamic colour and dark mode:

| Family | Role | Symbol |
|---|---|---|
| Streaks | `primary` | Stacked chevrons rising (3 for week, 4 month, 5 century, 6 year) |
| Count | `secondary` | Aperture iris, blades = tier + 4 |
| Explorer | `tertiary` | Compass rose, 8 points; 16 for tier 2 |
| Time of day | `primary` (Early Bird) / `tertiary` (Night Owl) | Half-sun on a horizon line / crescent with one star |
| Special | `secondary` | First Light: single ray burst · Perfect Month: filled circle in a ring · Comeback: arrow looping back · Cool Under Pressure: snowflake · Curator: two overlapping speech-note rectangles |

Locked: ring only in `outlineVariant`, symbol at 40 % alpha, blurred 4 dp.

Unlock animation: emblem scales 0.6 → 1.0 with a spring (overshoot allowed), followed by a single 600 ms diagonal shimmer (linear gradient sweep of `onSurface` at 12 %) across the disc. No particles.

---

## 6. Motion

M3 motion tokens (`emphasized` easing family). Five named transitions; anything else is default.

| Name | Where | Spec |
|---|---|---|
| **Card reveal** | Today card on app launch / day change | Fade 0→1 + translateY 24 dp→0, 400 ms, emphasized decelerate. Status row follows 80 ms later. |
| **Photo arrive** | Completion screen entry | Scale 0.92→1.0 spring (stiffness 200, damping ratio 0.8) + fade 350 ms. |
| **Shared photo** | Calendar cell → Day, Feed card → Day, Recent strip → Day | `SharedTransitionLayout` bounds transform, 400 ms emphasized. The kicker and title fade in after the bounds settle. |
| **Number roll** | Streak count on Completion and Today | `AnimatedContent` vertical slide + fade, 300 ms, one digit group. |
| **Badge unlock** | Completion bottom sheet | Sheet: standard. Emblem: spring scale 0.6→1.0, then shimmer 600 ms. |

Rules:

- Respect the system animator duration scale; when it is 0 (or the accessibility "remove animations" is on), all of the above become instant crossfades.
- No motion on scroll (no parallax headers).
- Month swipe in Calendar uses `HorizontalPager` default physics.
- List-detail pane changes on the Fold use the `material3-adaptive` defaults.

---

## 7. Copy voice

Second person, present tense, short. Warm but not chirpy. Never exclamation marks in system text (badge descriptions may earn one). No emoji. Never "Oops". Never guilt ("You missed…" is fine as fact; "Don't break your streak!" is not).

| Context | Copy |
|---|---|
| Notification title | *(prompt title)* e.g. "Find the sky in a puddle" |
| Notification text | *(one-liner)* e.g. "Turn the world upside down using any reflective surface you pass today." |
| Notification actions | `Shoot` · `Reroll` |
| Shoot button | `Shoot` |
| Reroll, available | `Reroll` — supporting text on Detail: "One reroll a day." |
| Reroll, used | `Rerolled` (disabled) |
| Skip dialog | Title "Skip today?" Body "Today will count as skipped. Your 14-day streak ends unless a freeze covers it." Buttons `Skip` / `Keep going` |
| Skip dialog with freeze | Body "Today will count as skipped. One of your 2 freezes will keep your streak." |
| Today card, skipped | "Skipped — see you tomorrow." |
| Day screen, missed | kicker `MISSED · REFLECTIONS`; no body copy beyond the prompt |
| Freeze consumed (Today status row tooltip / Day screen line) | "A freeze kept your streak on 17 September." |
| Freeze earned (Completion) | "You earned a freeze. It'll cover one missed day." |
| Streak label | `14 days` (never "14-day streak!" in UI) |
| Completion, first ever | headline "First light." body "Day one. Everything else is repetition." |
| Completion, normal | headline = prompt title; no extra praise |
| Perfect Month badge description | "Every eligible day of a month. No gaps." |
| Comeback badge description | "Back after three or more quiet days." |
| Pause switch supporting text | "No prompts or notifications. Paused days don't affect your streak." |
| Precise timing supporting text | "Delivers at the exact minute. Android needs you to allow alarms and reminders for ShutterUp." |
| Theme focus placeholder | "Leave blank and I'll surprise you" |
| Library tag | "From the library" |
| Series switch | `Series` |
| Series supporting | "Some weeks arrive as a set of seven related prompts instead of seven separate ones." |
| Series kicker | `{title} · {n} OF 7` e.g. `A WEEK OF HANDS · 3 OF 7` |
| AI unavailable | "On-device AI isn't available on this phone right now. ShutterUp is using its built-in prompt library." |
| About line | "Everything stays on your phone. ShutterUp has no internet access." |
| Delete dialog | Title "Delete this photo?" Body "The day stays complete." Buttons `Delete from ShutterUp` / `Also delete from Gallery` / `Cancel` |
| Delete photo (Gallery) result | Snackbar "Deleted." |
| Empty calendar | "Your first photo goes here." |
| Share button | `Share` |
| Share failed | Snackbar "The card didn't come together. Try again." |

---

## 8. Widget

Glance `AppWidget`, two sizes. Uses dynamic colour via `GlanceTheme`; title uses the system **serif** family (Glance/RemoteViews can't load bundled fonts), which keeps the editorial feel.

- **4×2** — theme-tinted background (same 12 % rule over `widgetBackground`), kicker `TUESDAY · REFLECTIONS`, title (serif, 20 sp, 2 lines), one-liner (14 sp, 2 lines), `Shoot` pill button bottom-right. Tap anywhere → Prompt Detail; button → `autoLaunchCamera`.
- **2×2** — kicker + title only (serif, 18 sp, 3 lines). Tap → Prompt Detail.
- Completed state: replace text with the thumbnail (rounded 16 dp) and a small check chip.
- Paused: "Paused" in the title slot.

---

## 9. App icon

Adaptive icon, vector foreground + solid background, plus a **monochrome** layer for Android themed icons.

Concept: **an aperture opening onto a sun.** Six iris blades in a ring; the central opening is a circle sitting slightly high-left of true centre (as if the sun is rising through the shutter).

- Background: `#F3EDE4` (warm paper).
- Foreground: blades in `#2B2622` (warm ink), 6 blades, blade edges straight, blade tips rounded 4 %, iris ring occupies 62 % of the safe zone; opening diameter 30 % of the ring, offset 6 % up and 6 % left; the opening is filled `#E0A458` (amber).
- Monochrome layer: the blades only, opening left transparent.
- Dark-mode note: no separate dark variant needed; the launcher applies themed icons if the user enables them.

In-app wordmark: "ShutterUp" in Fraunces 500, used on Onboarding page 1, the share card (at `labelMedium`, `onSurfaceVariant`), and Today's top-left label (at `labelLarge` size, system font there — the wordmark face is reserved for onboarding and the share card).

---

## 10. Fold-specific design notes

- Inner screen is nearly square (~2184×1968). List-detail split **40/60** for Today, **45/55** for Calendar, so photos in the right pane stay large.
- Never place the primary action across the hinge line. On the inner display the hinge falls near the horizontal centre; the Shoot button lives in the right pane's bottom-right, not centred on the screen.
- Cover screen is tall and narrow (~21:9): the pending Today card wraps its content. The completed photo card still uses the 55 % height rule so the image has room; on the inner screen that photo card is capped at 420 dp tall.
- Fold/unfold mid-Completion: the photo keeps its position on screen (state is preserved; no re-animation).
- Tabletop posture (v1.1): photo on the top half, text and actions on the bottom half, 16 dp gap either side of the hinge.

---

## 11. Accessibility

- All text meets WCAG AA against its surface, including over theme tints (guaranteed by the low-alpha rule) and over photo scrims (scrim ≥ 60 % `scrim` colour).
- Touch targets ≥ 48 dp; calendar cells are 44 dp visual with 48 dp touch.
- Every calendar cell has a content description: "19 September, completed" / "17 September, missed, streak frozen".
- Emblems have content descriptions with badge name and locked/unlocked state.
- Full support for font scaling to 200 %; layouts reflow, nothing truncates except 3-line titles.
- Motion honours the system animation scale (§6).
- Colour is never the only carrier of status (shape + glyph + description always accompany it).

---

## 12. Implementation notes

- Theme object: `ShutterUpTheme` wrapping `MaterialTheme` with the type scale above and a `LocalThemeTint` composition local providing `Color` for the current theme string via `themeTint(theme: String, scheme: ColorScheme): Color`. Unit-test that the hue is stable across runs and that harmonization stays within 15° of the raw hue.
- Fraunces: bundle `fraunces_variable.ttf` (OFL licence file alongside). Use `FontVariation.Settings` for `opsz` and `wght`.
- Badge emblems: one `BadgeEmblem(id, unlocked, size)` composable with a `when` over the badge family; symbols as `Path`s. Add a Compose Preview grid of all badges in both states and both colour modes.
- Provide `@Preview`s for every screen in: compact light, compact dark, expanded light, 200 % font scale. Previews are the design review surface for this project.
- Debug menu "Seed 60 days of history" must generate a mix of all statuses and a few frozen days so every calendar state is visible.

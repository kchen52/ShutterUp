# ShutterUp — Product & Technical Specification

**Status:** v1 spec, ready for implementation hand-off
**Target device:** Samsung Galaxy Z Fold 7 (Android 16 / One UI 8)

**Distribution:** Sideloaded APK for now; must stay Play-Store-ready

---

## 1. Overview

ShutterUp is a single-user, fully offline Android app that sends the user one photography prompt per day, chosen from a bundled curated library (~2000 prompts). Tapping the notification shows the prompt in detail and hands off to the system camera. The resulting photo is stored with its prompt so the user can browse their history later. Light gamification (streaks, badges) keeps the daily habit going.

On-device Gemini Nano is **not loaded at runtime** in v1. Loading it on the Fold 7 was making the rest of the device unusable (system-wide RAM pressure / LMK). Dynamic user theme focus is deferred until a later revisit.

### 1.1 Goals

- One interesting, *doable* prompt every day, drawn from a bundled library with an auto-assigned theme.
- Zero-friction path from notification → prompt → camera → saved photo.
- A pleasant, foldable-aware history browser (calendar first).
- Works with no network. No accounts, no cloud, no telemetry.
- Gamification that rewards consistency without nagging.

### 1.2 Non-goals (explicitly out of scope for v1)

- In-app social features: sharing feeds, comments, likes, followers, or any surface that publishes work inside ShutterUp (revisit later).
- Cloud sync, accounts, multi-device.
- Cloud AI fallback of any kind.
- Runtime Gemini Nano / ML Kit GenAI Prompt API generation (deferred; Nano code remains in the tree unused).
- User theme focus / free-text steering of daily prompts (deferred).
- In-app camera (CameraX viewfinder).
- Video.
- Late entries: a photo can only be attached to *today's* prompt.
- Multiple photos per day: exactly one photo answers one prompt.
- Localization beyond English.

A **system share sheet** (`ACTION_SEND` of a composed card) is in v1. That is a hand-off to another app the user already has, with no server, no account, and no feed in this app. It is not a social feature in the sense of this non-goal. Revert §4.6 and the Share buttons on Day / Completion if that reading is wrong.

---

## 2. Platform constraints and key decisions

| Topic | Decision | Rationale |
|---|---|---|
| Device / OS | Z Fold 7, Android 16 (API 36), One UI 8 | Only target device for now |
| `minSdk` / `targetSdk` / `compileSdk` | 34 / 36 / 36 | Fold 7 is the only target; 34 leaves room to open up later without carrying legacy permission paths |
| On-device AI | **Not used at runtime.** Bundled library is the primary generator. `NanoPromptGenerator` is retained but unwired. | Gemini Nano via AICore was loading a multi-gigabyte model into system RAM and LMK-killing other apps on the Fold 7. Dynamic themes revisit this later. |
| AI unavailable | N/A — the library is always available | Fully offline guarantee, no dead days |
| Network | **No `INTERNET` permission** | Strong, checkable privacy guarantee. Rules out crash reporting/analytics; acceptable |
| Scheduling | WorkManager (inexact) by default; optional "Precise timing" toggle that requests `SCHEDULE_EXACT_ALARM` | Samsung is aggressive with background limits; exact alarms need a manual Settings grant. Never declare `USE_EXACT_ALARM` (Play-restricted to alarm/calendar apps) |
| Camera | `ACTION_IMAGE_CAPTURE` via `ActivityResultContracts.TakePicture` into a `FileProvider` URI; Photo Picker as fallback | Keeps Samsung Camera features (Pro mode, Flex mode, cover-screen preview) with a direct return to the app |
| Photo storage | `MediaStore` → `Pictures/ShutterUp/`; DB stores the content URI + a private thumbnail | Visible in Samsung Gallery / Google Photos backup, survives uninstall |
| Photo size | Keep the original as delivered; generate ~400 px thumbnail | Storage is the user's call; grid stays fast |
| Location | No location permission; optional city from a bundled list for daylight and hemisphere. Camera-written EXIF left untouched on the original, and shared cards are a new PNG that carries none of it | Privacy, offline. Nothing is sent anywhere |
| Backup | Settings → Photos writes a ZIP of progress + photo copies; Android Auto Backup still covers DB + prefs; Gallery originals usually survive uninstall | Auto Backup's 25 MB quota cannot hold originals; the ZIP is on-demand for reinstalls |
| Deletion | Ask each time whether to also delete from Gallery | Default agreed |
| Day boundary | Local calendar date, midnight in the device's current timezone | Simple mental model |

---

## 3. Glossary

- **Prompt** — a generated daily photography challenge (title, one-liner, details, optional constraint, theme).
- **Theme** — a short category label for the prompt (e.g. "Reflections", "Negative space", "Kitchen still life"). Assigned from the library entry.
- **Series** — an optional week of seven related prompts sharing a title and theme (e.g. "A Week of Hands"). Opt-in; the daily loop is unchanged.
- **Theme focus** — deferred. The preference slot may still exist unused. v1 does not offer free-text steering.
- **Day** — a local calendar date. Each day has exactly one prompt.
- **Entry** — the user's single photo answering a day's prompt, plus optional note.
- **Day status** — `PENDING` (not yet acted on, still today), `COMPLETED`, `SKIPPED`, `MISSED`, `PAUSED`.
- **Streak** — consecutive completed days; `PAUSED` days are neutral; `SKIPPED`/`MISSED` break it unless a freeze is consumed.
- **Freeze** — an earned token that automatically protects the streak on one missed day.
- **Second take** — shooting a past day's prompt again on a later date. The later day stores `repeatsDate` pointing at the original first take.

---

## 4. User flows

### 4.1 First run / onboarding

1. Welcome screen: what the app does (wordmark + one sentence).
2. Notification permission (`POST_NOTIFICATIONS`) — explain, then request.
3. Pick notification time (default 09:00). `Get my first prompt` on this page.
4. Camera permission is **not** requested here (not needed for `ACTION_IMAGE_CAPTURE`).
5. Pick today's prompt immediately from the **library**, top up the buffer (see §7.6), schedule the daily notification, land on Home.

### 4.2 Daily loop (happy path)

1. At the user's chosen time, the notification fires with the prompt title and one-liner. Actions: **Shoot** (opens Prompt Detail with `autoLaunchCamera=true`), **Reroll** (only if a reroll remains).
2. User taps the notification → Prompt Detail screen: title, theme chip, one-liner, details, constraint, tips, remaining time (or remaining daylight if a city is set; see §4.2.1), **Shoot** button.
3. User taps **Shoot** → system camera opens with `EXTRA_OUTPUT` pointing at a pending private file.
4. User takes a photo and confirms in the camera → returns to the app.
5. App moves the file into `MediaStore` (`Pictures/ShutterUp/yyyy-MM-dd_<theme-slug>.jpg`), creates a thumbnail, creates the Entry, marks the day `COMPLETED`, evaluates achievements, and shows a Completion screen (photo, prompt, optional note field, any newly unlocked badge, streak count).
6. Notification is dismissed automatically.

### 4.2.1 Remaining light on Prompt Detail

Prompt Detail shows one remaining-time line, never coloured, Prompt Detail only (not Home, the widget, or the notification).

- Default (no city chosen): `"N hours left today"`, switching to minutes under an hour. Unchanged from v1.
- With a city chosen, and while the sun is up: `"N hours of good light left"` (minutes when under an hour). Sunrise/sunset are computed on-device from the date and the city's latitude/longitude (NOAA solar calculator). The device `ZoneId` is used, not a city timezone, so travelling still reads as local clock time.
- After sunset, before sunrise, and during polar night: fall back to the clock line. Do not comment on the light being gone.
- Polar day (sun does not set): `"Good light all day"`.
- City list is bundled. No `ACCESS_COARSE_LOCATION`, no `INTERNET`, no geocoding.

The city is chosen once in Settings → Prompts → "Where you are", and can be returned to unset. Changing city only regenerates the un-shown future prompt buffer when the hemisphere changes (today's prompt is kept).

### 4.3 Alternate paths

- **Camera cancelled** → back to Prompt Detail, nothing saved, pending temp file deleted.
- **Camera unavailable / intent fails twice** → offer "Choose from Gallery" via the Photo Picker. The picked photo must have `DATE_TAKEN` (or EXIF `DateTimeOriginal`) on today's local date; otherwise reject with "Only photos taken today count".
- **Retake** — before midnight, the Completion/Day screen offers **Retake**; the new photo replaces the old (confirm; old file deleted from `MediaStore` only if the app created it).
- **Share** — when the day has a photo, Completion and Day offer a quiet **Share** text button. It composes a card (photo, date, theme, title — never the note) and hands it to the system share sheet. See §4.6.
- **Reroll** — once per day, from Prompt Detail or notification. Picks a fresh **library** prompt (respecting dedup). The rerolled-away prompt is kept in the DB with `supersededBy` for history/dedup, not shown in the calendar. Rerolling a day that holds a second take also clears `repeatsDate`, so the day becomes an ordinary prompt.
- **Second take** — on the Day screen of a past day that has a photograph, **Shoot this again** copies that prompt onto today if today is still `PENDING`, otherwise onto tomorrow. A confirmation dialog names the landing day. See §4.7.
- **Skip** — explicit "Skip today". Day becomes `SKIPPED`. Breaks streak unless a freeze is consumed. Confirmation dialog explains this.
- **Missed** — at local midnight, any `PENDING` day becomes `MISSED` (freeze consumed if available). No evening reminder in v1.
- **Opened app before notification time** → today's prompt is already visible on Home (prompts are valid from 00:00). Notification still fires at the chosen time unless the day is already `COMPLETED`/`SKIPPED`.
- **Pause** — Settings toggle. While paused: no notifications, no prompts generated, days recorded as `PAUSED` (neutral for streaks). Unpausing picks today's prompt immediately.

### 4.4 Browsing history

- Home shows today's card + streak/freeze summary + mini month strip.
- Calendar (month grid) with day cells showing thumbnail (completed), dot colour for skipped/missed/paused. Pinch-out (or a quiet **Year** control in the top bar) zooms to a year-in-hues grid — one small cell per day, filled with that day's theme tint. Tap a cell → Day screen (photo, prompt, note, theme, timestamps, achievements earned that day, Delete, Share when a photo exists, and **Shoot this again** when the day is a past photograph). Pinch-in or **Month** returns to the month grid. Not a fifth bottom-bar destination.
- Feed (chronological cards) and Themes (grouped by theme with counts) as secondary tabs.
- Badges screen (see §6).

### 4.5 Deleting an entry

Dialog: "Delete this photo from ShutterUp only" / "Also delete from Gallery" / Cancel. The day becomes `COMPLETED_NO_PHOTO` (still counts as completed for streaks; calendar shows a checkmark with no thumbnail). Share is no longer offered. Deleting either photograph in a second-take chain does not break the other day's screen: the remaining take still loads, and a missing frame shows "Original missing".

### 4.6 Sharing a card

The app has no internet permission and no in-app social surface. Sharing is a **system share sheet** (`ACTION_SEND` + `FileProvider`): the user picks a destination on the device. ShutterUp never uploads, never opens a socket, and never learns who received the card.

**When:** a quiet `Share` text button on the Day screen and the Completion screen, in the existing action row next to `Delete` / `Shoot this again` / `Retake` / `Done`. Only when the day actually has a still photo (`COMPLETED` with an `Entry` of `MediaKind.PHOTO`). Not offered for `COMPLETED_NO_PHOTO`, video, or days without an entry.

**What leaves:** a freshly rendered PNG card, not the original photograph:

- The photo at native aspect, 16 dp corners.
- Kicker `19 SEPTEMBER · REFLECTIONS` (date + theme, uppercase, +1.0 tracking).
- The prompt title in Fraunces (the one headline).
- A small `ShutterUp` wordmark.

The **note is never included**. Theme tint and light/dark follow the user's current appearance, using the same `themeTint` as the rest of the app.

**How:** Compose renders the real design-system card to an offscreen bitmap at 1080 px wide (360 dp at 3×). PNG is written to `cache/share/` (a dedicated FileProvider cache path, not the pending-capture directory), old share files are deleted, and the URI is handed to `Intent.createChooser` with `FLAG_GRANT_READ_URI_PERMISSION` (and `ClipData`, so the grant actually travels). Rendering/encoding run off the main thread except for the brief ComposeView measure/draw, which must be on main. Failures snackbar `The card didn't come together. Try again.` and leave no half-written file.

Because the shared bytes are a new composite, none of the original photo's EXIF travels with it — no camera make, no `DateTimeOriginal`, no GPS if the camera wrote it.

This is deliberately one screen's worth of chrome. If §1.2 is read as forbidding even a system share sheet, revert this section and the two Share buttons.

### 4.7 Second take

Photography improves by returning to a subject. On any **past** day that has a photograph, the Day action row offers a quiet **Shoot this again**.

1. The app copies that day's prompt (title, one-liner, details, tips, constraint, theme, source) onto a later date, recorded as a repeat of the **original** date (`repeatsDate`). Repeating a take that is already a repeat still points at that original, so the chain stays linear.
2. **Landing day.** Same predictability Series uses: today if today is still `PENDING` (or has no prompt yet), otherwise tomorrow. A confirmation dialog states this plainly: "This becomes today's prompt." or "This becomes tomorrow's prompt." The app never silently overwrites a day the user has already acted on (`COMPLETED`, `SKIPPED`, `MISSED`, `PAUSED`, `COMPLETED_NO_PHOTO`). If the chosen target is already acted on, the action does nothing.
3. **Series.** If the target day belongs to an active Series, the repeat takes that day out of the series (clears `seriesId` and `seriesIndex`) while the series otherwise continues, leaving that week's dot unfilled.
4. **The diptych.** On a second-take day's screen, the current photograph sits with the take before it: compact stacks them, expanded places them side by side, both 1:1 or both native, matched. Kickers under the pair: the earlier date (`19 SEPTEMBER`) and the interval (`83 DAYS ON`, `A MONTH ON`, `A YEAR ON`). One shared prompt headline below. Tapping either photograph opens the existing full-screen viewer.
5. **More than two takes.** A prompt can be repeated any number of times. The screen always shows the viewed take against the one before it, with quiet text links to the rest of the chain. The original day's screen keeps a single photograph and a quiet line pointing forward (`Shot again on 11 December.`).
6. **Reroll.** Rerolling a day that holds a second take clears `repeatsDate` and generates a normal prompt. The one-reroll-a-day rule is unchanged.
7. **Gamification.** A second take counts toward streaks, themes and badges exactly like any other completed day. No new badge.

---

## 5. Screen inventory

| Screen | Cover screen (compact width) | Inner screen (expanded width) |
|---|---|---|
| Onboarding | Single-column pager | Same, centred at max 600 dp |
| Home | Today card, streak row, month strip, bottom nav | List-detail: left = Home content, right = Prompt Detail of selected day |
| Prompt Detail | Full screen; Shoot as extended FAB | Right pane of list-detail |
| Completion | Full screen photo + fields | Two-pane: photo left, prompt/note/badge right |
| Calendar | Month grid, swipe months. Pinch-out (or Year in the top bar) zooms to the year-in-hues grid; swipe years the same way months swipe. | Grid left, Day detail right. Year view uses the same split. |
| Year (Calendar zoom) | 12 month bands, one tinted cell per day; no photos or numbers in cells. Not a bottom-bar destination. | Same, in the list pane |
| Day | Photo, prompt, note, actions | Right pane |
| Feed | Cards | Two-column staggered grid |
| The Monthly (issue) | Composed page: kicker, headline, contact sheet, body | Same, centred at max 600 dp |
| Past issues | Quiet list of previous months | Same |
| Themes | List with counts → filtered feed | List left, filtered grid right |
| Badges | Grid of badges (locked/unlocked) | Wider grid |
| Settings | Standard list | Two-pane with category list |

Navigation: bottom bar on compact, navigation rail on expanded (`NavigationSuiteScaffold`). Four destinations: Today · Calendar · Feed · Badges. The year-in-hues view is a zoom of Calendar (pinch or top-bar **Year**), not a fifth destination. All screens must survive fold/unfold (configuration change) without losing state, including an in-flight camera launch.

---

## 6. Gamification

Keep it warm and low-pressure. No leaderboards, no social.

### 6.1 Streaks

- **Current streak**: consecutive days with status `COMPLETED` (or `COMPLETED_NO_PHOTO`), walking backwards from today (today counts if completed; if today is still `PENDING`, start from yesterday). `PAUSED` days are skipped over, not counted.
- **Longest streak** stored and displayed.
- **Freeze tokens**: earn 1 for every 7 consecutive completed days; hold max 2. A `MISSED` or `SKIPPED` day auto-consumes a freeze (day status stays `MISSED`/`SKIPPED` but is marked `frozen=true` and does not break the streak). Show freezes as snowflakes next to the streak count.

### 6.2 Badges (achievements)

Evaluated after every day-status change. All deterministic from DB state so they can be recomputed.

| ID | Name | Condition |
|---|---|---|
| `first_light` | First Light | First completed day |
| `streak_7` / `_30` / `_100` / `_365` | Week / Month / Century / Year of Light | Streak reaches N |
| `total_10` / `_50` / `_100` / `_250` / `_500` | Shutter Count N | Total completed days |
| `explorer_10` / `_25` | Theme Explorer | N distinct themes completed |
| `early_bird` | Early Bird | 5 entries captured within 60 min of the notification time |
| `night_owl` | Night Owl | 5 entries captured after 21:00 local |
| `perfect_month` | Perfect Month | Every non-paused day of a calendar month completed |
| `comeback` | Comeback | Complete a day after 3+ consecutive missed/skipped days |
| `iceberg` | Cool Under Pressure | First time a freeze saves a streak |
| `curator` | Curator | 25 entries with a note |

Newly unlocked badges are shown on the Completion screen (one at a time, dismissible) and on the Badges screen with unlock dates.

### 6.3 Monthly ring

Home shows a small progress ring for the current month: completed / (days elapsed − paused).

---

## 7. Prompt generation

### 7.1 Interface

All generation goes through one interface so the implementation can be swapped (bundled library, deterministic fake for tests, and — later — Nano):

```kotlin
interface PromptGenerator {
    suspend fun availability(): Availability  // AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE
    suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt>
}

data class GenerationRequest(
    val date: LocalDate,
    val themeFocus: String?,          // user-provided, optional
    val recentTitles: List<String>,   // last 30 shown prompt titles (dedup)
    val recentThemes: List<String>,   // last 14 themes (variety)
    val dayOfWeek: DayOfWeek,
    val season: Season,               // derived from date; southern hemisphere when a city south of the equator is set, otherwise northern
    val excludeConstraintKinds: Set<String> = emptySet(),
)

data class GeneratedPrompt(
    val title: String,        // ≤ 40 chars, fits a notification title
    val oneLiner: String,     // ≤ 100 chars, the challenge in one sentence
    val details: String,      // 2–4 sentences: what to look for, how to approach it
    val tips: List<String>,   // 1–3 short technique tips
    val constraint: String?,  // optional creative constraint, e.g. "No zoom", "Shoot from knee height"
    val theme: String,        // ≤ 24 chars label
    val source: PromptSource, // ON_DEVICE_AI or LIBRARY
)
```

Composite implementation: `LibraryPromptGenerator` is the runtime primary. A `FakePromptGenerator` (deterministic, seedable) lives in the debug source set and is selectable in Debug settings. `NanoPromptGenerator` is **not wired**; it must never call `Generation.getClient()`, `warmup()`, or `generateContent()` in v1.

### 7.2 Nano usage (deferred)

Do not instantiate `NanoPromptGenerator` at runtime. The class and ML Kit dependency stay in the tree for a later theme-focus revisit. Never show AI download, model name, or "Preparing on-device AI" copy.

### 7.3 Content contract

Library entries (and any future generator) must:

- Produce a **photography challenge doable with a phone in ≤ 30 minutes**, by an ordinary person, today.
- Be **interesting**: favour perspective, light, composition, storytelling, texture, colour, motion, everyday objects seen freshly. Vary between abstract, observational, and playful.
- Be **doable anywhere**: at least half of prompts must work indoors; never require specific weather, locations, animals, events, props the user is unlikely to own, or other people.
- Respect **ethics/safety**: no photographing strangers' faces, children, private property interiors, or anything dangerous (roads, heights, water).
- Carry a **theme** label. Theme focus is deferred and unused.
- **Avoid** anything semantically close to `recentTitles`, and prefer a theme not in `recentThemes`.
- Optionally add one **constraint** that makes it a challenge rather than a description.
- Use a friendly, second-person, non-preachy tone. No emojis. No hashtags.

### 7.4 Validation (post-filter)

`PromptValidator` rejects and triggers regeneration when:

- JSON fails schema or any length limit in §7.1.
- Title is a case/punctuation-insensitive match of any of the last 90 titles, or theme + first 4 words of the one-liner match a recent prompt.
- Text contains any term from a small blocklist (weapons, nudity, trespass, "stranger", "child/children" as subject, "drive/driving while", "roof", "cliff", etc.). Blocklist is a resource file so it can be tuned without code changes.
- Details mention gear beyond a phone/tripod (e.g. "DSLR", "50mm", "drone", "ND filter").
- Contains a URL, hashtag, or emoji.

### 7.5 Theme focus

Deferred. Settings and onboarding do not show a theme-focus field. The preference APIs may remain unused. Changing an unused focus still discards un-shown future buffer prompts if that path is called. Revisit with dynamic generation later.

### 7.6 When generation runs

Picking a library prompt is cheap (no model load). Strategy: keep a **buffer of prompts for today + next 2 days**.

1. **Foreground top-up** — whenever the app is opened, if the buffer has < 3 future days, fill until full. A slim in-app status bar shows `Generating prompts for Tuesday` (or `Generating prompts for a seven-day series`) if the write is still in flight; it never blocks the UI.
2. **Periodic top-up** — a WorkManager job (once per 24 h) tops up the buffer. Timeout 3 minutes total; whatever it finishes is kept.
3. **Notification worker** — when firing today's notification, if today has no prompt, take it from the buffer; if the buffer is empty, pick from the library immediately.
4. On **reroll**, a fresh library prompt is picked in the foreground.

All generated prompts are persisted immediately with `date` assignments, so a prompt is never generated twice for the same date.

### 7.7 Library (primary bank)

- `assets/prompt_library.json`: **~2000** prompts with the same schema as `GeneratedPrompt` plus `id` and `tags`.
- Selection: exclude any library prompt used in the last 180 days; otherwise uniformly random. Theme-focus tag matching stays in the picker for a later revisit but is unused in v1 UI.
- Entries authored to meet §7.3 rules; spread across ≥ 40 themes (each with ≥ 7 unused entries so Series can run); ≥ 50 % indoor-friendly.
- Because every prompt is from the library, do **not** show a "From the library" chip.

### 7.8 Series (opt-in weekly arc)

Settings → Prompts → **Series** (switch, default off). Supporting text when off: "Some weeks arrive as a set of seven related prompts instead of seven separate ones. Turning this on starts a series tomorrow. Today's prompt stays." When on: "Some weeks arrive as a set of seven related prompts instead of seven separate ones. The current series finishes even if you turn this off."

When the setting is on, generation produces a seven-day themed run rather than independent daily prompts. The daily loop is unchanged: still one prompt a day, still one photo a day, still the same Shoot button. A series only changes how the next seven days' prompts are chosen and adds a quiet progress affordance (kicker + seven-dot row). This is not a new mode with new screens.

A series has a **title** (e.g. "A Week of Hands") and seven prompts belonging to it, pinned to seven consecutive dates. Each prompt must still satisfy §7.3 and pass `PromptValidator`. They must vary within the series, not restate each other. At least four of the seven must work indoors.

- **When a series starts.** Enabling the setting does not change today's prompt. Independent (non-series) buffer prompts after today are replaced, and the next series begins tomorrow. Each subsequent series begins the day after the previous one ends. Turning the setting off lets the current series finish, then returns to single daily prompts — already-generated prompts are never deleted.
- **Library path.** Pick a theme with at least seven unused prompts (180-day exclusion), take seven of them, and derive the series title from the theme (`A Week of {theme}`).
- **Reroll.** The one-reroll-a-day rule is unchanged. A reroll inside a series stays within the series' theme. The rerolled-away prompt goes to `superseded_prompts`.
- **Missing a day does not end the series.** That day's dot is left unfilled. No penalty, no warning copy, no "series broken" state.
- **No series badge.** Completing a series is its own reward.

### 7.9 The Monthly (issue generation)

On the first of each month the app quietly assembles the month just finished into a single composed page: a contact sheet of that month's photographs, the month's two loudest themes, and two or three sentences that describe the month back to the user.

- **When it arrives.** An issue is due for every **finished** calendar month (device timezone) that has at least one completed day. The current month is never due, so a mid-month install produces nothing until the next 1st. A device that was off across the boundary catches up every missing finished month. Zero completed days produce no issue.
- **Where it lives.** A card for the newest undismissed issue appears at the top of Feed. Dismissing the card does not delete the issue. Past issues are reachable from a quiet "Past issues" entry on Feed. There is no fifth bottom-bar destination.
- **No notification.** The app never announces the issue.
- **Copy.** Compose the two fields deterministically. Headline comes from a hand-written bank of two phrases per library theme (alternating by month); unknown themes use the theme as a bare phrase. Body is the shape of the month only — day count, notes, longest run, a quiet comparison with the month before if it is not a drop. Themes live in the kicker, never in the body. `MonthlyIssueValidator` still applies to any future generator path.
- **Idempotency.** Each `yearMonth` is written once. Regenerating must not duplicate the row. Generation runs in WorkManager (`MonthlyIssueWorker`), not on the UI thread when Feed opens.

---

## 8. Notifications and scheduling

### 8.1 Channel

- Channel `daily_prompt`, importance **DEFAULT**, name "Daily prompt". Sound/vibration per system default. Not bypass-DND.
- One notification per day, `notificationId = date.toEpochDay().toInt()`. Auto-cancel on tap; cancelled programmatically when the day becomes `COMPLETED`/`SKIPPED`.
- Content: title = prompt title; text = one-liner; big text = details (first 2 sentences); large icon = theme-coloured monogram (no photo). Actions: **Shoot**, **Reroll** (hidden if used).
- Tap → `MainActivity` with deep link `shutterup://day/<date>`; the **Shoot** action adds `autoLaunchCamera=true`. No broadcast trampolines (Android 12+ restriction).

### 8.2 Scheduler

- `DailyNotificationScheduler` computes the next trigger: today at `notifyTime` if still in the future and today is not completed/skipped, else tomorrow at `notifyTime`. Pure function over `(now, notifyTime, todayStatus, zone)` — unit-tested.
- **Default (inexact):** unique `OneTimeWorkRequest` (`ExistingWorkPolicy.REPLACE`) with `setInitialDelay` to the trigger; the worker posts the notification, rolls over statuses (§8.3), then enqueues the next day's request (self-rescheduling). WorkManager persists across reboots; no `BOOT_COMPLETED` receiver needed for this path.
- **Precise timing (opt-in):** Settings toggle. If enabled and `alarmManager.canScheduleExactAlarms()` is false, open `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with an explanation. Use `setExactAndAllowWhileIdle` with a `BroadcastReceiver` that posts the notification and reschedules. Handle `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` and `BOOT_COMPLETED` (receiver only registered when this mode is on). If permission is revoked, fall back to WorkManager and show a Settings banner.
- Re-schedule on: notify-time change, timezone change (`ACTION_TIMEZONE_CHANGED`), app update (`MY_PACKAGE_REPLACED`), pause/unpause.
- Show a Settings hint, with a deep link to the app's battery settings page, if the app is in a restricted standby bucket or battery-restricted (`UsageStatsManager.getAppStandbyBucket()`, `ActivityManager.isBackgroundRestricted()`). Never auto-prompt for battery-optimisation exemption.

### 8.3 Day rollover

Status rollover (`PENDING` → `MISSED`, freeze consumption, achievement evaluation, `PAUSED` recording) is idempotent and runs: on the notification worker, on app open, and via a `DayRolloverUseCase` unit-tested against a fake `Clock`. It processes every date between the last processed date and today, so gaps (device off for a week) are handled correctly.

---

## 9. Capture and storage pipeline

1. Prompt Detail **Shoot** → create `cache/pending/<uuid>.jpg`, get a `FileProvider` URI, persist the URI + date in `SavedStateHandle` (survives fold/unfold and process death), launch `ActivityResultContracts.TakePicture()`.
2. On `true` result: verify file is non-empty and decodable. Read EXIF orientation; do **not** re-encode the original.
3. Insert into `MediaStore.Images` with `RELATIVE_PATH = Pictures/ShutterUp`, `DISPLAY_NAME = yyyy-MM-dd_<theme-slug>.jpg`, `IS_PENDING = 1`; stream-copy; set `IS_PENDING = 0`. Store the resulting content URI. Delete the temp file.
4. Generate a thumbnail (longest side 400 px, respecting EXIF orientation) into `files/thumbs/<date>.jpg`.
5. Create `Entry`, set day `COMPLETED`, evaluate achievements, cancel notification, navigate to Completion.
6. On `false` result: delete temp file, stay on Prompt Detail.
7. **Photo Picker fallback** (`PickVisualMedia`, images only): after picking, read `DATE_TAKEN` from `MediaStore` (fallback EXIF via `ExifInterface` on the opened stream); require today's local date. Copy into `Pictures/ShutterUp` as above (originals are never moved). Mark `Entry.importedFromGallery = true`.
8. **Displaying** originals: load via Coil from the content URI with the thumbnail as placeholder. If the URI no longer resolves (user deleted in Gallery), show the thumbnail with a "Original missing" tag and offer Delete.

Scoped storage: writing to `MediaStore` collections needs no permission on API 29+; reading the app's own inserted URIs needs no permission; the Photo Picker needs no permission. **Do not** request `READ_MEDIA_IMAGES`.

One-time migration: installs that still point `Entry.mediaUri` at app-private files (`FileProvider` / `file://`) are walked on a background coroutine at process start. Each readable original is stream-copied into `Pictures/ShutterUp` with the same `DISPLAY_NAME` rule and the row is rewritten to the new `content://` URI; thumbnails are left alone. The pass is idempotent (already-migrated MediaStore URIs are skipped) and interruption-safe (a crash mid-loop resumes remaining rows on the next launch). A file that cannot be copied keeps working off its thumbnail. Settings → Photos reports `Pictures/ShutterUp` and counts Gallery originals plus private thumbnails.

**Progress backup (Settings → Photos):** **Backup progress** writes a ZIP the user keeps (`progress.json` plus `thumbs/` and `photos/` copies). **Restore progress** confirms, then replaces Room + prefs, remaps MediaStore URIs (keep if still readable, else match `DISPLAY_NAME` in `Pictures/ShutterUp`, else re-insert from the ZIP), and rewrites thumbnail paths into this install. Photos already in Gallery are not overwritten. The ZIP is a local file via the system document picker; nothing is uploaded.

---

## 10. Data model

Room database `shutterup.db` (schema exported, migrations tested). Preferences in `DataStore<Preferences>`.

```
DayPrompt
  date            LocalDate  PK
  title, oneLiner, details, constraint?, theme
  tips            List<String> (JSON)
  source          ON_DEVICE_AI | LIBRARY
  libraryId       String?
  modelName       String?          -- from getBaseModelName()
  generatedAt     Instant
  status          PENDING | COMPLETED | COMPLETED_NO_PHOTO | SKIPPED | MISSED | PAUSED
  frozen          Boolean          -- a freeze protected this day
  rerollUsed      Boolean
  seriesId        Long?            -- FK to Series when this day belongs to a week
  seriesIndex     Int?             -- 1–7 within that series
  repeatsDate     LocalDate?       -- original first take this prompt repeats; null if original
  supersededBy    LocalDate?       -- for rerolled-away prompts (stored in SupersededPrompt, see below)

Series                         -- opt-in seven-day themed run (SPEC §7.8)
  id PK, title, startDate, endDate, theme, source

MonthlyIssue                   -- one composed page per finished month (SPEC §7.9)
  id PK, yearMonth (unique, yyyy-MM), startDate, endDate, completedDayCount
  headline, body
  dominantTheme, loudestThemes
  source          ON_DEVICE_AI | LIBRARY
  generatedAt     Instant
  dismissedFromFeed Boolean
  modelName       String?

SupersededPrompt              -- rerolled-away prompts, for dedup only
  id PK, date, title, theme, generatedAt

Entry
  date            LocalDate  PK, FK → DayPrompt
  mediaUri        String           -- content:// URI
  thumbPath       String
  capturedAt      Instant          -- from EXIF DateTimeOriginal, else now
  width, height   Int
  note            String?
  importedFromGallery Boolean
  createdAt       Instant

Achievement
  id              String PK        -- from §6.2 table
  unlockedAt      Instant
  unlockedOnDate  LocalDate

StreakState (single row)
  current, longest, freezes       Int
  lastProcessedDate               LocalDate

LibraryUsage
  libraryId PK, usedOnDate

Preferences (DataStore)
  notifyTime (LocalTime), preciseTiming (Boolean), themeFocus (String?),
  seriesEnabled (Boolean, default false), paused (Boolean), onboardingComplete (Boolean),
  debugUseFakeAi (Boolean, debug only),
  coarseCityId (String?, default unset) -- id from the bundled city list; lat/lon are not stored
```

---

## 11. Permissions matrix

| Permission | When | Notes |
|---|---|---|
| `POST_NOTIFICATIONS` | Onboarding | Required for core value; show a persistent Home banner if denied |
| `SCHEDULE_EXACT_ALARM` | Only when "Precise timing" enabled | User grants in Settings; app must work without it |
| `RECEIVE_BOOT_COMPLETED` | Manifest | Only used by the exact-alarm path |
| `WAKE_LOCK` | Manifest (WorkManager) | Implicit via library |
| `CAMERA` | **Not requested** | `ACTION_IMAGE_CAPTURE` doesn't need it; declaring it is unnecessary. Do not declare `<uses-feature android:name="android.hardware.camera" android:required="true">` |
| `INTERNET` | **Never** | Hard rule; add a unit test that asserts the merged manifest lacks it |
| Storage permissions | **None** | MediaStore write + Photo Picker |
| Location permissions | **Never** | Daylight uses a bundled city list |
| `FOREGROUND_SERVICE` | Not in v1 | Generation runs inside WorkManager limits |

---

## 12. Architecture and stack

- **Language/UI:** Kotlin 2.x, Jetpack Compose (Material 3), `material3-adaptive` (`ListDetailPaneScaffold`, `NavigationSuiteScaffold`), `WindowSizeClass`.
- **Architecture:** single-module app is acceptable for v1, organised by layer: `ui/` (screens + ViewModels), `domain/` (use cases, pure Kotlin), `data/` (Room, DataStore, MediaStore, generators), `work/` (WorkManager/AlarmManager), `di/`. Keep `domain/` free of Android imports so it is JVM-unit-testable.
- **DI:** Hilt (with `HiltWorkerFactory`).
- **Async:** Coroutines + Flow. Inject `Clock` and `ZoneId` providers everywhere time matters.
- **Persistence:** Room (KSP), DataStore Preferences.
- **Background:** WorkManager 2.10+, AlarmManager (opt-in path).
- **Prompt library:** bundled `prompt_library.json` via `LibraryPromptGenerator`. ML Kit GenAI stays on the classpath unused.
- **Images:** Coil 3, `androidx.exifinterface`.
- **Widget:** Glance (`AppWidget`) — today's title + one-liner, tap opens Prompt Detail. Updated on prompt change / status change.
- **Navigation:** Navigation Compose with type-safe routes; deep link `shutterup://day/{date}`.
- **Theming:** Material You dynamic colour, dark mode follows system, edge-to-edge. Visual design, typography, motion, copy voice, badge emblems, widget and icon are specified in [DESIGN.md](DESIGN.md), which takes precedence over M3 defaults for everything it covers.
- **Build:** Gradle Kotlin DSL, version catalog, `debug` and `release` build types; release signed with a local keystore (gitignored — `.gitignore` already covers `*.jks`/`*.keystore`).
- **Quality:** `ktlint` or `detekt`, Android Lint fatal on `release`, strict `explicitApi` not required.
- **CI:** GitHub Actions running `./gradlew lint testDebugUnitTest assembleDebug` on PRs, plus a manually triggered signed-APK workflow (§16.1). Instrumented tests run on the plugged-in Fold 7 (`connectedDebugAndroidTest`), not in CI.

---

## 13. Foldable-specific requirements

- No fixed orientation; `resizeableActivity=true`; support multi-window and DeX resize.
- Layouts keyed on `WindowSizeClass`: compact width (cover screen, 6.5" 21:9) = single pane; expanded width (inner 8" near-square) = list-detail.
- Fold/unfold during any screen (including while the camera intent is in front) must not lose state: all ViewModels use `SavedStateHandle` for in-flight identifiers (pending capture URI, selected date, note draft).
- Continuity: after returning from the camera on a different screen than it was launched from, land on the same Completion screen.
- Camera preview is the system camera's responsibility (Samsung handles cover/inner preview and Flex mode).
- v1.1 nice-to-have: tabletop posture (`FoldingFeature.State.HALF_OPENED`, horizontal hinge) → Day/Completion screen puts the photo on the top half, text/controls on the bottom half.
- Test both screens for every screen in §5, plus rotate on the inner display.

---

## 14. Edge cases

| Case | Behaviour |
|---|---|
| Device off across midnight(s) | Rollover processes each missing date; missed days consume freezes in order |
| Timezone change | Day boundaries follow the current zone; reschedule; never regenerate an existing date's prompt |
| Notification time changed to earlier than now | Next trigger is tomorrow unless today is `PENDING` and time already passed → fire within 1 minute |
| Theme focus changed (unused in v1) | Discard un-shown future buffer prompts and regenerate; today's prompt keeps unless rerolled |
| Reroll after buffer top-up | Only today's prompt changes |
| Low storage on capture | Catch `IOException`, keep temp file, show retry; don't mark completed |
| Photo taken but app killed before save | On next launch, check `cache/pending/` for today's file and complete the save |
| User deletes original in Gallery | Thumbnail remains; Day screen shows "Original missing" |
| Photo Picker returns a non-today photo | Reject with clear message; day remains `PENDING` |
| App uninstalled/reinstalled | Photos remain in Gallery; restore from Settings → Photos backup ZIP if the user made one; otherwise DB restored from Auto Backup if available; thumbnails regenerated from URIs or the ZIP |
| Two devices (future) | Out of scope; data model uses dates as keys so merges are conceivable |

---

## 15. Testing

### 15.1 Unit tests (JVM, `src/test`)

Pure-Kotlin domain must reach high coverage. Required suites:

- `PromptParserTest` — valid JSON, missing fields, over-length fields, extra fields, non-JSON garbage.
- `PromptValidatorTest` — blocklist hits, gear mentions, dedup by title/theme, URL/emoji rejection, accepts a corpus of good prompts.
- `GeneratePromptUseCaseTest` — retries on validation failure, falls back to library after 3 attempts, never generates twice for one date, buffer top-up to 3 days, series generation of seven days, series does not replace today's prompt, enabling series starts tomorrow and replaces independent buffer, in-series reroll stays on theme, library series fallback, reroll of a second take clears `repeatsDate`.
- `PromptLibraryBankTest` — ~2000 unique ids/titles, ≥ 40 themes with ≥ 7 each, ≥ 50 % indoor, every entry passes `PromptValidator`.
- `SeriesCalendarTest` / `SeriesProgressCalculatorTest` — next series start, enabling does not disrupt today, dots for completed / current / missed.
- `SecondTakeCalendarTest` / `TakeChainTest` / `TakeIntervalTest` / `StartSecondTakeUseCaseTest` — landing today when pending vs tomorrow when completed/skipped/paused; chain of two and of five; interval phrasing across days / a month / a year; series detachment of only the target day; reroll clears the repeat link.
- `GenerateMonthlyIssueUseCaseTest` — finished-month windows across timezones and year boundaries, mid-month install, zero completed days, fallback copy, idempotent regeneration, catch-up of skipped empty months.
- `MonthlyIssueValidatorTest` / `MonthlyIssueFallbackTest` / `MonthlyIssueCalendarTest` / `ThemeRankingTest` — reject cases, templates across sparse/full/single-theme/note-heavy months, ties, month windows.
- `MonthlyIssueWorkerTest` — Robolectric: success writes once; failure retries.
- `LibraryPromptGeneratorTest` — excludes prompts used in last 180 days, prefers matching tags, works when all are exhausted, library series from a theme with ≥ 7 unused prompts.
- `DailyNotificationSchedulerTest` — next-trigger computation across before/after notify time, completed today, paused, DST transitions, timezone change.
- `DayRolloverUseCaseTest` — multi-day gaps, freeze consumption order, paused days neutral, idempotency.
- `StreakCalculatorTest` — current/longest, paused skipping, frozen days, retro-consistency.
- `AchievementEvaluatorTest` — each badge in §6.2 unlocks exactly once at the right moment.
- `CaptureDateValidatorTest` — today vs. yesterday vs. timezone edge (photo at 23:50 vs 00:10).
- `MediaNamingTest` — filename slug generation.
- `ManifestGuardTest` — parses the merged debug manifest and asserts no `INTERNET`, `READ_MEDIA_IMAGES`, `USE_EXACT_ALARM`, or location permissions.
- `SunTimesTest` — NOAA sunrise/sunset against published times across both hemispheres, an equinox, both solstices, the equator, and Tromsø polar day/night.
- `DaylightRemainingTest` — copy at the hour/minute threshold, fallback when unset / after sunset / polar night, polar-day copy.
- `SeasonTest` — northern default when unset; southern latitude flips the season.
- `ShareCardContentTest` / `ShareableDayTest` — kicker composition, shareable-day rule (photo required; note never a field).
- `ShareCacheTest` / `ShareIntentsTest` — PNG write, FileProvider URI, grant flags, cache prune, failure leaves no leftover file.
- `ProgressBackupJsonTest` / `ProgressBackupStoreTest` — ZIP round-trip of prompts, notes, streaks, badges, prefs, thumbs, and Gallery originals; restore re-inserts a deleted MediaStore row; zip-slip and unknown format rejected.

Use `kotlinx-coroutines-test`, `Turbine` for flows, a fake `Clock`, and `FakePromptGenerator`. Room DAOs: Robolectric-backed tests for queries used by streaks/calendar.

### 15.2 UI tests (Compose instrumentation, `src/androidTest`, run on the Fold 7)

Use `createAndroidComposeRule<MainActivity>()` with Hilt test modules binding `FakePromptGenerator`, in-memory Room, and a controllable `Clock`.

- `OnboardingFlowTest` — completes onboarding, lands on Home with a prompt visible.
- `HomeTodayPromptTest` — today's prompt title/one-liner shown; streak row shown.
- `PromptDetailShootTest` — tapping Shoot fires `ACTION_IMAGE_CAPTURE` with `EXTRA_OUTPUT` (Espresso Intents `intending(...).respondWith(...)`), and a stubbed success result leads to Completion with `COMPLETED` status.
- `RerollTest` — reroll changes the prompt once; button disabled after.
- `SkipTest` — skip dialog explains streak impact; status becomes `SKIPPED`.
- `CalendarTest` — seeded month renders statuses/thumbnails; tapping a day opens Day screen.
- `SettingsTest` — changing notify time persists and reschedules (verify via WorkManager `TestDriver`).
- `AdaptiveLayoutTest` — using `DeviceConfigurationOverride(ForcedSize(...))`, compact shows single pane, expanded shows list-detail.
- `DeepLinkTest` — `shutterup://day/<today>` opens Prompt Detail; with `autoLaunchCamera` extra the capture intent is fired.
- `WidgetTest` (optional) — Glance preview renders today's title.

### 15.3 Manual device checklist (Fold 7)

- Notification arrives at chosen time on both WorkManager and Precise paths, with the app force-stopped in between.
- Daily library pick succeeds; inspect prompt quality for 10 rerolls.
- Camera round-trip on cover screen, on inner screen, and unfolding mid-capture.
- Photo appears in Samsung Gallery under Pictures/ShutterUp; delete-from-Gallery path works.
- Settings → Photos **Backup progress** writes a ZIP; uninstall; reinstall; **Restore progress** brings back prompts, notes, streaks, and photos.
- Midnight rollover with the device idle overnight.

---

## 16. Distribution

**Now (sideload):** `assembleRelease` signed with the project's release keystore; install via `adb install`. Keep `applicationId` stable (`app.shutterup`) from day one — changing it later breaks Auto Backup restore and forces an uninstall.

### 16.1 On-demand APK builds via GitHub Actions

Requirement: the owner can trigger an APK build from GitHub whenever they want and download it to sideload.

**Workflow `.github/workflows/build-apk.yml`**

- Triggers: `workflow_dispatch` (manual "Run workflow" button) with inputs `build_type` (`release` default, or `debug`) and `create_release` (boolean, default false). Optionally also on `push` of a tag matching `v*`.
- Steps: checkout → `actions/setup-java` (Temurin 17) → `gradle/actions/setup-gradle` (caching) → decode keystore from secrets → `./gradlew assembleRelease` (or `assembleDebug`) → `actions/upload-artifact` of `app/build/outputs/apk/**/*.apk` named `shutterup-<build_type>-<versionName>-<short-sha>` (retention 30 days) → if `create_release`, `softprops/action-gh-release` attaching the APK to a GitHub Release tagged `v<versionName>-<run_number>`.
- Also build an AAB in the release path (`bundleRelease`) and upload it as a second artifact for future Play use.
- Concurrency group per ref so re-runs cancel earlier builds.

**Signing**

Sideloaded updates must be signed with the *same* key every time or Android refuses to install over the existing app. Therefore the release keystore lives in GitHub Actions secrets, never in the repo:

| Secret | Contents |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 shutterup-release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | key alias (e.g. `shutterup`) |
| `ANDROID_KEY_PASSWORD` | key password |

`app/build.gradle.kts` reads these from environment variables (`SHUTTERUP_KEYSTORE_PATH`, `SHUTTERUP_KEYSTORE_PASSWORD`, `SHUTTERUP_KEY_ALIAS`, `SHUTTERUP_KEY_PASSWORD`) and falls back to the debug signing config when they are absent, so local builds and CI on forks still succeed. The workflow writes the decoded keystore to `${RUNNER_TEMP}/release.jks` and exports the variables.

**One-time owner setup** (cannot be done by an agent):

1. Generate the keystore locally: `keytool -genkeypair -v -keystore shutterup-release.jks -alias shutterup -keyalg RSA -keysize 2048 -validity 10000`.
2. Back it up somewhere safe (losing it means users must uninstall to update).
3. Add the four secrets above under the repo's Settings → Secrets and variables → Actions.

Until the secrets exist, the workflow's release path will produce a debug-signed APK and print a warning step; the `debug` build type always works.

**Versioning:** `versionName` from `gradle.properties` (semver, bumped manually), `versionCode = GITHUB_RUN_NUMBER` when building in CI (falls back to 1 locally) so every CI APK is installable over the previous one.

**Regular CI** (`.github/workflows/ci.yml`): on PRs and pushes to `main`, run `./gradlew lint testDebugUnitTest assembleDebug` and upload the debug APK as an artifact (7-day retention) so every PR has an installable build.

**Play-Store readiness (do these from the start so opening up later is cheap):**

- No Play-restricted permissions (`USE_EXACT_ALARM`, `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`). `SCHEDULE_EXACT_ALARM` is allowed but must be justified as user-facing reminders with a settings opt-in — already the design.
- Target latest API each year; build an AAB (`bundleRelease`) as well as the APK.
- Data Safety: "No data collected/shared" — true because there's no network.
- Privacy policy stub (`PRIVACY.md`) stating everything stays on device.
- If broadening to non-Samsung devices, re-test `ACTION_IMAGE_CAPTURE` behaviour per OEM (known to vary) and keep the Photo Picker fallback.

---

## 17. Roadmap

**v1 (this spec):** onboarding, daily library prompts + buffer, optional Series weeks, WorkManager + optional exact notifications, capture pipeline, MediaStore save, calendar/feed/themes/day/badges/settings, streaks + freezes + badges + monthly ring, Glance widget, adaptive layouts, on-demand progress backup ZIP, unit + UI tests, CI.

**v1.1:** tabletop posture layout; evening "still time" reminder toggle.

**Later:** dynamic themes / on-device Nano (only if it can run without LMK); one-off "theme for tomorrow"; image-input prompts; in-app social features (feeds, comments, likes), multi-device sync, Play release, other devices, Wear OS glance. System share of a composed card is already in v1 (§4.6).

---

## 18. Hand-off notes for implementing agents

### 18.1 What an agent can and cannot verify

An implementing agent working in a cloud environment can build the app, run all JVM unit tests, run Android Lint, and produce debug/release APKs (it needs JDK 17 and the Android SDK command-line tools; if the environment lacks them, install them, or configure the Cloud Agent environment to include them). It **cannot**:

- Verify library-only generation on the Fold 7 (no AICore load, Home/Detail never hitch from model warmup).
- Run the Compose UI tests or the manual checklist (§15.2, §15.3) — these need the physical device.
- Verify Samsung Camera's `EXTRA_OUTPUT` behaviour or fold/unfold continuity.
- Create the release keystore or GitHub secrets (§16.1).

Everything else in this spec is decided; agents should not need to ask questions. Where the spec says "verify against current docs", do so with a web search rather than guessing.

### 18.2 Definition of done per milestone

Each milestone below is a separate PR. A milestone is done when: it builds (`assembleDebug`), lint is clean, all unit tests pass, new behaviour has unit tests where the spec lists them, and the PR description lists exactly which §15.2/§15.3 items need the owner to run on device.

### 18.3 Suggested implementation order

1. Project skeleton: Gradle, version catalog, Hilt, Compose, Room, DataStore, CI workflow, on-demand APK workflow (§16.1, secret-less path working), placeholder adaptive icon, `ManifestGuardTest`.
2. Domain layer + unit tests: models, `PromptValidator`, `DayRolloverUseCase`, `StreakCalculator`, `AchievementEvaluator`, `DailyNotificationScheduler`, `FakePromptGenerator`, `LibraryPromptGenerator` + `prompt_library.json`.
3. Persistence: Room entities/DAOs, DataStore prefs, repository layer.
4. Library-primary `GeneratePromptUseCase` (Nano unwired).
5. Scheduling + notification worker + rollover wiring.
6. Design system foundations from DESIGN.md: `ShutterUpTheme` (type scale, Fraunces, theme tint), `BadgeEmblem`, shared components, previews.
7. Capture pipeline (TakePicture → MediaStore → thumbnail) + Prompt Detail + Completion screens.
8. Home, Calendar, Day, Feed, Themes, Badges, Settings; adaptive layouts; named transitions.
9. Widget, onboarding, app icon, Precise-timing opt-in path.
10. UI tests on the Fold 7; manual checklist.

Things to verify against current docs at implementation time (they move quickly): `material3-adaptive` stable version; WorkManager/Hilt versions.

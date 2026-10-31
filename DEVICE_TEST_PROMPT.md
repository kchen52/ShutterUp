# ShutterUp device test prompt

Hand this entire file to an agent that has a physical Android device over ADB.
The agent should follow it as a runbook, not as background reading.

Source of truth if a rule is missing: `SPEC.md` and `DESIGN.md` in this repo.

---

You are a QA agent with a physical Android device connected over ADB. Install and exercise **ShutterUp** as a real user would, then report pass/fail with evidence.

This is **manual device QA**, not unit or instrumented CI. Compose UI tests already stub the camera. You must use the **real Samsung Camera**, **real Gallery**, **real notifications**, **real widgets**, and **real fold/unfold**.

Do not change product code unless a crash or blocker makes the rest of the plan unrunnable. Prefer documenting bugs over fixing them mid-run.

## Product

ShutterUp is a fully offline daily photography journal. One prompt per day (Gemini Nano on-device, with a 220-prompt library fallback), one still photo via the **system camera** into `Pictures/ShutterUp`, light streaks/freezes/badges, calendar/feed/history, Glance widgets, and foldable-aware layouts. No accounts, no internet permission, no in-app camera, no late entries, no multiple photos per day.

| Item | Value |
|---|---|
| Package | `app.shutterup` |
| Launcher activity | `app.shutterup.MainActivity` |
| Target device | Samsung Galaxy Z Fold 7, Android 16 / One UI 8, Gemini Nano v2 |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Build | `./gradlew assembleDebug` |
| Photo album | `Pictures/ShutterUp/` |
| Filename | `yyyy-MM-dd_<theme-slug>.jpg` |
| Notification channel | `daily_prompt` / "Daily prompt" |
| FileProvider | `app.shutterup.fileprovider` |

Four destinations: **Today · Calendar · Feed · Badges**. Cover screen = compact (bottom nav). Inner screen = expanded (nav rail + list-detail).

## Ground rules

### How to work

- Confirm a single device with `adb devices`. If more than one, pick the Fold 7 and pass `-s <serial>` on every command.
- Interact through the UI like a person. Use ADB to install, grant, deep-link, screenshot, and inspect — not to fake a camera result.
- After every material action, screenshot and keep a running log.
- Cover and inner: cases marked **FOLD** must run on both, or fold mid-flow as specified.
- Cases marked **WAIT** need a real 1–5 minute wait, not a clock hack.
- Leave the device usable. Restore notification time, city, pause, series, theme focus, Precise timing, font scale, dark mode, animator scale, and AICore if you disable it. Do not factory-reset. Do not change timezone permanently. Do not leave the system clock in the future.

Useful inspection commands:

```bash
adb devices -l
adb exec-out screencap -p > /tmp/su-<id>.png
adb shell uiautomator dump /sdcard/window_dump.xml && adb pull /sdcard/window_dump.xml
adb shell dumpsys notification --noredact
adb shell dumpsys package app.shutterup
adb shell dumpsys alarm
adb shell dumpsys jobscheduler
adb shell pidof -s app.shutterup
adb logcat --pid=<pid>
```

MediaStore (filter results for `ShutterUp`):

```bash
adb shell content query --uri content://media/external/images/media \
  --projection _display_name:relative_path:date_added:width:height \
  --sort "_id DESC"
```

Deep links (replace `YYYY-MM-DD` with today's local date):

```bash
adb shell am start -a android.intent.action.VIEW -d "shutterup://day/YYYY-MM-DD"
adb shell am start -a android.intent.action.VIEW -d "shutterup://day/YYYY-MM-DD?autoLaunchCamera=true"
adb shell am start -a android.intent.action.VIEW -d "shutterup://day/YYYY-MM-DD?reroll=true"
adb shell am start -a android.intent.action.VIEW -d "shutterup://detail/YYYY-MM-DD"
adb shell am start -a android.intent.action.VIEW -d "shutterup://detail/YYYY-MM-DD?autoLaunchCamera=true"
```

### Time travel and debug tools

Use a **debug APK**. Settings → Debug (debug builds only) exposes:

- **Use fake AI**
- **Force day rollover**
- **Seed 60 days of history**
- **Reset all data**

Preferred tricks:

- Time travel: **Force day rollover** (do not change the system clock unless a case is otherwise impossible; if you must, restore immediately).
- Notification timing: set Notification time to about 2 minutes from now, background or force-stop the app, wait.
- History: **Seed 60 days of history** after empty-state tests. Seed uses the same dummy title on every day and a mix of completed / skipped / missed / completed-no-photo / paused, plus some frozen days.
- Clean journal: **Reset all data** wipes Room (not necessarily Gallery files) and does not by itself reset the onboarding DataStore flag. For a true first run, uninstall then reinstall: `adb uninstall app.shutterup`. Photos in Gallery survive uninstall; that is specified.
- Camera: take real photos of the desk or wall. They land in Gallery under `Pictures/ShutterUp`. Clean them up at the end.

### Pass / fail

| Result | Meaning |
|---|---|
| PASS | Observed behaviour matches SPEC/DESIGN copy and layout rules |
| FAIL | Mismatch, crash, lost state, wrong photo, wrong streak, extra permission, or visual breakage |
| BLOCKED | Could not run; say what you tried |
| N/A | Out of scope for this device or session |

Do not stop the whole plan on a single FAIL unless the app will not launch. Continue, note the bug, work around it.

Visual / copy lint on every screen:

- Quiet, editorial, warm. Photos win. Exactly one Fraunces headline per screen.
- No red for missed or skipped days. No emoji. No exclamation marks in system copy (badge descriptions may have one). Never "Oops". Never guilt ("Don't break your streak!").
- Touch targets at least 48 dp. Colour is never the only status signal.
- Theme tint is a low-alpha wash, never a painted UI.
- Library prompts show a tiny **From the library** tag. Nano prompts do not.
- Generation must not block the UI. While prompts are being written, a slim bar may show **Generating prompts for &lt;Weekday&gt;** or **Generating prompts for a seven-day series**.

### Short pass vs full pass

If asked to keep it short, run **A, D, E, G, H, M2–M4, N, P1** and treat the rest as extended. Otherwise run everything.

---

## 2. Setup

1. `adb devices -l` — record serial, model (Fold 7, often `SM-F966` or similar), Android version (`getprop ro.build.version.release`), SDK (`getprop ro.build.version.sdk`).
2. Build if needed: `./gradlew assembleDebug`. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Uninstall first only after confirming this is the test install of ShutterUp.
3. Confirm package: `adb shell pm path app.shutterup`.
4. Confirm **no INTERNET** granted. Confirm CAMERA, READ_MEDIA_IMAGES, ACCESS_FINE_LOCATION, and ACCESS_COARSE_LOCATION are **not** requested.
5. Record starting permissions.
6. Note fold state. You will use both cover and inner physically.
7. Create an evidence directory and a results table you will fill as you go.

If the app is already past onboarding, start with uninstall + reinstall so first-run (section A) is real. After A and B, you may Seed 60 days.

---

## 3. Test plan

### A. First run / onboarding — FOLD

**A1. Cold launch.** Uninstall, install, start `app.shutterup/.MainActivity`. Expect 4-page onboarding, not Home. Page 1: "ShutterUp" wordmark + one sentence, soft shifting gradient, **Next**. Cover: single column. Inner: same, centred, max about 600 dp, not stretched edge to edge.

**A2. Notifications page.** Mock notification card built in Compose, not a bitmap. **Allow notifications** triggers POST_NOTIFICATIONS. Allow it and confirm the permission is granted. On a later pass (or after uninstall): Deny, finish onboarding, confirm Home shows **Turn on notifications to get your daily prompt** with **Open settings** (not an error-red card). Open settings lands in the app's notification settings.

**A3. Time + theme focus.** Default notification time **09:00**. Change it to a distinctive time (for example 07:35) and confirm it sticks. Theme focus placeholder **Leave blank and I'll surprise you**. Leave blank on this pass.

**A4. AI status page.** **Get my first prompt**. If Nano is available, continue; Settings later should show **Ready** plus a model name. If downloadable/downloading: **Preparing on-device AI** with progress; you may continue; library used until ready. If unavailable: library copy; prompts tagged **From the library**.

**A5. Land on Home.** Today's prompt is visible immediately (valid from 00:00, not from notify time). Four destinations present. Settings gear top-right on Today (cover); on inner, Settings at the bottom of the rail. No camera permission dialog at any point.

**A6. Configuration change during onboarding.** Mid-onboarding, fold/unfold and rotate the inner display. Page index, entered time, and theme-focus text must survive.

### B. Empty states (before seeding)

**B1. Calendar** with no history: centred **Your first photo goes here.** plus **Come back after today's prompt.** No clip-art.

**B2. Feed** same empty treatment. No Monthly card on a mid-month first install (the current month is never due).

**B3. Badges** with none unlocked: full grid of locked emblems is the empty state. Sections **Streaks · Count · Explorer · Time of day · Special**. Locked names are hints, not the real names. Tap a locked badge: dialog with outline/blurred emblem + hint.

### C. Home / Today card — FOLD

**C1. Pending today.** Kicker `WEEKDAY · THEME` uppercase with tracking. Title in Fraunces. One-liner. **Shoot** filled pill + **Details**. Pending card wraps content (no empty cavern inside the card). Status row: streak (`N days`), freeze snowflakes, month ring.

**C2. Details** opens Prompt Detail. Back returns to Home with the same prompt.

**C3. Inner list-detail.** Left about 40% Home. Right about 60% Prompt Detail for today. **Shoot lives in the right pane, not across the hinge.**

**C4. Recent strip.** Empty until there are completed days. After a completion (section E): 1:1 thumbs, horizontal, tap opens Day.

**C5. Generation bar.** On first launch or after theme-focus / series changes, a slim non-blocking bar may appear: **Generating prompts for Saturday** (weekday) or **Generating prompts for a seven-day series**. Home stays usable. The bar disappears when generation finishes.

### D. Prompt Detail — FOLD

**D1. Content stack.** Date kicker, theme, title, one-liner, details, how-to, tips, optional constraint card, remaining-time line, sticky bottom bar: **Shoot**, **Reroll**, **Skip**.

**D2. Remaining time, no city.** `N hours left today` (minutes under an hour). Never coloured, no icon. Only on Prompt Detail — not Home, not the widget, not the notification.

**D3. Remaining time, with city.** Settings → Prompts → **Where you are**. Supporting text: city-level guess, no location permission. While the sun is up: `N hours of good light left`. After sunset / before sunrise: fall back to the clock line. Do not say the light is gone. Unset the city and confirm the clock line returns. Changing city must not regenerate today's prompt unless the hemisphere changed; if hemisphere changes, the future buffer regenerates and today stays.

**D4. Reroll.** Supporting **One reroll a day.** Tap Reroll: new prompt. Button becomes disabled **Rerolled**. Notification Reroll action should also disappear after this. The discarded prompt must not appear as today's calendar cell.

**D5. Skip.** Dialog title **Skip today?** Body mentions skipped plus streak or freeze. Buttons **Skip** / **Keep going**. Cancel leaves PENDING. Confirm: Home card **Skipped — see you tomorrow.** Title struck through. MISSED never appears on the today card.

**D6. Camera cancel.** Shoot, then cancel in the system camera. Return to Prompt Detail, nothing saved, day still PENDING, no new Gallery file.

### E. Capture happy path — FOLD

Do this on **cover**, then again on **inner** (Retake, or a fresh day via rollover). At least one pass must be a true first completion so **First Light** can unlock.

**E1. Shoot on cover.** System camera opens with ShutterUp as caller. Take a still, confirm. Return to ShutterUp **Completion**, not a blank Home.

**E2. Completion.** Native-aspect photo, aperture-settle check (or the final check if animations are off), kicker, prompt title, **Add a note…**, streak, **Done / Share / Retake**. First-ever completion: headline **First light.** body **Day one. Everything else is repetition.** If a badge unlocks: modal sheet with emblem, name, one-line description, **Nice**. One badge per sheet.

**E3. Gallery.** Photo exists under `Pictures/ShutterUp/`, name `yyyy-MM-dd_<theme-slug>.jpg`. Original is camera-sized, not a 400 px re-encode. App thumbnail is separate.

**E4. Notification dismissed** once COMPLETED.

**E5. Note.** Type a note on Completion, Done. Reopen: note persisted. Notes must never appear on a Share card.

**E6. Retake.** Before midnight, Retake replaces the photo. Old file gone from MediaStore if the app created it.

**E7. Unfold mid-capture.** From cover, Shoot, unfold while Camera is in front, take the photo. Must land on Completion without losing the pending capture. Reverse: start on inner, fold to cover mid-camera, finish.

**E8. Kill during save (best-effort).** Shoot, take photo, `adb shell am force-stop app.shutterup` as you confirm (racy). Relaunch. Leftover pending file for today should complete the save. If you cannot hit the race, mark BLOCKED with what happened.

**E9. Home completed state.** Card becomes the photo with a bottom scrim, kicker and title over it, buttons **Add a note** / **Retake**. Month ring and streak updated.

### F. Photo Picker fallback

Hard to force "camera fails twice" without mocks. Cancel camera twice from Shoot. If the inline card **Camera didn't return a photo. You can pick one you took today instead.** plus **Choose from Gallery** appears, use it. If it does not appear after two cancels, record that — SPEC says unavailable/intent-fails-twice, which may differ from user cancel. Do not fail the whole capture suite solely on this.

If the picker appears:

- Photo taken today: accepted, copy in `Pictures/ShutterUp`, original left in place.
- Photo from another day: snackbar **That one's from another day — only today's photos count.** Day stays PENDING.

### G. Day screen, delete, missing original — FOLD

**G1. Layout.** Full-bleed photo at top, then prompt stack, editable Note (autosave), action row **Delete**, **Share** (if photo), **Shoot this again** (past photographed days), **Retake** (today only). At 200% font the action row wraps, never clips, never horizontal-scrolls.

**G2. Full-screen viewer.** Tap the photo. Back returns.

**G3. Delete from ShutterUp only.** Dialog **Delete this photo?** / **The day stays complete.** Buttons **Delete from ShutterUp** / **Also delete from Gallery** / **Cancel**. First option: day stays complete with no photo (calendar check, no thumb). Share disappears. Gallery file remains. Streak still counts.

**G4. Also delete from Gallery.** On another completed day: app photo and Gallery file gone. Snackbar **Deleted.**

**G5. Original missing.** Complete a day. Delete that file in Gallery. Reopen Day: thumbnail plus **Original missing**. Delete still offered.

### H. Share card

**H1.** Share on Completion and on Day when a photo exists. System share sheet, not an in-app feed.

**H2.** Save the PNG somewhere you can open. Expect photo, date kicker `D MONTH · THEME`, prompt title, small **ShutterUp** wordmark. Note absent. No streak, no badge, no series name. PNG about 1080 px wide, not the original JPEG. Pull the file and confirm it has no camera GPS / DateTimeOriginal if `exiftool` or `file` is available.

**H3.** Share is absent for completed-without-photo.

**H4.** If share fails, snackbar **The card didn't come together. Try again.** Do not force a failure.

### I. Second take

Needs a past photographed day and today still PENDING. Seed, then pick a seeded completed day that is not today.

**I1.** **Shoot this again.** Dialog title **Shoot this again?** Body **This becomes today's prompt.** if today is PENDING, else **This becomes tomorrow's prompt.** Buttons **Shoot this again** / **Not now**.

**I2.** Confirm onto today. Today's prompt is the copied prompt. Completing it shows a diptych (current vs previous). Compact: stacked. Inner: side by side, seam not on the hinge. Kickers under frames (earlier date vs interval such as `N days on` / `a month on`). One shared headline. Tap either photo opens the viewer.

**I3.** Original day's screen gets **Shot again on &lt;date&gt.**

**I4.** Reroll on a second-take today clears the repeat and generates an ordinary prompt. Still only one reroll per day.

**I5.** If today is already completed or skipped, second take lands tomorrow. If that target is also acted on: **Tomorrow is already spoken for.** / the action does nothing.

**I6.** After section L: repeating onto a series day pulls that day out of the series (that week's dot unfilled) without ending the series.

### J. Calendar, year-in-hues, Feed, Themes, Badges — FOLD

Seed 60 days after empty-state tests.

**J1. Month grid.** Swipe months and use the chevrons. Cells: completed thumbnail; completed-no-photo check; skipped outline + dash (not red); missed small dot (not red); frozen snowflake; paused faint number; today pending primary ring; future muted number. Footer: month ring + longest streak.

**J2.** Tap completed opens Day. Tap skipped/missed opens Day with kicker `SKIPPED · THEME` or `MISSED · THEME`.

**J3. Year in hues.** Pinch-out or top-bar **Year**. Twelve month bands, theme-accent fills for completed days, no photos or numbers in cells. Must not look like a GitHub contribution graph. Swipe years. **Month** or pinch-in returns. Tap a cell opens Day. Inner: year in the list pane, Day on the right. Animator scale 0: instant crossfade.

**J4. Inner Calendar** is list-detail (grid left about 45%, Day right). Today selected by default.

**J5. Feed.** Newest first, native-aspect cards, kicker `D MON · THEME`, title. Chip row **All** plus per-theme chips. Non-completed days absent. Inner: two-column staggered grid.

**J6. Themes.** From Feed chips: grouped list with counts, then filtered feed. Inner: list left, grid right.

**J7. The Monthly.** Mid-month first install: no issue. After Seed, if completed days exist in a previous calendar month, a Feed card may appear. Dismissible with **Dismiss** (does not delete). **Past issues** opens a list titled **The Monthly**. Issue page: `{MONTH} · N DAYS`, one Fraunces headline, contact sheet, body, themes kicker at the foot. No notification for the issue. Tapping a thumb opens that Day. If none appears, force-stop and relaunch, check logcat for `MonthlyIssueWorker`, then mark BLOCKED if still missing.

**J8. Badges.** Stat row for current and longest streak. Grid 3-col cover / 5-col inner. Unlocked show names; locked show hints. After First Light, that emblem is unlocked.

### K. Streaks, freezes, skip/miss

**K1.** Complete today; streak increments on Completion.

**K2.** Skip with freeze available: streak held, freeze count down, skipped day marked frozen.

**K3.** Skip with zero freezes: streak breaks. Copy must not nag.

**K4.** **Force day rollover** while today is PENDING: today becomes MISSED (freeze consumed if any). Record actual UI honestly; after debug rollover the app's "today" may have moved.

**K5.** Paused days are neutral, not misses.

### L. Settings

**L1. Daily prompt.** Change Notification time; kill app; reopen — persisted. Precise timing off by default. Turn **Precise timing** on. If exact alarms are not granted: **Needs permission → Open settings**. After grant: **Allowed**. Supporting text: delivers at the exact minute. `dumpsys alarm` should show ShutterUp when precise is on; WorkManager when off. Revoke exact-alarm permission: fallback to WorkManager plus a Settings banner, no crash.

**L2. Pause.** Supporting: **No prompts or notifications. Paused days don't affect your streak.** While paused: no new prompts, no notifications, Home card **Paused** plus **Resume**. Unpausing generates today's prompt immediately.

**L3. Series** (default off). Enabling must not change today's prompt. Future days become a 7-day run. Home kicker `{SERIES TITLE} · N OF 7` and a seven-dot row (current = ring, completed = fill, missed = unfilled, never red). Prompt Detail: date kicker stays; series line takes the theme slot. Reroll inside a series stays on theme. Turning Series off lets the current series finish; already-generated prompts are not deleted. Generation bar may read **Generating prompts for a seven-day series**.

**L4. Theme focus.** Set something doable, for example `reflections`, at most 60 characters. Later generations should incorporate it. Clearing restores surprise. Changing focus keeps today's prompt unless you reroll.

**L5. AI status.** Read-only Ready / Preparing / Unavailable. Debug **Use fake AI**: generations become deterministic; toggle off to use Nano again.

**L6. Photos.** Save location **Pictures/ShutterUp**. Storage used is a real number that moves after a capture.

**L7. About.** Version. **Everything stays on your phone. ShutterUp has no internet access.** Privacy dialog. Licences: Fraunces OFL.

**L8. Debug.** All four rows respond. Reset does not crash. After Reset, record whether Home regenerates a prompt while onboarding stays complete.

**L9. Battery hint.** Only if the app is restricted. Do not restrict battery unless you can reverse it. Copy: **ShutterUp may be delayed by battery restrictions.** plus **Open battery settings**. Never auto-prompt for unrestricted battery.

### M. Notifications and scheduling — WAIT

**M1. Channel.** Apps → ShutterUp → Notifications: **Daily prompt**, importance default, does not bypass DND.

**M2. WorkManager path.** Precise timing off. Set notify time to 2 minutes from now. `adb shell am force-stop app.shutterup`. Wait. Notification still appears. Title = prompt title, text = one-liner, expanded = details. Actions **Shoot** and **Reroll** (Reroll hidden if already used). Large icon is a monogram, not the photo.

**M3. Tap notification body** opens Prompt Detail for that date. Auto-cancel.

**M4. Shoot action** opens Prompt Detail and launches the camera. Cancel camera: still on Detail.

**M5. Reroll action** produces a new prompt and consumes the reroll.

**M6. Precise path.** Enable Precise timing and grant exact alarms. Set time 2 minutes out. Force-stop. Wait. Record timestamp vs set time (Samsung may still batch). Reboot is optional; if you reboot, the notification must reschedule.

**M7. Already completed.** Complete today, then wait across a notify time you set in the near future: no notification for a COMPLETED or SKIPPED day.

**M8. Opened before notify time.** Prompt already on Home; notification still fires at the chosen time if still PENDING.

**M9. Deep links** from ADB, cold and warm start. Invalid date must not crash.

### N. Widgets

**N1.** Add both ShutterUp widgets: small 2×2 and medium 4×2.

**N2. Pending 4×2.** Theme-tinted, kicker, serif title, one-liner, Shoot pill. Tap body → Prompt Detail. Shoot pill → camera.

**N3. Pending 2×2.** Kicker + title only. Tap → Detail.

**N4.** After completing today, widgets show thumbnail plus a small check chip, not the old text.

**N5.** Pause → title slot **Paused**. Resume → prompt returns.

**N6.** Reroll or a new day updates the widget without re-adding it.

### O. Nano quality and library fallback

**O1.** Fake AI off. Collect about 10 generated titles (one reroll per day, so use Force rollover / Reset / new days as needed). They must be doable with a phone in 30 minutes, not require strangers, children, private interiors, danger, or specific weather, and must have no emoji, hashtags, URLs, or dedicated-camera gear. Mix of indoor-capable prompts.

**O2. Library path.** Optional; restore after. Android Settings → Apps → **AICore** → Disable. Relaunch, generate. Expect **From the library** and Unavailable status. Re-enable AICore before finishing.

**O3.** Low battery under 15%: SPEC says do not call Nano. Try only if you can charge-toggle; otherwise N/A.

### P. Foldable, multi-window, dark, font — FOLD

**P1.** Every destination on cover and inner: Today, Detail, Completion, Calendar, Year, Day, Feed, Themes, Badges, Settings, and Onboarding if you uninstall once more. Rotate inner. No orientation lock. State preserved.

**P2.** Fold/unfold on Completion: photo stays; aperture check does not replay.

**P3.** Multi-window / split with another app: layouts reflow, not clipped into unreadability.

**P4.** Tabletop / HALF_OPENED is v1.1. If it does something special, record it; otherwise N/A.

**P5. Dark mode.** Follows system. Photos untinted. Missed still not red. Share card matches appearance.

**P6. Font 200%.** Titles wrap up to 3 lines then ellipsize, never shrink. Action rows wrap. Calendar still usable. Restore font afterwards.

**P7. Animator duration scale 0.** Named motions become instant. Year zoom is a crossfade. Restore afterwards.

**P8. TalkBack (optional).** Calendar cells announce like **19 September, completed** or **17 September, missed, streak frozen**. Completion announces **Completed** once; photo is **Today's photo**. Badge emblems include name plus locked/unlocked. Disable TalkBack afterwards.

### Q. Permissions, privacy, process death

**Q1.** Runtime/manifest: POST_NOTIFICATIONS (and exact-alarm as an app-op). No INTERNET, CAMERA, storage, or location.

**Q2.** Logcat while shooting and rerolling: no crash loops.

**Q3.** `adb shell am force-stop app.shutterup` on Home, Detail, Completion note draft, Settings text field — relaunch. Pending capture URI, selected date, and note draft should survive where SPEC requires SavedStateHandle. Record what actually survives.

**Q4.** `adb install -r` the same APK. Notifications still scheduled. Widget still present.

### R. Cleanup

1. Delete test photos from Gallery or via in-app **Also delete from Gallery**.
2. Restore notify time, city, series, pause, theme focus, precise timing, fake AI off, font, dark mode, animation scale, AICore enabled.
3. Leave the debug APK installed unless asked otherwise.
4. Do not commit device-only artifacts into git unless asked.

---

## 4. Report format

Return a single markdown report:

1. **Device** — model, serial, Android, One UI, Nano available yes/no, APK versionName/versionCode, fold tested yes/no.
2. **Summary counts** — PASS / FAIL / BLOCKED / N/A.
3. **Must-fix FAILs first** — each with case id, expected, actual, screenshot path, logcat snippet if crash.
4. **Full case table** — `id | result | notes`.
5. **Prompt quality notes (O1)** — list titles; call out any content-contract violations.
6. **Design nits** that are not spec breaks.
7. **What you could not run** and why.

Be literal. "Seemed fine" is not a result. Quote on-screen copy when you check strings.

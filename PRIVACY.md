# ShutterUp privacy policy (stub)

Everything stays on your phone. ShutterUp has no internet access.

- No accounts, no cloud sync, no analytics, no crash reporting.
- Your photos live in your Gallery (`Pictures/ShutterUp/`); the app's database
  and settings participate in Android Auto Backup like any other app data.
  Settings → Photos can write a backup ZIP of progress and photos that you keep.
- The daily prompts come from a built-in library that stays on your phone.
- Share is a system share sheet (`ACTION_SEND`), not an upload. ShutterUp
  never opens a network socket; the destination app is one you already have
  and chose. What leaves is a freshly rendered PNG card (photo, date, theme,
  title). The original photograph is not attached, so none of its EXIF
  travels with the card — no camera make or model, no `DateTimeOriginal`,
  and no location even if the camera wrote GPS tags. Notes never leave
  the app.

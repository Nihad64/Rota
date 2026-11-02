# Rotam 3.0 implementation checkpoint

Work in progress on `feature/rotam-3-redesign`, draft PR #2. Do not merge or call this a production update yet.

## Verified checkpoint

Commit `d0c3dc3065a8eacc75f78b6d7ff28566589d3192`: Kotlin compilation and debug APK generation succeeded. All 14 JVM/Robolectric tests passed (7 rules, 7 repository tests). Lint stopped at one Android 26 incompatibility in the legacy `Theme.Rota` resource; this is fixed in `3a4b7f49091c38e3afeb7a8f83c4dd69f0fa9aa3`. That commit also adds a regression for clearing an end time while a reminder is enabled. Commit `3a4b7f49091c38e3afeb7a8f83c4dd69f0fa9aa3` passed all 15 tests, lint and APK generation. Emulator verification is in progress. Commit `f2f17cad049d19e3ab25fbeb12164f5e2a2dba1c` adds the timeline regression (16 tests total) and signed upgrade fixture; its CI is in progress.

## Implemented

- Independent nullable start and end, explicit overnight end date, per-plan reminders, persistent recurrence rules and exception tombstones.
- Sequential creation, persistent drafts, single-page editing, categories and details, linked goals, duplication, templates, conflict warning for known intervals only.
- Shared dark/ivory/orange design, approved R vector, home/calendar/archive, settings and AI overlay.
- Persistent focus session with monotonic elapsed time, pause/resume, optional countdown, measured time separate from planned duration; no automatic completion.
- Goals derive completed linked-plan progress. Deleting a goal preserves plans and removes links from future series templates.
- Versioned JSON backup of plans/goals/series, validated restore preview, atomic replacement and automatic recovery snapshot. API keys excluded.
- Encrypted per-device Gemini key; explicit preview/confirmation; idempotent recurring proposals; context sharing opt-in.
- Alarm rescheduling, quiet hours, completion/skip/snooze actions, reboot maintenance. Home widget shows next plan, completion and add actions.
- CI: JVM tests, lint and APK build; Android 35 emulator test for creation, independent optional times, archive search and single-page editing, with screenshots.

## Still needs verification or completion

- Complete current CI, inspect emulator screenshots, fix any UI flow failures. Test actual permission/reboot/quiet-hour delivery on device; Gemini network requests require the user's own API key.
- In-place signed upgrade from installed 2.5.6 (package com.rota.app, versionCode 32). New code is 33; the historical signing identity was found embedded in the legacy workflow. CI restores it ephemerally for an install-over fixture test. The key exposure in repository history remains an actual security issue; do not rotate it silently or rewrite history without an explicit migration plan. The CI APK is not a verified user-device update until the upgrade test passes. Never instruct uninstalling the user's existing app.
- Day/selected-week timeline blocks and 7/30-day statistics are now implemented; inspect their rendering with long titles and larger fonts.
- Secondary proposals still pending: custom categories, undated Later list, count-limited recurrence, multiple reminders, custom snooze interval, arbitrary multiselect move, richer goal quantities/substeps, and cloud sync (separate scope). Do not claim these are complete.
- Export/restore currently covers plan data, goals and series; settings, drafts and templates are device-local.

## Resume

Read latest branch/CI before editing. Library design references were inspected: libfile_978530e545548191af10048444d12031 (approved home) and libfile_85c6cb65f8b48191a3debe8ce5894960 (approved logo). `docs/PLAN.md` contains the full agreed plan. Keep durable progress on this branch.

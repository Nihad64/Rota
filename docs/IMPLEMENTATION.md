> Firebase devamı: `rotam-nihad64` oluşturuldu; Google sağlayıcısı deploy edildi. AI Logic Auth-only ve App Check enforced. SDK bağlantısı/Google config güncellendi; gerçek kullanıcı ve model erişim testi henüz tamamlanmadı. Ayrıntı: [Firebase raporu](FIREBASE-PROVISIONING-2026-11-09.md).

> 9 Kasım 2026: Aşağıdaki metin tarihsel kontrol noktasıdır. Güncel kararlar için [gereksinimler ve EK A](REQUIREMENTS-2026-11-09.md), uygulama/test durumu için [9 Kasım raporu](UPDATE-2026-11-09.md) esas alınmalıdır. Eski kullanıcı API anahtarı, global AI, Hedefler ve ayrı kapsam notları güncel ürün kararı değildir.

# Rotam 3.0 implementation checkpoint

Work in progress on `feature/rotam-3-redesign`, draft PR #2. Do not merge or call this a production update yet.

## Verified checkpoint

Commit `56427ec51d9b0419333aa08aedc8cb2ae26cd5f1` passed [CI run 41563154425](https://github.com/Nihad64/Rota/actions/runs/41563154425): all 16 JVM/Robolectric tests, Android lint, debug APK build and Android 35 emulator instrumentation. The emulator installed the actual signed 2.5.6 APK, seeded a synthetic legacy plan, installed 3.0 over it, and verified the old title, note and start time survived. It then created a plan without either time, opened its single-page editor, changed the title, saved, and checked the persisted title and null times.

The previous and review APK signing certificates match; application ID remains `com.rota.app`, version code increases from 32 to 33. This confirms the tested upgrade path, not every physical device or every existing user dataset. Lint has warnings but no blocking errors.

Visual review found the global AI action covering the plan editor footer. The editor now owns a separate AI row above its primary action; the device test asserts their bounds do not overlap. Home, initial creation, optional-time creation, editing, day calendar, goals, settings and 30-day statistics have been inspected at default font scale. Compose PixelCopy synchronizes captures with rendering instead of taking an intermediate system-compositor frame. Screenshots are copied out before test cleanup, and CI fails if they cannot be retrieved.

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

- User device feedback (3 November 2026): the installed review APK has many visual and hardware/behavior problems. Exact symptoms, device details and installed build are not yet supplied. Reproduce those reports before release; a green emulator run is not user-device acceptance.

- Test actual permission/reboot/quiet-hour delivery on a physical device; Gemini network requests require the user's own API key.
- The historical signing identity is embedded in old repository history. CI restores it ephemerally to preserve install compatibility. This existing key exposure needs an explicit migration plan; do not rotate silently or rewrite history. Never instruct uninstalling the user's existing app.
- Day/selected-week timeline blocks and 7/30-day statistics are implemented. Larger fonts, narrow devices, long titles and dense overlapping schedules still need broader visual coverage.
- Secondary proposals still pending: custom categories, undated Later list, count-limited recurrence, multiple reminders, custom snooze interval, arbitrary multiselect move, richer goal quantities/substeps, and cloud sync (separate scope). Do not claim these are complete.
- Export/restore currently covers plan data, goals and series; settings, drafts and templates are device-local.

## Resume

Read latest branch/CI before editing. Library design references were inspected: libfile_978530e545548191af10048444d12031 (approved home) and libfile_85c6cb65f8b48191a3debe8ce5894960 (approved logo). `docs/PLAN.md` contains the full agreed plan. Keep durable progress on this branch.

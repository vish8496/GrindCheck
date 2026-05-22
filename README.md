# GrindCheck — AI Gym Form Checker

Real-time 30fps pose tracking + Claude AI coaching on Android.

Native Kotlin + Jetpack Compose + Google MLKit Pose Detection.

## Stack

| Layer | Tech |
|---|---|
| Mobile | Kotlin + Jetpack Compose (Material 3) |
| Camera | CameraX |
| Pose detection | Google MLKit Pose Detection (33 landmarks, on-device, 30fps) |
| AI coaching | Anthropic Claude (called via Railway server) |
| Backend | Node.js + Express on Railway |
| Auth + DB | Supabase (Postgres + Row-Level Security) |
| Payments | Razorpay (₹99/mo premium) |

## Repo layout

```
grindcheck/
├── android-native/   # Kotlin Android app (this is the product)
├── server/           # Node.js Express server — POST /feedback → Claude
└── supabase/         # SQL schema (profiles / sessions / leaderboard)
```

## Phase status

- [x] Phase 1 — Native scaffold + CameraX + MLKit + live skeleton overlay
- [x] Phase 2 — Squat rep counting + Claude feedback wiring
- [ ] Phase 3 — Supabase auth + session history + leaderboard
- [ ] Phase 4 — Pushup, plank, deadlift, curl detectors + tutorial
- [ ] Phase 5 — Razorpay premium gating

## Quick start

### Build the Android app

Requires: Android Studio (or just the Android SDK + JDK 17+).

```bash
cd android-native
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Or open `android-native/` in Android Studio and hit Run.

### Backend (Railway)

```bash
cd server
npm install
ANTHROPIC_API_KEY=sk-ant-... node index.js
```

Or deploy to Railway — pushes to `main` auto-redeploy the `server/` folder.

### Supabase

Run `supabase/schema.sql` in the SQL editor. Sets up `profiles`, `sessions`, `leaderboard` tables with RLS policies and triggers.

## Environment variables

Server side (Railway dashboard → Variables):
- `ANTHROPIC_API_KEY` — Anthropic API key
- `CLAUDE_MODEL` — optional; defaults to `claude-sonnet-4-5`
- `PORT` — Railway sets this automatically

Android side (`android-native/gradle.properties` or pass via `-P`):
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `API_URL` — Railway server URL
- `RAZORPAY_KEY_ID`

Defaults pointing at the prod Supabase + Railway instances are baked into `app/build.gradle.kts` for development convenience.

## Why native, not React Native

We started on Expo + React Native and spent several hours stuck on the vision-camera v5 / fast-tflite / nitro-modules ecosystem version skew. The same setup that ships in 5 minutes in native Kotlin took 7 failed EAS builds in RN. Native build was the right tool.

The earlier RN exploration lives on the `native-build` branch for historical reference.

# Kazushiki Pilates for Android

The Android version of Kazushiki Pilates: a native Kotlin + Jetpack Compose replica of the
iPhone app (`amdtg2/kazushiki-pilates`): same screens, workouts, animations, challenges,
achievements, reminders and subscription.

## Install a test build on your phone

Every push to `main` builds the app on GitHub Actions and publishes it as the **latest** release.

1. On your Android phone, open this repo's **Releases** page (signed in to GitHub) and download
   `KazushikiPilates.apk` from "Latest test build".
2. Open the download. If asked, allow your browser (or the GitHub app) to install unknown apps.
3. Tap **Install**. New builds install over the old one and keep your data.

Test builds are debug builds: the Profile tab has testing tools to unlock Premium without paying.

## Project

- `app/src/main/java/com/kazushiki/pilates/`
  - `figure/` – the animated figure engine (poses, solver, drawing)
  - `exercises/` – the 34 exercises
  - `model/` – workouts, programs, challenges, profile, sessions, streaks, achievements
  - `data/` – on-device storage
  - `services/` – Google Play subscriptions, workout-day reminders, voice coach
  - `ui/` – theme, navigation and every screen
- `app/src/test/` – unit tests (run on every build)
- `PORTING.md` – how the Swift code maps to Kotlin

Open the folder in Android Studio to build locally.

## Before publishing to Google Play

- Create two subscriptions in Play Console: `kazushiki.pilates.monthly` ($12.99/month) and
  `kazushiki.pilates.annual` ($79.99/year), each with a 7-day free-trial offer.
- Replace the privacy policy link in `services/SubscriptionStore.kt`.
- Build a signed release bundle with your own upload key (not `app/debug.keystore`).

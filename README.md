# Lapse

Lapse is an Android app that keeps a daily report of time spent in Instagram, YouTube, and Reddit. It counts Instagram reels, YouTube Shorts, and Reddit posts scrolled past, and it can be paused.

Reports stay on the phone. The app has no account and does not request network access.

## What a day contains

| App | Time | Scrolls |
| --- | --- | --- |
| Instagram and Instagram Lite | Foreground time with the screen on | Reels scrolled |
| YouTube | Foreground time with the screen on, including regular videos | Shorts scrolled |
| Reddit | Foreground time with the screen on | Posts that leave the top of the feed |

Chats, comment threads, and browser tabs are not scroll counts. Opening a feed does not count the posts already on screen. Pausing, turning the screen off, or force-stopping Lapse ends the count until you come back.

The live counter is a small chip at the top of those apps. It does not take taps. Turn it off in Setup if you only want the report.

## Pause and force stop

- **Pause** on Today, on the tracking notification, or from the Lapse Quick Settings tile.
- **Force stop** from Android's app info screen also stops tracking. Open Lapse again afterward. On some phones, force stop turns the accessibility service off, and you enable it once more.

## Privacy

Lapse uses an accessibility service because Android has no other way to see a reel or a post inside another app. The service is limited to four packages: `com.instagram.android`, `com.instagram.lite`, `com.google.android.youtube`, and `com.reddit.frontpage`.

For every other app, Lapse only notices that you left, so the clock can stop. It does not walk those screens. Inside the four packages it reads view ids and short labels long enough to tell a viewer from a chat, then drops the words. Counts and durations are stored in a private database. Backup is off.

Scroll counts are estimates. An update to Instagram, YouTube, or Reddit can change the screens Lapse recognizes, so a count can miss or, less often, pick up an extra one. The time total is the steadier figure.

## Set up a phone

1. Install the debug APK, or run the `app` configuration from Android Studio.
2. Open Lapse and turn on its accessibility service. Android lists it under Accessibility, installed apps, or downloaded apps, depending on the phone.
3. Allow notifications if you want Pause in the shade.
4. Set battery to unrestricted so the counter is less likely to freeze while you scroll.
5. Optional: add the Lapse tile to Quick Settings.

## Build

The project uses Android Gradle Plugin 9.4, Gradle 9.6, and JDK 17 or newer. `compileSdk` is 37.2. `targetSdk` stays 36.

```bash
export ANDROID_HOME="$HOME/android-sdk"   # or your SDK path
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit tests cover the reel, Shorts, post, and session clocks without a device. Those counters live in `app/src/main/java/app/lapse/engine` and do not import Android.

## Play Store

Google Play expects an accessibility service to help people with disabilities. A personal tracker that uses the same API can be rejected or removed. Sideloading is the straightforward way to run Lapse on your own phone. If you ship it, the store listing has to describe the accessibility use in the declaration form, and the in-app disclosure has to match what the service actually reads.

Lapse is not affiliated with BrainPal, Instagram, YouTube, or Reddit.

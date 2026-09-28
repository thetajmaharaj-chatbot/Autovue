# AutoVue

AutoVue is an Android 16 media/browser project.

## Current features

- Phone WebView with YouTube as the start page
- Back, forward, home and URL navigation
- Fullscreen web video support on the phone
- Media3 / ExoPlayer direct media playback
- HLS and DASH playback modules
- Android Storage Access Framework picker for local audio/video
- Save multiple direct audio streams or accessible local audio items
- Manage and remove saved Android Auto audio sources
- Media3 MediaLibraryService for Android Auto discovery, browsing, search and playback
- Audio-focus and noisy-output handling for car playback
- GitHub Actions debug APK build

## Android Auto

Android Auto connects to AutoVue through the Media3 media library service and uses the car host's driver-optimized media UI. Saved direct audio/media sources appear in the AutoVue Audio library and can be searched and played from the supported media surface.

Phone video/browser playback is kept separate from the Android Auto media surface.

## Build

The GitHub Actions workflow builds:

`app/build/outputs/apk/debug/app-debug.apk`

and publishes it as the `autovue-debug-apk` workflow artifact.

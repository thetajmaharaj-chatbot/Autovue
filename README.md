# AutoVue

AutoVue is an Android 16 media/browser project.

## Current features

- Phone WebView with YouTube as the start page
- Back, forward, home and URL navigation
- Media3 / ExoPlayer direct media playback
- HLS and DASH playback modules
- Save one direct audio stream for the Android Auto media library
- Media3 MediaLibraryService for Android Auto discovery, browsing, search and playback
- GitHub Actions debug APK build

## Android Auto

Android Auto connects to AutoVue through the Media3 media library service and uses the car host's driver-optimized media UI. The saved item is intended to be a direct playable audio/media stream URL.

Phone video/browser playback is kept separate from the Android Auto media surface.

## Build

The GitHub Actions workflow builds:

`app/build/outputs/apk/debug/app-debug.apk`

and publishes it as the `autovue-debug-apk` workflow artifact.

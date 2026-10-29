package app.miyomu.player

/** ExoPlayer is a regular Apache-2.0 dependency, so playback always works on Android. */
actual fun isVideoPlaybackAvailable(): Boolean = true

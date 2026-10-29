package app.miyomu.player

/**
 * Whether this build can actually play video.
 *
 * Android ships the ExoPlayer backend (Apache-2.0) as a normal dependency, so it is always
 * available. The desktop backend needs the native MPV binaries, which Miyomu does not bundle
 * because the aggregate `mediamp-mpv-runtime` artifact is GPL-3.0 — see
 * docs/adr/0012-desktop-video-backend-licensing.md. A distributor that accepts those terms (or
 * points the player at a system MPV via `MpvMediampPlayer.prepareLibraries`) flips this to true.
 */
expect fun isVideoPlaybackAvailable(): Boolean

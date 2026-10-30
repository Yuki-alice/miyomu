package app.miyomu.player

/**
 * Whether this build can actually play video.
 *
 * Android ships the ExoPlayer backend (Apache-2.0) as a normal dependency, so it is always
 * available. Desktop bundles the MPV runtime only for macOS and Windows: those artifacts are
 * LGPL-2.1-or-later, while the Linux runtime (and the aggregate artifact) is GPL-3.0 and is
 * deliberately not distributed.
 */
expect fun isVideoPlaybackAvailable(): Boolean

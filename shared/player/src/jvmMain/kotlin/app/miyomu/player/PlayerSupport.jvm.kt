package app.miyomu.player

/**
 * Desktop playback depends on the MPV native binaries being on the classpath. mediamp's loader
 * looks for `mpv-natives-<os>-<arch>.txt`; probing for the same resource lets the UI say why
 * playback is unavailable instead of throwing from the player constructor. macOS/Windows ship
 * the runtime (ADR-0012 option A); Linux does not, so the probe stays false there.
 */
actual fun isVideoPlaybackAvailable(): Boolean =
    runCatching {
        val os = System.getProperty("os.name").lowercase().let { name ->
            when {
                name.contains("mac") -> "macos"
                name.contains("win") -> "windows"
                else -> "linux"
            }
        }
        val arch = System.getProperty("os.arch").lowercase().let { arch ->
            if (arch.contains("aarch64") || arch.contains("arm64")) "arm64" else "x64"
        }
        val resource = "mpv-natives-$os-$arch.txt"
        val loader = Thread.currentThread().contextClassLoader
            ?: PlayerSupportMarker::class.java.classLoader
        loader?.getResource(resource) != null
    }.getOrDefault(false)

private object PlayerSupportMarker

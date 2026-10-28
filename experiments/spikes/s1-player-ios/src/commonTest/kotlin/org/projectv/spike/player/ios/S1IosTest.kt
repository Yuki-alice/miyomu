package org.projectv.spike.player.ios

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.openani.mediamp.avkit.AVKitMediampPlayerFactory
import org.openani.mediamp.playUri
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class S1IosTest {

    @Test
    fun mp4LocalFile() = runBlocking(Dispatchers.Default) {
        val player = AVKitMediampPlayerFactory().create(EmptyCoroutineContext)
        try {
            println("S1_IOS_BEFORE_PLAY $FILE_URL")
            val result = runCatching {
                withTimeout(15_000) {
                    player.playUri(FILE_URL, true, 0L)
                }
            }
            println("S1_IOS_AFTER_PLAY success=${result.isSuccess} error=${result.exceptionOrNull()}")

            if (result.isSuccess) {
                withTimeout(20_000) {
                    while (!player.state.value.isPlaying) {
                        println("S1_IOS_WAIT STATE=${player.state.value}")
                        delay(500)
                    }
                }
                delay(2_000)
                println(
                    "S1_IOS_PLAYING STATE=${player.state.value} " +
                        "POSITION=${player.currentPositionMillis.value}",
                )
            }
        } finally {
            runCatching { player.close() }
        }
    }

    private companion object {
        const val FILE_URL =
            "file:///Users/kurisu/project%20v/spikes/s1-player/fixtures/base.mp4"
    }
}

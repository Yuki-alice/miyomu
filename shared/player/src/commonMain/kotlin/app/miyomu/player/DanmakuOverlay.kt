package app.miyomu.player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import app.miyomu.danmaku.DanmakuState
import kotlinx.coroutines.isActive


/**
 * Stateless danmaku overlay: every frame derives the visible comments from the playback window
 * (`position - SCROLL_MS … position`), so there is no spawn bookkeeping and seeking "just works".
 * Scroll lanes are round-robin over the comments in flight; top/bottom comments hold in place
 * for [HOLD_MS]. Text is drawn through Compose's text measurer, which works on both the
 * ExoPlayer and MPV surfaces.
 */
@Composable
fun DanmakuOverlay(
    positionMillis: Long,
    isPlaying: Boolean,
    state: DanmakuState,
    modifier: Modifier = Modifier,
) {
    val track = state.track
    if (!state.enabled || track == null || track.comments.isEmpty()) return

    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rowPx = with(density) { (state.fontSizeSp * 1.35f).sp.toPx() }
    val topPad = with(density) { (state.fontSizeSp * 0.4f).sp.toPx() }
    val textStyle = remember(state.fontSizeSp, state.opacity) {
        TextStyle(
            fontSize = state.fontSizeSp.sp,
            color = Color.White,
            shadow = Shadow(Color.Black.copy(alpha = state.opacity), blurRadius = 8f),
        )
    }
    val laneCount = remember(state.fontSizeSp) { (state.fontSizeSp / 4f).toInt().coerceAtLeast(6) }

    // Frame clock keeps motion smooth between position flow emissions.
    val framePosition by produceState(positionMillis, positionMillis, isPlaying) {
        var last = positionMillis
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                if (isPlaying) {
                    last += (now - lastNanos) / 1_000_000
                }
                lastNanos = now
                value = last
            }
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val inFlight = track.commentsBetween(framePosition - SCROLL_MS, framePosition + LOOKAHEAD_MS)
        var laneCursor = 0

        for (comment in inFlight) {
            val color = if (comment.color == 0xFFFFFF) Color.White else Color(comment.color)
            val layout = textMeasurer.measure(
                AnnotatedString(comment.text),
                style = textStyle.copy(color = color),
            )
            val textWidth = layout.size.width.toFloat()
            val isHold = comment.mode >= 4
            if (isHold) {
                val age = framePosition - comment.timeMs
                if (age < 0 || age > HOLD_MS) continue
                val x = (w - textWidth) / 2f
                val y = if (comment.mode == 5) topPad else h - layout.size.height - topPad
                drawText(layout, topLeft = Offset(x, y))
            } else {
                val age = framePosition - comment.timeMs
                if (age < 0 || age > SCROLL_MS) continue
                val progress = age.toFloat() / SCROLL_MS
                val x = w - progress * (w + textWidth)
                val y = topPad + laneCursor * rowPx
                laneCursor = (laneCursor + 1) % laneCount
                if (y + layout.size.height < h) {
                    drawText(layout, topLeft = Offset(x, y))
                }
            }
        }
    }
}

private const val SCROLL_MS = 9_000L
private const val HOLD_MS = 4_000L
private const val LOOKAHEAD_MS = 100L

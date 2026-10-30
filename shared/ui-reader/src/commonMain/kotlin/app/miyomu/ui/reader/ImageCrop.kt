package app.miyomu.ui.reader

import androidx.compose.ui.graphics.toPixelMap

/**
 * Content bounds for margin cropping (Aniyomi's cropBorders). The scan samples pixels with a
 * step so a page costs a bounded number of predicate calls; the caller supplies the predicate so
 * the scan stays testable without a graphics stack.
 */
data class CropBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1

    /** True when the scan hit no content (blank page) — cropping must be skipped. */
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

/**
 * Finds the bounding box of non-margin pixels in a [width]×[height] image, sampling every
 * [step] pixels along both axes. Returns null when no content pixel is found.
 */
fun computeContentBounds(
    width: Int,
    height: Int,
    step: Int,
    hasContent: (x: Int, y: Int) -> Boolean,
): CropBounds? {
    if (width <= 0 || height <= 0 || step <= 0) return null

    fun rowHasContent(y: Int): Boolean {
        var x = 0
        while (x < width) {
            if (hasContent(x, y)) return true
            x += step
        }
        return false
    }

    fun columnHasContent(x: Int): Boolean {
        var y = 0
        while (y < height) {
            if (hasContent(x, y)) return true
            y += step
        }
        return false
    }

    var top = -1
    var bottom = -1
    var y = 0
    while (y < height) {
        if (rowHasContent(y)) {
            top = y
            break
        }
        y += step
    }
    if (top == -1) return null
    y = height - 1
    while (y >= top) {
        if (rowHasContent(y)) {
            bottom = y
            break
        }
        y -= step
    }

    var left = -1
    var right = -1
    var x = 0
    while (x < width) {
        if (columnHasContent(x)) {
            left = x
            break
        }
        x += step
    }
    x = width - 1
    while (x >= left) {
        if (columnHasContent(x)) {
            right = x
            break
        }
        x -= step
    }

    return CropBounds(left = left, top = top, right = right, bottom = bottom)
}

/** Near-white margin predicate used against a sampled pixel map. */
fun isMarginPixel(r: Int, g: Int, b: Int, threshold: Int = 245): Boolean =
    r >= threshold && g >= threshold && b >= threshold

/**
 * Scans a platform-sampled decode for the content bounding box and maps it back to the full
 * bitmap's coordinates, inflating one sample cell per edge so stepped scanning never crops
 * content. Returns null for blank pages.
 */
suspend fun computeSampledContentBounds(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    sampled: androidx.compose.ui.graphics.ImageBitmap,
    whiteThreshold: Int = 245,
): CropBounds? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
    val fullW = bitmap.width
    val fullH = bitmap.height
    val sampleW = sampled.width
    val sampleH = sampled.height
    if (fullW <= 0 || fullH <= 0 || sampleW <= 0 || sampleH <= 0) return@withContext null

    val pixels = sampled.toPixelMap()
    val step = if (sampleW > 160 || sampleH > 160) 2 else 1
    val bounds = computeContentBounds(sampleW, sampleH, step) { x, y ->
        val pixel = pixels[x, y]
        !isMarginPixel(
            (pixel.red * 255f).toInt(),
            (pixel.green * 255f).toInt(),
            (pixel.blue * 255f).toInt(),
            whiteThreshold,
        )
    } ?: return@withContext null

    val cellW = fullW.toFloat() / sampleW
    val cellH = fullH.toFloat() / sampleH
    CropBounds(
        left = ((bounds.left - 1).coerceAtLeast(0) * cellW).toInt().coerceIn(0, fullW - 1),
        top = ((bounds.top - 1).coerceAtLeast(0) * cellH).toInt().coerceIn(0, fullH - 1),
        right = ((bounds.right + 1).coerceAtMost(sampleW - 1) * cellW).toInt().coerceIn(0, fullW - 1),
        bottom = ((bounds.bottom + 1).coerceAtMost(sampleH - 1) * cellH).toInt().coerceIn(0, fullH - 1),
    )
}

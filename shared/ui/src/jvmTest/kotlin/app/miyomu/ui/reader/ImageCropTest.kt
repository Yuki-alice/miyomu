package app.miyomu.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertTrue

class ImageCropTest {

    /** 10×10 grid; content lives at x in 3..6, y in 2..7. */
    private val width = 10
    private val height = 10
    private fun hasContent(x: Int, y: Int): Boolean = x in 3..6 && y in 2..7

    @Test
    fun findsContentBoundingBox() {
        val bounds = computeContentBounds(width, height, 1, ::hasContent)!!
        assertEquals(3, bounds.left)
        assertEquals(2, bounds.top)
        assertEquals(6, bounds.right)
        assertEquals(7, bounds.bottom)
        assertEquals(4, bounds.width)
        assertEquals(6, bounds.height)
    }

    @Test
    fun stepSamplingStaysInsideContent() {
        val bounds = computeContentBounds(width, height, 2, ::hasContent)!!
        assertTrue(bounds.left in 3..4)
        assertEquals(2, bounds.top)
        assertTrue(bounds.right in 5..6)
        assertTrue(bounds.bottom in 6..7)
    }

    @Test
    fun blankPageReturnsNull() {
        assertNull(computeContentBounds(width, height, 1) { _, _ -> false })
    }

    @Test
    fun fullBleedPageCoversEverything() {
        val bounds = computeContentBounds(width, height, 1) { _, _ -> true }!!
        assertEquals(0, bounds.left)
        assertEquals(0, bounds.top)
        assertEquals(width - 1, bounds.right)
        assertEquals(height - 1, bounds.bottom)
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(computeContentBounds(0, 10, 1, ::hasContent))
        assertNull(computeContentBounds(10, 10, 0, ::hasContent))
    }

    @Test
    fun marginPredicateMatchesNearWhite() {
        assertEquals(true, isMarginPixel(255, 255, 255))
        assertEquals(true, isMarginPixel(246, 250, 245))
        assertEquals(false, isMarginPixel(244, 255, 255))
    }

    @Test
    fun isEmptyGuardsBlankResults() {
        val blank = CropBounds(left = 0, top = 0, right = -1, bottom = -1)
        assertEquals(true, blank.isEmpty)
        val real = CropBounds(left = 1, top = 1, right = 5, bottom = 5)
        assertEquals(false, real.isEmpty)
    }
}

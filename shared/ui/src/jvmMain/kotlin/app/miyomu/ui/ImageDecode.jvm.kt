package app.miyomu.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

// Desktop memory allows full decodes; the step scan bounds the analysis cost instead.
actual fun decodeImageBitmapSampled(bytes: ByteArray, maxDim: Int): ImageBitmap? =
    decodeImageBitmap(bytes)

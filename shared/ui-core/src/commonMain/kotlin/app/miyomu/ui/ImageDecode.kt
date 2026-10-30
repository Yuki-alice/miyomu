package app.miyomu.ui

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

/** Platform-sampled decode for margin-crop analysis; result is small (~[maxDim] px). */
expect fun decodeImageBitmapSampled(bytes: ByteArray, maxDim: Int = 240): ImageBitmap?

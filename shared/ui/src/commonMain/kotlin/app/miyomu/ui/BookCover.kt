package app.miyomu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalComicRepository

/**
 * Cover thumbnail for a local book.
 *
 * The library grid now goes through coil (see `app.miyomu.ui.library.LocalCover`), but the history
 * tab and the detail header still render covers directly from the repository.
 */
@Composable
fun BookCover(
    book: LocalBook,
    comics: LocalComicRepository,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(initialValue = null, book.id) {
        value = comics.cover(book)?.let(::decodeImageBitmap)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = book.title.take(1),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun LocalBookFormat.shortLabel(): String = when (this) {
    LocalBookFormat.DIRECTORY -> "目录"
    LocalBookFormat.ZIP -> "ZIP"
    LocalBookFormat.SEVEN_ZIP -> "7Z"
    LocalBookFormat.PDF -> "PDF"
    LocalBookFormat.EPUB -> "EPUB"
    LocalBookFormat.TXT -> "TXT"
    LocalBookFormat.FB2 -> "FB2"
    LocalBookFormat.HTML -> "HTML"
    LocalBookFormat.MARKDOWN -> "MD"
}

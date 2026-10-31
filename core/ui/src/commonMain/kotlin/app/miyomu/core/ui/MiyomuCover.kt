package app.miyomu.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun MiyomuCover(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    if (url.isNullOrBlank()) {
        CoverFallback(title, modifier.clip(shape))
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
            loading = { CoverFallback(title, Modifier.fillMaxSize()) },
            error = { CoverFallback(title, Modifier.fillMaxSize()) },
        )
    }
}

@Composable
private fun CoverFallback(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.take(2).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp),
        )
    }
}

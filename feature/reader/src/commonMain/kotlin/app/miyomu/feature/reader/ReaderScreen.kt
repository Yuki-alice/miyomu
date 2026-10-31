package app.miyomu.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary

@Composable
fun ReaderScreen(
    repository: ContentRepository,
    libraryRepository: LibraryRepository,
    contentId: String,
    chapterId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pages by remember { mutableStateOf<List<PageSummary>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(contentId, chapterId) {
        when (
            val result = repository.pages(
                contentId = ContentId(contentId.toLongOrNull() ?: -1L),
                chapterId = ChapterId(chapterId.toLongOrNull() ?: -1L),
            )
        ) {
            is Outcome.Success -> pages = result.value
            is Outcome.Failure -> failed = true
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState.currentPage, pages.size) {
        if (pages.isNotEmpty()) {
            libraryRepository.saveProgress(
                contentId = ContentId(contentId.toLongOrNull() ?: -1L),
                chapterId = ChapterId(chapterId.toLongOrNull() ?: -1L),
                chapterTitle = "Chapter",
                page = pagerState.currentPage,
                scroll = 0f,
                percent = (pagerState.currentPage + 1).toFloat() / pages.size,
            )
        }
    }

    Column(modifier.fillMaxSize().background(Color(0xFF101014))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = Color(0xFFE7E2EA)) }
            Text(
                if (pages.isEmpty()) "Reader" else "Page ${pagerState.currentPage + 1} / ${pages.size}",
                color = Color(0xFFE7E2EA),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        when {
            failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pages not found", color = Color(0xFFE7E2EA))
            }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading pages…", color = Color(0xFFE7E2EA))
            }
            else -> HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            ) { page ->
                ReaderPage(page = pages[page])
            }
        }
    }
}

@Composable
private fun ReaderPage(page: PageSummary) {
    Surface(
        modifier = Modifier.fillMaxSize().padding(bottom = 16.dp).clip(RoundedCornerShape(20.dp)),
        color = Color(0xFFF6F3EC),
    ) {
        Box(
            Modifier.fillMaxSize().padding(18.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF6F3EC), Color(0xFFE8E2D6)),
                    ),
                    RoundedCornerShape(14.dp),
                ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "DEMO PAGE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8A8378),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Page ${page.index + 1}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF191816),
                )
                Text(
                    page.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5A52),
                )
            }
        }
    }
}

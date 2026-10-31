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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.core.model.PageSummary
import app.miyomu.core.ui.MiyomuImage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReaderScreen(
    contentId: String,
    chapterId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReaderViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(contentId, chapterId) {
        viewModel.load(contentId, chapterId)
    }

    var currentPage by remember { mutableStateOf(state.initialPage) }
    LaunchedEffect(state.initialPage) {
        currentPage = state.initialPage
    }

    Column(modifier.fillMaxSize().background(Color(0xFF101014))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = Color(0xFFE7E2EA)) }
            Text(
                if (state.pages.isEmpty()) "Reader" else "Page ${currentPage + 1} / ${state.pages.size}",
                color = Color(0xFFE7E2EA),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        when {
            state.failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pages not found", color = Color(0xFFE7E2EA))
            }
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading pages…", color = Color(0xFFE7E2EA))
            }
            state.pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pages", color = Color(0xFFE7E2EA))
            }
            else -> key(state.initialPage, state.pages.size) {
                val pagerState = rememberPagerState(
                    initialPage = state.initialPage,
                    pageCount = { state.pages.size },
                )
                LaunchedEffect(pagerState.currentPage) {
                    currentPage = pagerState.currentPage
                    viewModel.saveProgress(
                        contentId = contentId,
                        chapterId = chapterId,
                        page = pagerState.currentPage,
                        percent = (pagerState.currentPage + 1).toFloat() / state.pages.size,
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                ) { page ->
                    ReaderPage(page = state.pages[page])
                }
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
        MiyomuImage(
            url = page.url,
            contentDescription = "Page ${page.index + 1}",
            modifier = Modifier.fillMaxSize().padding(8.dp),
        )
    }
}

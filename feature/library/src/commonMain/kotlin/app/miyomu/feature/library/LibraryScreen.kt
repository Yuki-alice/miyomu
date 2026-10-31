package app.miyomu.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class DemoCard(val title: String, val by: String, val progress: Float, val colors: List<Color>)

private val demos = listOf(
    DemoCard("Sky Archive", "Mira Okonkwo", .62f, listOf(Color(0xFF264653), Color(0xFF2A9D8F))),
    DemoCard("Northbound Owl", "Signe Aalto", .48f, listOf(Color(0xFF1D3557), Color(0xFF457B9D))),
    DemoCard("Static Bloom", "Kira Moreau", .81f, listOf(Color(0xFF5F0F40), Color(0xFF9A031E))),
    DemoCard("Moth & Meteor", "Juno Adeyemi", 0f, listOf(Color(0xFF3A0CA3), Color(0xFF4361EE))),
    DemoCard("Cloud Kitchen", "Priya Sandoval", .34f, listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))),
    DemoCard("Neon Monks", "Asha Varga", 0f, listOf(Color(0xFF240046), Color(0xFF5A189A))),
)

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Good evening", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "12 new chapters · 128 titles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = {}) { Text("Search") }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Continue reading", style = MaterialTheme.typography.labelLarge)
                Text("Sky Archive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Chapter 80 · The library that drifts", style = MaterialTheme.typography.bodyMedium)
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = .22f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(.62f).height(4.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(99.dp)),
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Continue") }
            }
        }

        Text("Jump back in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(demos) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(item.colors)),
                    ) {
                        Text(
                            item.title.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(item.by, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(item.progress).height(3.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) = LibraryScreen(modifier)

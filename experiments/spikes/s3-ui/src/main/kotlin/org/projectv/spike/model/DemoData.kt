package org.projectv.spike.model

import androidx.compose.ui.graphics.Color

data class DemoComic(
    val id: Int,
    val title: String,
    val author: String,
    val tags: List<String>,
    val chapters: Int,
    val accent: Color,
)

val demoComics = listOf(
    DemoComic(1, "Aster Bloom", "Mika Sato", listOf("Fantasy", "Adventure"), 128, Color(0xFF5B8DEF)),
    DemoComic(2, "Night Cartographer", "Ren Ito", listOf("Mystery", "Drama"), 64, Color(0xFF7A5AF8)),
    DemoComic(3, "Paper Lanterns", "Hana Mori", listOf("Slice of Life"), 42, Color(0xFFE07A5F)),
    DemoComic(4, "Static Garden", "Kai Nakamura", listOf("Sci-Fi"), 96, Color(0xFF3D9B8F)),
    DemoComic(5, "The Long Orbit", "Aya Kondo", listOf("Sci-Fi", "Romance"), 210, Color(0xFF4C6FFF)),
    DemoComic(6, "Cobalt Season", "Jun Park", listOf("Sports", "Drama"), 75, Color(0xFF2563EB)),
    DemoComic(7, "Quiet Machinery", "Sora Fujita", listOf("Mecha"), 55, Color(0xFF64748B)),
    DemoComic(8, "Salt & Ember", "Mei Watanabe", listOf("Historical"), 88, Color(0xFFB45309)),
    DemoComic(9, "Low Tide City", "Riku Tanaka", listOf("Urban", "Mystery"), 34, Color(0xFF0F766E)),
    DemoComic(10, "Glass Meridian", "Nao Shimizu", listOf("Fantasy"), 140, Color(0xFF9333EA)),
    DemoComic(11, "Winter Broadcast", "Yui Kobayashi", listOf("Drama"), 26, Color(0xFF475569)),
    DemoComic(12, "Afterimage Club", "Haru Endo", listOf("School", "Supernatural"), 112, Color(0xFFDB2777)),
)

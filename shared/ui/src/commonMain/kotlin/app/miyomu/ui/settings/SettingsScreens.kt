package app.miyomu.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.IconItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SliderItem

/**
 * Settings entry list (Aniyomi's More > Settings). The reader screen binds the ported
 * ReaderPreferences for real; appearance follows once the theme engine lands.
 */
class SettingsMainRoute(private val graph: MiyomuGraph) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    SettingsEntry(Icons.Outlined.Palette, "外观") {
                        navigator.push(SettingsAppearanceRoute(graph))
                    }
                }
                item {
                    SettingsEntry(Icons.AutoMirrored.Outlined.MenuBook, "阅读") {
                        navigator.push(SettingsReaderRoute(graph))
                    }
                }
                item {
                    SettingsEntry(Icons.Outlined.CloudSync, "备份与同步") {
                        navigator.push(app.miyomu.ui.backup.BackupSyncRoute(graph))
                    }
                }
                item {
                    SettingsEntry(Icons.Outlined.Info, "弹幕") {
                        navigator.push(DanmakuSettingsRoute(graph))
                    }
                }
                item {
                    SettingsEntry(Icons.Outlined.Info, "关于 Miyomu") {
                        navigator.push(AboutRoute())
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsEntry(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconItem(
        label = label,
        icon = icon,
        onClick = onClick,
    )
}

/** Reader settings bound to the ported [ReaderPreferences] (Aniyomi's SettingsReaderScreen). */
class SettingsReaderRoute(private val graph: MiyomuGraph) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val prefs = remember(graph) { graph.readerPreferences }

        val modeFlagState = remember(prefs) { prefs.defaultReadingMode().asState(scope) }
        val orientationFlagState = remember(prefs) { prefs.defaultOrientationType().asState(scope) }
        val themeFlagState = remember(prefs) { prefs.readerTheme().asState(scope) }
        val cropPagedState = remember(prefs) { prefs.cropBorders().asState(scope) }
        val cropWebtoonState = remember(prefs) { prefs.cropBordersWebtoon().asState(scope) }
        val webtoonPaddingState = remember(prefs) { prefs.webtoonSidePadding().asState(scope) }
        val transitionsState = remember(prefs) { prefs.pageTransitions().asState(scope) }
        val flashState = remember(prefs) { prefs.flashOnPageChange().asState(scope) }
        val preservePositionState = remember(prefs) { prefs.preserveReadingPosition().asState(scope) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("阅读") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { HeadingItem("默认") }
                item {
                    val current = ReadingMode.fromPreference(modeFlagState.value)
                    ReadingMode.entries
                        .filter { it != ReadingMode.DEFAULT }
                        .forEach { mode ->
                            RadioItem(
                                label = readingModeLabel(mode),
                                selected = current == mode,
                                onClick = { prefs.defaultReadingMode().set(mode.flagValue) },
                            )
                        }
                }
                item {
                    HeadingItem("默认方向")
                    val current = ReaderOrientation.fromPreference(orientationFlagState.value)
                    ReaderOrientation.entries
                        .filter { it != ReaderOrientation.DEFAULT }
                        .forEach { orientation ->
                            RadioItem(
                                label = orientationLabel(orientation),
                                selected = current == orientation,
                                onClick = { prefs.defaultOrientationType().set(orientation.flagValue) },
                            )
                        }
                }
                item {
                    HeadingItem("显示")
                    val themes = listOf("自动（跟随系统）", "黑色", "灰色", "白色")
                    themes.forEachIndexed { index, label ->
                        RadioItem(
                            label = label,
                            selected = themeFlagState.value == index,
                            onClick = { prefs.readerTheme().set(index) },
                        )
                    }
                }
                item {
                    CheckboxItem(
                        label = "裁剪页面空白边（翻页模式）",
                        pref = prefs.cropBorders(),
                    )
                }
                item {
                    CheckboxItem(
                        label = "裁剪页面空白边（Webtoon）",
                        pref = prefs.cropBordersWebtoon(),
                    )
                }
                item {
                    SliderItem(
                        value = webtoonPaddingState.value,
                        valueRange = 0..25,
                        label = "Webtoon 边距（%）",
                        onChange = { prefs.webtoonSidePadding().set(it) },
                        valueText = "${webtoonPaddingState.value}%",
                    )
                }
                item { HeadingItem("行为") }
                item {
                    CheckboxItem(
                        label = "页面切换动画",
                        pref = prefs.pageTransitions(),
                    )
                }
                item {
                    CheckboxItem(
                        label = "翻页时闪光提示",
                        pref = prefs.flashOnPageChange(),
                    )
                }
                item {
                    CheckboxItem(
                        label = "保留阅读位置",
                        pref = prefs.preserveReadingPosition(),
                    )
                }
            }
        }
    }
}

private fun readingModeLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.LEFT_TO_RIGHT -> "从左到右"
    ReadingMode.RIGHT_TO_LEFT -> "从右到左（日漫）"
    ReadingMode.VERTICAL -> "纵向分页"
    ReadingMode.WEBTOON -> "Webtoon（长条）"
    else -> mode.name
}

private fun orientationLabel(orientation: ReaderOrientation): String = when (orientation) {
    ReaderOrientation.FREE -> "自由"
    ReaderOrientation.PORTRAIT -> "竖屏"
    ReaderOrientation.LANDSCAPE -> "横屏"
    ReaderOrientation.LOCKED_PORTRAIT -> "锁定竖屏"
    ReaderOrientation.LOCKED_LANDSCAPE -> "锁定横屏"
    else -> orientation.name
}

/** Minimal about screen; content mirrors the README status block. */
class AboutRoute : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("关于 Miyomu") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Text("Miyomu 0.1.0", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    Text(
                        "跨平台漫画/视频阅读器，以 Aniyomi 体验为基准的 KMP 重实现。" +
                            "Apache-2.0；移植来源见 NOTICE。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


/** Appearance settings: theme mode, app theme (Aniyomi keys) and the AMOLED switch. */
class SettingsAppearanceRoute(private val graph: MiyomuGraph) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val modeState = remember(graph) {
            graph.preferenceStore.getString("pref_theme_mode_key", ThemeMode.SYSTEM.name).asState(scope)
        }
        val themeState = remember(graph) {
            graph.preferenceStore.getString("pref_app_theme", "").asState(scope)
        }
        val amoledState = remember(graph) {
            graph.preferenceStore.getBoolean("pref_theme_dark_amoled_key", false).asState(scope)
        }
        val currentMode = ThemeMode.fromStored(modeState.value)
        val currentTheme = AppTheme.fromStored(themeState.value, currentMode)

        fun setMode(mode: ThemeMode) {
            graph.preferenceStore.getString("pref_theme_mode_key", ThemeMode.SYSTEM.name).set(mode.name)
            modeState.value = mode.name
        }

        fun setTheme(theme: AppTheme) {
            graph.preferenceStore.getString("pref_app_theme", "").set(theme.name)
            themeState.value = theme.name
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("外观") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { HeadingItem("主题模式") }
                item {
                    RadioItem("跟随系统", selected = currentMode == ThemeMode.SYSTEM) { setMode(ThemeMode.SYSTEM) }
                }
                item {
                    RadioItem("亮色", selected = currentMode == ThemeMode.LIGHT) { setMode(ThemeMode.LIGHT) }
                }
                item {
                    RadioItem("暗色", selected = currentMode == ThemeMode.DARK) { setMode(ThemeMode.DARK) }
                }
                if (currentMode != ThemeMode.LIGHT) {
                    item {
                        CheckboxItem(
                            label = "纯黑（AMOLED）",
                            checked = amoledState.value,
                            onClick = {
                                graph.preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)
                                    .set(!amoledState.value)
                            },
                        )
                    }
                }
                item { HeadingItem("主题风格") }
                items(AppTheme.entries.size) { index ->
                    val theme = AppTheme.entries[index]
                    val label = if (theme == AppTheme.MONET) "Monet（动态取色，暂用默认色）" else themeLabel(theme)
                    RadioItem(
                        label = label,
                        selected = currentTheme == theme,
                        onClick = { setTheme(theme) },
                    )
                }
            }
        }
    }
}

private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.DEFAULT -> "默认"
    AppTheme.CLOUDFLARE -> "Cloudflare"
    AppTheme.COTTONCANDY -> "棉花糖"
    AppTheme.DOOM -> "末日"
    AppTheme.GREEN_APPLE -> "青苹果"
    AppTheme.LAVENDER -> "薰衣草"
    AppTheme.MATRIX -> "矩阵"
    AppTheme.MIDNIGHT_DUSK -> "午夜薄暮"
    AppTheme.MOCHA -> "摩卡"
    AppTheme.MONOCHROME -> "单色"
    AppTheme.NORD -> "Nord"
    AppTheme.SAPPHIRE -> "蓝宝石"
    AppTheme.STRAWBERRY_DAIQUIRI -> "草莓戴克利"
    AppTheme.TAKO -> "章鱼"
    AppTheme.TEALTURQUOISE -> "青绿松石"
    AppTheme.TIDAL_WAVE -> "浪潮"
    AppTheme.YINYANG -> "阴阳"
    AppTheme.YOTSUBA -> "四叶"
    AppTheme.MONET -> "Monet"
}

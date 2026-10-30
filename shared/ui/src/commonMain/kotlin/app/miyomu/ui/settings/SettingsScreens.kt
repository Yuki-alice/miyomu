package app.miyomu.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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


/** Appearance settings: theme mode (Aniyomi's `pref_theme_mode_key` semantics). */
class SettingsAppearanceRoute(private val graph: MiyomuGraph) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val prefs = remember(graph) { graph.preferenceStore.getInt("pref_theme_mode_key", 0) }
        val modeState = remember(prefs) { prefs.asState(scope) }
        val darkStylePrefs = remember(graph) { graph.preferenceStore.getInt("app_dark_theme_key", 0) }
        val darkStyleState = remember(darkStylePrefs) { darkStylePrefs.asState(scope) }

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
                    RadioItem(
                        label = "跟随系统",
                        selected = modeState.value == 0,
                        onClick = { prefs.set(0) },
                    )
                }
                item {
                    RadioItem(
                        label = "亮色",
                        selected = modeState.value == 1,
                        onClick = { prefs.set(1) },
                    )
                }
                item {
                    RadioItem(
                        label = "暗色",
                        selected = modeState.value == 2,
                        onClick = { prefs.set(2) },
                    )
                }
                item {
                    HeadingItem("暗色主题风格（暗色模式下生效）")
                    val styles = listOf("默认", "青苹果", "午夜薄暮", "薰衣草", "草莓")
                    styles.forEachIndexed { index, label ->
                        RadioItem(
                            label = label,
                            selected = darkStyleState.value == index,
                            onClick = { darkStylePrefs.set(index) },
                        )
                    }
                }
            }
        }
    }
}

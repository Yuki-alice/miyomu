# 18 · ViewModel + Koin

日期：2026-10-31
状态：四个主 ViewModel + Koin 装配已完成

## 1. 目标

把页面里的 `remember + LaunchedEffect + repository` 收进 ViewModel，并用 Koin 统一装配：

- UI 只消费 `StateFlow<UiState>`；
- Repository 不进入 Composable；
- 页面切换后状态由 ViewModel 持有；
- 测试可替换 Repository。

## 2. Koin 模块

`app:shared/di/AppModule.kt`：

```kotlin
fun appModule(database: MiyomuDatabase): Module = module {
    single<LibraryRepository> { DefaultLibraryRepository(database) }
    single<ContentRepository> {
        DefaultContentRepository(SourceRegistry(listOf(MangaDexSourceRuntime())))
    }

    viewModel { DiscoverViewModel(get()) }
    viewModel { DetailsViewModel(get(), get()) }
    viewModel { ReaderViewModel(get(), get()) }
    viewModel { LibraryViewModel(get()) }
}
```

入口：

```kotlin
@Composable
fun MiyomuApp(database: MiyomuDatabase) {
    KoinApplication(application = { modules(appModule(database)) }) {
        MiyomuTheme { ... }
    }
}
```

> 已迁移到 `KoinApplication(configuration = koinConfiguration { ... }, logLevel = Level.INFO)`。

## 3. ViewModels

| ViewModel | 状态 | 职责 |
| --- | --- | --- |
| `DiscoverViewModel` | items/loading/error | 调 `ContentRepository.browse()` |
| `DetailsViewModel` | details/loading/failed/isFavourite | 详情 + 收藏 |
| `ReaderViewModel` | pages/initialPage/loading/failed | 页面 + 进度恢复 |
| `LibraryViewModel` | favourites | 收藏 Flow |

### Discover

```kotlin
val state by viewModel.state.collectAsState()
LaunchedEffect(Unit) { viewModel.refresh() }
```

### Details

```kotlin
LaunchedEffect(contentId) { viewModel.load(contentId) }
TextButton(onClick = viewModel::toggleFavourite) { ... }
```

### Reader

```kotlin
LaunchedEffect(contentId, chapterId) { viewModel.load(contentId, chapterId) }
val pagerState = rememberPagerState(
    initialPage = state.initialPage,
    pageCount = { state.pages.size },
)
LaunchedEffect(pagerState.currentPage) {
    viewModel.saveProgress(contentId, chapterId, pagerState.currentPage, percent)
}
```

### Library

```kotlin
val favourites by viewModel.favourites.collectAsState()
```

## 4. Reader 进度恢复

`ReaderViewModel.load()`：

1. 拉页面；
2. `libraryRepository.observeProgress(contentId).first()`；
3. 只有进度章节等于当前章节时，才把 `initialPage` 设为保存的页码；
4. 页面用 `key(initialPage, pages.size)` 创建 Pager，避免 `rememberPagerState` 复用旧页。

## 5. 收藏闭环

- Details `☆ Save` → `LibraryRepository.setFavourite()`；
- Room `favourite` 表；
- LibraryViewModel 的 `observeFavourites()` 驱动 Library 页；
- 重启后收藏仍在。

## 6. 依赖

feature 模块新增：

```kotlin
implementation(libs.lifecycle.viewmodel)
implementation(libs.koin.viewmodel)
```

## 7. 限制

- ViewModel 通过 `load(contentId)` 接收参数，而不是 `SavedStateHandle`；
- 已有 Reader/Library ViewModel 单测，但 Details/Discover 还没有。
- `LibraryViewModel` 只用 `WhileSubscribed`，切页会重订阅；
- Koin 旧 API 的 deprecation warning；
- 页面还有少量 `LaunchedEffect`（加载/保存副作用），但状态在 ViewModel。

## 8. 下一步

1. 用 `SavedStateHandle` 接 nav 参数；
2. ViewModel 单测（fake repositories + Turbine）；
3. 迁移 Koin 新 `KoinConfiguration` API；
4. 收藏页支持移除/批量；
5. 详情页收藏状态改为一对一 Flow，避免全表扫描。

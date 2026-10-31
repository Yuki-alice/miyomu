# 14 · 真实数据流：Fixture → Repository → UI

日期：2026-10-31
状态：第一条数据链路已打通

## 1. 目标

从“核心代码能编译”推进到“真实数据能流动”：

```text
DemoSourceRuntime (SourceRuntime)
        ↓
SourceRegistry
        ↓
DefaultContentRepository
        ↓
DiscoverScreen / DetailsScreen
```

没有真实网络、没有 Room 持久化，但数据已经走完整的 SourceRuntime → Repository → UI 链路。

## 2. 组成

### 2.1 DemoSourceRuntime

位置：`core:data/demo/DemoSourceRuntime.kt`

实现 Kototoro 的 `SourceRuntime`：

- `getSources()`：一个虚拟源 `demo`；
- `getList()`：8 部虚构作品，支持 offset/query；
- `getDetails()`：每部 5 章；
- `getPages()`：每章 3 页；
- `getRelated()`：3 部相关作品；
- 其余能力走默认实现。

数据全部本地、确定性，不依赖网络。

### 2.2 SourceRegistry

位置：`core:source/SourceRegistry.kt`

- 持有多个 `SourceRuntime`；
- `sources()` 聚合去重；
- `primary()` 当前返回第一个 runtime；
- `runtime(name)` 按源名查找。

这是一个最小门面，后续替换为持久化源列表 + 扩展源注册表。

### 2.3 ContentRepository

位置：`core:data/ContentRepository.kt`、`DefaultContentRepository.kt`

```kotlin
interface ContentRepository {
    suspend fun browse(offset: Int = 0, limit: Int = 20): Outcome<List<ContentSummary>>
    suspend fun details(contentId: ContentId): Outcome<ContentDetails>
}
```

职责：

- 调 SourceRuntime；
- 把 `SourceContent`/`SourceChapter` 映射为 `ContentSummary`/`ChapterSummary`；
- 缓存内容，避免详情页重复翻列表；
- 把异常映射为 `AppError`。

### 2.4 UI

- `feature:discover/DiscoverScreen`：加载 fixture 列表，显示标题/作者/类型/评分；
- `feature:discover/DetailsScreen`：加载详情与章节，显示简介与 5 章列表；
- 从 Browse 点击一部作品 → `Details` 路由 → 返回。

## 3. 数据模型

新增：

```kotlin
data class ContentDetails(
    val summary: ContentSummary,
    val description: String?,
    val chapters: List<ChapterSummary>,
)

data class ChapterSummary(
    val id: ChapterId,
    val title: String,
    val number: Float,
    val uploadedAt: Long,
)
```

`AppError` 增加 `NotFound`，用于详情页找不到内容。

## 4. 测试

`core:data/src/desktopTest/.../ContentRepositoryTest.kt`：

```text
browseReturnsFixtureContent  ✅ 8 items
detailsReturnsChapters        ✅ 5 chapters
```

全量核心测试：

```text
reader    193
source     74
network   105
data        2
-------------
total     374
```

## 5. 构建验证

```bash
./gradlew :core:reader:desktopTest \
          :core:source:desktopTest \
          :core:network:desktopTest \
          :core:data:desktopTest

./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :app:shared:linkDebugFrameworkIosSimulatorArm64

xcodebuild -project iosApp/Miyomu.xcodeproj -scheme Miyomu \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO build
```

结果：Android / Desktop / iOS 全部通过。

## 6. 当前限制

- 数据只在内存，重启丢失；
- 没有真实源/网络；
- 没有图片加载，封面用渐变占位；
- 没有 ViewModel/Koin，页面用 `LaunchedEffect` + `remember`；
- 没有 Reader 路由；
- `core:data` 还没有接 Room。

## 7. 下一步

1. `core:backup` / `core:domain` 复制。
2. `core:data` 接 Room：收藏/历史/进度。
3. 用真实 JSON 源替换 DemoSourceRuntime。
4. 页面迁到 ViewModel + Koin。
5. ~~增加 `Details → Reader(chapterId)`~~ 已完成，见 [15-reader-route.md](15-reader-route.md)。
6. 图片管线：Coil + 请求头/Referer。

## 8. 结论

这是第一次真正的纵向数据流动：

- 上层 UI 不认识 SourceRuntime；
- Repository 不认识 Compose；
- 源可以被替换，而不改页面；
- 页面可以被替换，而不改源。

下一步才是“真源”和“持久化”，而不是继续加占位页面。

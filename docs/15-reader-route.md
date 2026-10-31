# 15 · Reader 路由与分页演示

日期：2026-10-31
状态：Details → Reader 已打通（fixture 页面）

## 1. 链路

```text
Browse
  → Details(contentId)
    → Reader(contentId, chapterId)
      → ContentRepository.pages()
        → DemoSourceRuntime.getPages()
          → 3 个 PageSummary
            → HorizontalPager
```

## 2. 新增模型

```kotlin
data class PageSummary(
    val id: Long,
    val index: Int,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
```

`ContentRepository` 增加：

```kotlin
suspend fun pages(contentId: ContentId, chapterId: ChapterId): Outcome<List<PageSummary>>
```

Repository 会：

1. 从缓存/详情中找 `SourceChapter`；
2. 调 `SourceRuntime.getPages()`；
3. 映射为 `PageSummary`；
4. 找不到内容/章节时返回 `AppError.NotFound`。

## 3. 路由

`MiyomuRoute.Reader`：

```kotlin
data class Reader(val contentId: String, val chapterId: String) : MiyomuRoute {
    override val key = "reader/$contentId/$chapterId"
}
```

`NavHost` 使用：

```text
reader/{contentId}/{chapterId}
```

两个参数都从 KMP `SavedState` 读取：

```kotlin
entry.arguments?.read { getString("contentId") }
```

## 4. UI

- `DetailsScreen` 的章节行可点击，跳转 Reader；
- `ReaderScreen` 使用 `HorizontalPager`；
- 顶部有返回和页码；
- 页面是 fixture 占位（渐变 + 页码 + URL），不是真实图片；
- 阅读方向/缩放/预取/进度持久化尚未接。

## 5. 测试

`ContentRepositoryTest.pagesReturnsFixturePages`：

```text
pages size = 3
first index = 0
url contains example.com/page
```

核心测试总数：

```text
reader    193
source     74
network   105
data        3
-------------
total     375
```

## 6. 构建

```bash
./gradlew :core:data:desktopTest
./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :app:shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/Miyomu.xcodeproj -scheme Miyomu \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO build
```

Android / Desktop / iOS 全部通过。

## 7. 下一步

1. 把 fixture 页面换成 Coil 图片（headers/Referer）。
2. 用 `reader-core` 的 `PagedReaderScene` 替代简单 pager。
3. 保存阅读进度（Room）。
4. 章节切换/跨章；
5. 阅读设置面板（MD3E）。

## 8. 结论

阅读闭环现在有了最小的“从详情点章节到看到页面”的路径。

它还不是阅读器，但已经把 Reader 路由、Repository、分页 UI 和数据源接在了一起；后续接 `reader-core` 和图片管线时不需要再改导航/数据层结构。

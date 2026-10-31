# 17 · 真实 JSON 源与 Coil 图片管线

日期：2026-10-31
状态：MangaDex JSON 源 + Coil 封面/页面已接入

## 1. 真实源：MangaDex

`core:source/MangaDexSourceRuntime.kt` 实现 Kototoro 的 `SourceRuntime`：

| 操作 | MangaDex API |
| --- | --- |
| 列表/搜索 | `GET /manga?limit=20&offset=…&includes[]=cover_art&includes[]=author` |
| 详情 | `GET /manga/{id}` |
| 章节 | `GET /manga/{id}/feed?translatedLanguage[]=en` |
| 页面 | `GET /at-home/server/{chapterId}` |
| 封面 | `https://uploads.mangadex.org/covers/{mangaId}/{fileName}.512.jpg` |

实现要点：

- 默认 `HttpClient()` 使用平台引擎：Android/Desktop OkHttp，iOS Darwin；
- 默认 UA：`Miyomu/0.1`；
- MangaDex 的 UUID 映射为稳定的 64 位 hash（`SourceContent.id` 仍是 Long）；
- `mangaIds` / `chapterIds` 在 runtime 内缓存，支持详情/章节/页面；
- 分页使用 `OFFSET`；
- 搜索通过 `title` 参数；
- 排序：`followedCount` / `latestUploadedChapter` / `relevance`。

`DemoSourceRuntime` 保留为确定性测试 fixture，不再作为 App 默认源。

## 2. Coil 图片

新增 `core:ui` 组件：

```kotlin
MiyomuCover(url, title, modifier)   // 封面，失败回退渐变
MiyomuImage(url, contentDescription, modifier) // 阅读页面
```

实现：

- `SubcomposeAsyncImage`；
- loading/error 状态；
- 封面 `ContentScale.Crop`，页面 `ContentScale.Fit`；
- Android/Desktop 使用 `coil-network-okhttp`；
- iOS 使用 `coil-network-ktor3` + Ktor Darwin；
- 封面 URL 来自 MangaDex；页面 URL 来自 at-home API；
- 图片失败时保留渐变/占位，不阻塞页面。

## 3. App 接线

`appModule`：

```kotlin
single<ContentRepository> {
    DefaultContentRepository(
        SourceRegistry(listOf(MangaDexSourceRuntime())),
    )
}
```

- Browse 列表来自 MangaDex；
- 详情/章节来自 MangaDex；
- Reader 页面 URL 来自 at-home API；
- 封面/页面通过 Coil 加载。

## 4. 测试

`core:source` 新增 `MangaDexSourceRuntimeTest`：

- 用 Ktor `MockEngine` 返回一段 MangaDex JSON；
- 验证标题、作者、封面 URL、稳定 ID。

核心测试总数：

```text
reader    193
source     75
network   105
data        5
-------------
total     378
```

## 5. 构建

```bash
./gradlew :core:source:desktopTest
./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :app:shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/Miyomu.xcodeproj -scheme Miyomu \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO build
```

## 6. 限制

- 目前真实源只有 MangaDex，另有 Demo fixture 源；Browse 支持源切换与搜索。
- 没有 MangaDex 的标签/年份/状态筛选 UI；
- 没有 related、外链章节、scanlation group 选择；
- ID 映射是 runtime 内存缓存，进程重启后需要重新拉列表；
- API 可能有速率限制/地区限制；
- ~~页面请求头尚未传入 Coil~~ 已通过 `MiyomuImage(headers=...)` 传入。
- 还没有失败重试/诊断面板。

## 7. 下一步

1. 源管理页：多源切换、启用/禁用；
2. 把 DemoSourceRuntime 作为“示例源”保留在开发模式；
3. 真实源的筛选/排序 UI；
4. 错误分类 + 重试；
5. 图片请求头/Referer；
6. 换成 `reader-core` 的 PagedReaderScene。

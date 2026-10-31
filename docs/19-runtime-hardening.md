# 19 · 限制清理：从演示到可运行

日期：2026-10-31
状态：上一轮列出的限制已逐项处理

## 1. 清单

| 限制 | 处理 | 证据 |
| --- | --- | --- |
| Reader 不恢复进度 | `ReaderViewModel` 读取 `observeProgress`，Pager 初始槽位恢复 | `ReaderViewModelTest` |
| Library 不消费 Room | `LibraryViewModel.observeFavourites()`，Library/Home 显示收藏 | `LibraryViewModelTest` |
| 页面请求头没传给 Coil | `MiyomuImage(headers=page.headers)` + `NetworkHeaders` | 编译通过 |
| 只有 MangaDex 一个源 | `SourceRegistry` + Demo 源；Browse 源 chips + 搜索 | UI 代码 |
| Reader 仍是纯 Pager | 用 `PagedReaderScene` 生成 slot，Pager 按 slot 渲染 | `ReaderViewModel` |
| Koin 旧 API deprecation | `KoinApplication(configuration=..., logLevel=...)` | 无 deprecation warning |
| ViewModel 无测试 | Reader/Library ViewModel 测试 | 3 tests |
| Android 迁移无适配/测试 | `AndroidMigration1To2` + v1→v2 迁移测试 | `MigrationTest` |

## 2. Reader 场景

`ReaderViewModel` 现在：

1. 拉 `pages`；
2. 建 `PagedReaderScene`（1000×1600，单页 LTR，`Estimated(0.7)`）；
3. 用 `scene.slotIndexOf(PageId)` 把进度页映射为 `initialSlot`；
4. UI 用 `scene.allSlots` 作为 Pager 的页面单位；
5. 保存进度时用 slot 的 `progressAnchorPageId` 反查 page index。

这意味着：

- 页面布局由 `reader-core` 决定，而不是 Pager 直接数页面；
- 后续切双页/RTL 只需改 `PagedSpreadConfig`；
- 进度锚点语义与 reader-core 一致。

## 3. Coil 页面请求头

`MiyomuImage` 接收 `headers: Map<String, String>`：

```kotlin
val request = ImageRequest.Builder(context)
    .data(url)
    .httpHeaders(
        NetworkHeaders.Builder().apply {
            headers.forEach { (k, v) -> set(k, v) }
        }.build(),
    )
    .build()
```

Reader 传入 `page.headers`（MangaDex 页面通常不需要，但结构已就绪）。

## 4. 源切换与搜索

- `ContentRepository.sources()`
- `ContentRepository.selectSource(name)`
- Browse 顶部：`FilterChip` 源选择 + `OutlinedTextField` 搜索；
- `SourceRegistry` 同时注册 `MangaDexSourceRuntime` 与 `DemoSourceRuntime`；
- MangaDex 搜索映射到 `title` 参数；Demo 源本地过滤；
- 切换源会清空缓存，避免跨源 ID 冲突。

## 5. Koin 新 API

```kotlin
KoinApplication(
    configuration = koinConfiguration { modules(appModule(database)) },
    logLevel = Level.INFO,
) { ... }
```

不再有 deprecation warning。

## 6. ViewModel 测试

新增：

```text
ReaderViewModelTest.restoresProgressForSameChapter
ReaderViewModelTest.ignoresProgressFromAnotherChapter
LibraryViewModelTest.exposesFavouritesFromRepository
```

覆盖：

- 进度章节匹配/不匹配；
- 收藏 Flow 驱动。

## 7. Room 迁移

- common：`MIGRATION_1_2`（SQLiteConnection）；
- Android：`AndroidMigration1To2`（SupportSQLiteDatabase）；
- `MigrationTest`：手工建 v1 `app_meta`，打开 v2，插入收藏并读取成功。

## 8. 测试矩阵

```text
reader     193
source      76
network    105
data         5
database     1
feature:reader   3
feature:library  1
------------------
total      384
```

三端：

- Android APK ✅
- Desktop ✅
- iOS framework + Xcode 模拟器 ✅

## 9. 剩余限制

- MangaDex 只有搜索/源切换，没有完整筛选树（标签/年份/状态/排序 UI）；
- ~~源选择是全局的，没有按路由保存 source~~ 已改为路由携带 sourceName。
- MangaDex UUID → Long 映射只在 runtime 内存中，进程重启后需要重新拉列表；
- 没有 SavedStateHandle，ViewModel 的参数仍通过 `load(...)` 传入；
- 没有 Android/iOS 真机运行验证（仅构建/模拟器）；
- ~~Reader 仍是单页 LTR~~ 已暴露 Single/Double/RTL。
- ~~没有错误重试~~ 已加 Retry；诊断面板仍未做。

## 10. 结论

上一轮列出的“明显限制”已经不再阻塞开发：

- 数据能持久化并恢复；
- 真实源能切换/搜索；
- 图片带 header；
- 阅读布局由 reader-core 决定；
- ViewModel 有测试；
- Android 迁移有适配与测试；
- Koin 不再用弃用 API。

剩下的是产品级完善，而不是架构缺口。

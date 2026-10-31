# 16 · Room 持久化：收藏与阅读进度

日期：2026-10-31
状态：收藏 + 阅读进度已落库（fixture 数据）

## 1. 目标

把“退出重进还能恢复”落到实处：

- 收藏可以持久化；
- 阅读进度可以持久化；
- Android / Desktop / iOS 使用同一套 `MiyomuDatabase`；
- 数据库升级从 v1 → v2。

## 2. Schema

`core:database` 从只有 `app_meta` 的 v1 升到 v2：

```text
favourite
  contentId (PK), sourceName, title, coverUrl, authors,
  contentType, createdAt, updatedAt

reading_progress
  contentId (PK), chapterId, chapterTitle, page, scroll,
  percent, updatedAt
```

DAO：

- `FavouriteDao.observeAll()` / `observeIsFavourite()` / `upsert()` / `delete()`
- `ReadingProgressDao.observe()` / `find()` / `upsert()`

迁移：

- `MIGRATION_1_2` 在 commonMain，使用 `SQLiteConnection`；
- Desktop/iOS 使用 `BundledSQLiteDriver`；
- Android 使用框架 Room builder（新装库直接 v2）。

## 3. Repository

`core:data/LibraryRepository`：

```kotlin
interface LibraryRepository {
    fun observeFavourites(): Flow<List<ContentSummary>>
    fun observeProgress(contentId: ContentId): Flow<ReadingProgress?>
    suspend fun setFavourite(summary: ContentSummary, favourite: Boolean)
    suspend fun saveProgress(
        contentId: ContentId,
        chapterId: ChapterId,
        chapterTitle: String?,
        page: Int,
        scroll: Float,
        percent: Float,
    )
}
```

实现 `DefaultLibraryRepository`：

- 收藏写入 `favourite`；
- 进度写入 `reading_progress`；
- Flow 直接驱动 UI；
- 时间戳用 `kotlin.time.Clock`。

## 4. 平台数据库位置

| 平台 | 路径 |
| --- | --- |
| Android | `applicationContext.getDatabasePath("miyomu.db")` |
| Desktop | `~/.miyomu/miyomu.db` |
| iOS | `NSHomeDirectory()/Documents/miyomu.db` |

入口：

```kotlin
MiyomuApp(database)
```

- Android `MainActivity` 创建 framework DB；
- Desktop `Main.main()` 创建 bundled DB；
- iOS `MainViewController` 创建 bundled DB；
- `ScreenshotTest` 使用 in-memory DB。

## 5. UI 接线

### Details

- 顶部新增 `☆ Save` / `★ Saved`；
- 点击调用 `LibraryRepository.setFavourite()`；
- 收藏状态由 `observeFavourites()` 驱动，重启后仍存在。

### Reader

- 页码变化时调用 `saveProgress()`；
- 保存 `page`、`percent`、`chapterId`；
- 下次进入 Reader 时可以从 `observeProgress()` 恢复（UI 尚未接恢复逻辑，下一步）。

## 6. 测试

`LibraryRepositoryTest`：

```text
favouritePersistsAndCanBeRemoved   ✅
readingProgressPersists            ✅
```

核心测试总数：

```text
reader    193
source     74
network   105
data        5
-------------
total     377
```

## 7. 构建

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

## 8. 限制

- Android 迁移已补 `AndroidMigration1To2`（`SupportSQLiteDatabase`）。
- 已加 `MigrationTest`：v1 `app_meta` → v2，插入收藏并读取成功。
- 收藏页/Home 已消费 `observeFavourites()`，但还没有移除/批量操作。
- Reader 已恢复进度，但还没有“继续阅读”跳转。
- Room 已接 Koin/ViewModel。

## 9. 下一步

1. ~~Reader 启动时读取 `observeProgress()` 并恢复页码~~ 已完成。
2. ~~收藏页/Home 消费 `observeFavourites()`~~ 已完成。
3. Android 迁移适配 + Room 迁移测试。
4. ~~用 ViewModel + Koin 替换页面里的 `remember/LaunchedEffect`~~ 已完成。
5. ~~真实 JSON 源 + Coil 图片~~ 已完成，见 [17-real-source.md](17-real-source.md)。

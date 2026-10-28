# ADR-0009：Room KMP 作为数据层

- 状态：接受（2026-10-28）
- 关联：[ADR-0001](0001-kmp-cmp-baseline.md) · [M1 清单](../checklists/phase-1-m1.md)

## 背景

架构草案默认 SQLDelight。M1 实现数据层前，对 SQLDelight 2.4.1 与 Room 2.8.5 都做了同条件探针：

- 环境：Kotlin 2.3.21、AGP 9.4.1、KMP Android library + JVM、无兼容旗标。
- SQLDelight 2.4.1：`build` 通过，生成查询接口。
- Room 2.8.5 + KSP 2.3.12 + `androidx.sqlite:sqlite-bundled:2.6.2`：`build` 通过，JVM 内存数据库测试 1/1，Android AAR 编译，schema 导出。

Animeko（Apache-2.0，KMP + Compose Multiplatform）使用 Room 2.8.4 + KSP + bundled SQLite，证明 Room KMP 在 Android + 桌面的大型项目中可用。同时 Room 的 AI 训练语料、Android 生态和 Flow/Paging/迁移工具更成熟。

## 决策

- 数据层采用 **Room KMP 2.8.5**，KSP **2.3.12**，`androidx.sqlite:sqlite-bundled:2.6.2`。
- `:shared:data` 使用 Room Gradle 插件导出 schema；实体/DAO 在 commonMain，KSP 按 target 配置：
  - `add("kspAndroid", room-compiler)`
  - `add("kspJvm", room-compiler)`
- Android 与桌面都用 `BundledSQLiteDriver`，保证 SQLite 行为一致；数据库创建放在平台工厂：
  - Android：`AndroidMiyomuDatabaseFactory(context)`
  - 桌面：`JvmMiyomuDatabaseFactory(path)`
- 领域层只依赖 `LibraryRepository` / `ProgressRepository`，不依赖 Room 类型。
- 不使用 SQLDelight；删除 `.sq` schema 与 SQLDelight 插件/依赖。

## 验证

- 生产 `:shared:data:jvmTest` 3/3：
  - 库条目 upsert / Flow 查询 / 删除 / 标记已读；
  - 进度按 item 保留最新；
  - 文件数据库关闭后重新打开仍能读到数据。
- `:shared:data:assemble`、`:desktopApp:compileKotlin`、`:androidApp:assembleDebug` 通过。
- 双端调试壳实际运行：
  - 打开作品后显示 `library 1`；
  - 打开章节后显示 `progress saved`；
  - 证据：`evidence/m1-desktop-room.png`、`evidence/m1-android-room.png`。

## 后果

正面：

- 与 Animeko 的 KMP 数据层路线一致，AI 生成 Entity/DAO 的首次正确率更高。
- Room 的 schema 校验、迁移、Flow/Paging 生态更完整。
- Android 与桌面共用 bundled SQLite，减少平台 SQLite 差异。

负面 / 约束：

- 新增 KSP 与 Room Gradle 插件；工具链升级时必须同时验证 KSP/Room。
- `room-ktx` 没有 JVM 变体，KMP 下只使用 `room-runtime` + `sqlite-bundled`。
- Room 的 Android builder 需要 Context，数据库创建必须走平台工厂；common 不能直接传入路径。
- Schema 变更需要导出 JSON 并编写迁移，不能只改实体。
- 当前只有 `library_item` 与 `reading_progress` 两张表；分类、下载、同步表在后续里程碑补充。

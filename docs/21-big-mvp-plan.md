# 21 · Big MVP 计划：漫画 + 小说 + 双源 + 下载

日期：2026-10-31
状态：范围已确认，作为下一阶段执行基线
关系：本文补充 [07-架构优先方案](07-architecture-first.md) 的 MVP 范围；架构原则仍以 07 为准。

## 1. 结论

Miyomu 的 MVP 不是“Kototoro 全功能平替”，而是一个**双源引擎、双阅读器、双端可用的产品闭环**：

```text
Legado JSON  →  小说 / 部分漫画
Mihon APK    →  漫画（Android）
        ↓
Browse / Search / Details / Source Manager
        ↓
漫画阅读器 + 小说阅读器
        ↓
章节下载 + 离线阅读
        ↓
Android + Desktop 验收；iOS 只保证构建
```

关键决策：

| 议题 | 决策 |
| --- | --- |
| 内容类型 | 漫画 + 小说 |
| 平台 | Android + Desktop 首发；iOS 只保证构建 |
| 离线下载 | 做，手动章节下载 + 离线阅读 |
| Legado | 做，Tier 1 + 部分 Tier 2 |
| Mihon | 做，仅 Android；桌面端移出 MVP |
| Mihon Desktop | 单独立项，不阻塞 MVP |
| 小说来源 | Legado 源；不做 EPUB/TXT 本地导入 |
| 源兼容验收 | 10 个 Legado + 10 个 Mihon 的矩阵，目标通过率 ≥ 80% |
| 自动追更/CBZ | 不做 |

一句话：

> **MVP 验证的是“用户能用一个 App，通过两类源，读漫画和小说，并且能离线重看”。**

## 2. 为什么这样切

当前项目已经完成 P1 纵切：

- SourceRuntime 契约；
- MangaDex 真实源；
- 漫画 Paged Reader；
- Room 收藏/进度；
- Coil；
- ViewModel + Koin；
- Android/Desktop/iOS 三端构建；
- 384 个测试。

但距离“大 MVP”还缺三条真正的主线：

1. **源兼容**：Mihon + Legado，不只是多一个 HTTP 源；
2. **小说阅读**：文本模型、排版、进度、正文加载与图片阅读器完全不同；
3. **离线下载**：漫画图片和小说文本是两套存储/队列/恢复模型。

这三条都比“再加几个页面”更接近产品成败。

## 3. MVP 必须做 / 不做

### 3.1 Must

**源**

- Legado JSON T1，覆盖搜索、详情、目录、正文；
- Legado 源导入：剪贴板、文件、URL；
- Legado 源编辑、校验、启用/禁用、删除；
- Mihon Android 扩展加载：已安装 APK + 本地 APK 导入；
- Mihon extension-lib 1.2–1.9 兼容范围；
- 源管理页、源详情、源测试/诊断；
- 统一 `SourceRuntime` 适配层，不把 Mihon/Legado 模型泄漏到 UI。

**内容**

- 漫画 + 小说；
- Browse、搜索、筛选、分页/加载更多；
- 详情、章节、阅读状态；
- 收藏、进度、继续阅读。

**阅读**

- 漫画：单页/双页/RTL、缩放/拖动、跨章、预取/缓存、下载；
- 小说：滚动/分页、字体/行距/背景、章节切换、字符进度、下载；
- 错误、空态、重试、加载态。

**下载**

- 手动选择章节/多选；
- 队列、暂停、继续、取消、重试；
- 删除、存储占用；
- 离线阅读；
- Android 基础进度通知；
- 下载页面。

**平台**

- Android：完整功能；
- Desktop：Legado + 内置源 + 下载；
- iOS：构建通过、空态可用；不承诺 Mihon/Legado JS。

### 3.2 Won't

- Mihon Desktop；
- iOS 扩展宿主；
- 视频；
- 备份/WebDAV/Drive；
- 追踪站点/统计/迁移；
- OCR/翻译/TTS；
- EPUB/TXT 本地导入；
- CBZ 导出；
- 自动追更下载；
- 全量 Legado JS 兼容；
- 全版本 Mihon/所有扩展兼容。

## 4. 模块变化

在现有模块上增补，不重写 P1：

```text
core/
  model/                 + NovelChapterContent、DownloadState、SourceCapabilities
  source/                SourceRuntime、Registry、内置源
  source-legado/         Legado 规则引擎 + SourceRuntime 适配
  reader/                漫画场景（已有）
  reader-novel/          小说文档、分块、排版、分页
  data/                  Repository + 下载仓库 + 离线优先
  database/              + source_config、download、download_page
feature/
  extensions/            源管理 UI（Mihon + Legado）
  downloads/             下载队列/离线 UI
  reader/                漫画/小说阅读器 UI 分发
source/
  host/                  Android Mihon APK 宿主；桌面宿主后续
```

依赖规则不变：

```text
feature -> core
core:source-legado -> core:source + core:network + core:model
core:reader-novel  -> core:model
source:host        -> core:source + core:model（仅平台层）
core:* 不依赖 feature/app
```

### 4.1 源抽象

在 `SourceRuntime` 外增加“引擎/能力”层，不改变现有契约：

```kotlin
enum class SourceEngine { BUILTIN, LEGADO, MIHON }

data class SourceCapabilities(
    val contentTypes: Set<ContentType>,   // MANGA, NOVEL
    val supportsSearch: Boolean,
    val supportsFilters: Boolean,
    val supportsChapterContent: Boolean,
    val supportsImageHeaders: Boolean,
    val requiresLogin: Boolean,
    val hasCloudflare: Boolean,
)

interface SourceEngineHost {
    suspend fun installedSources(): List<SourceRef>
    suspend fun install(spec: SourceInstallSpec): SourceInstallResult
    suspend fun test(source: SourceRef): SourceTestReport
}
```

原则：

- Mihon/Legado 的模型只存在于适配层；
- 上层只认 `SourceRuntime` + `SourceCapabilities`；
- 不支持的能力必须显式返回，不静默假装支持；
- 源名 + 引擎 + 版本写进 `SourceDescriptor`，便于诊断。

## 5. Source Gate：Legado

### 5.1 范围

Legado 是小说主力源，也是跨端最可控的源引擎。

**Tier 1（MVP 必须）**

- GET/POST；
- URL 模板与相对 URL；
- JSONPath / CSS / XPath / 正则；
- 搜索、详情、目录、正文；
- headers、cookies、编码；
- 分页；
- 基础变量与替换。

**Tier 2（MVP 部分）**

- `{{}}` 简单表达式；
- `java.*` 常用字符串/时间/编码函数；
- 登录 Cookie 注入；
- 动态规则的基础分支。

**Tier 3（不做）**

- 完整 Rhino/QuickJS 运行时；
- 复杂 WebView 登录；
- 任意 Java 反射；
- 加密/签名逆向；
- 所有 Legado 源 100% 兼容。

### 5.2 模块

`core/source-legado`：

- `LegadoRuleEngine`：规则解析、URL、变量；
- `AnalyzeRule`：CSS/XPath/JSONPath/Regex；
- `LegadoMapper`：`LegadoBookSource` ↔ `SourceRuntime`；
- `LegadoHttp`：headers/cookies/重试；
- `LegadoJsBridge`：Android/Desktop actual；iOS stub；
- 测试 fixture：真实响应快照 + 失败源。

小说正文：

- `ChapterContent` 包含 HTML、纯文本、baseUrl、headers；
- HTML → 段落/标题/图片；
- 小说阅读器不直接渲染任意 WebView。

### 5.3 源管理

`feature/extensions`：

- 导入 JSON：剪贴板、文件、URL；
- 字段校验：`bookSourceUrl/name`、搜索、详情、目录、正文规则；
- 源列表：启用/禁用、排序、删除、复制；
- 源详情：能力、最近测试结果、失败原因；
- 源测试：搜索 → 详情 → 目录 → 正文，逐步 PASS/FAIL。

## 6. Source Gate：Mihon

### 6.1 范围

**仅 Android。**

- 已安装 Mihon/Tachiyomi 扩展 APK 扫描；
- 本地 APK 导入；
- extension-lib 1.2–1.9；
- `PackageManager` 元数据识别；
- child-first ClassLoader；
- Injekt 桥；
- `Source` / `CatalogueSource` → `SourceRuntime` 适配；
- 章节页 headers / 图片 headers；
- 源测试。

**不做：**

- Mihon Desktop；
- iOS；
- 未知 ABI/私有修改扩展；
- 所有扩展 100% 兼容。

### 6.2 桌面策略

Desktop 首发源能力：

```text
Legado JSON + 内置 JSON 源（如 MangaDex）
```

Mihon Desktop 单独立项，参考 Kototoro 的：

- `mihon-desktop-compat`
- `source-host`
- `dex-convert`
- AndroidCompat / WebView / Looper 兼容层

预计额外 4–6 周，且是最高风险项。**不进入 MVP 验收。**

## 7. 兼容性矩阵

### 7.1 Legado

| 项 | 目标 |
| --- | --- |
| 源数量 | 10 个真实源 |
| Tier 1 功能 | 100% 覆盖 |
| 通过率 | ≥ 80% |
| 失败记录 | 每个失败源记录规则/能力缺口 |
| 回归 | 响应 fixture + 快照 |

### 7.2 Mihon

| 项 | 目标 |
| --- | --- |
| 扩展数量 | 10 个常用扩展 |
| extension-lib | 1.2–1.9 |
| 通过率 | ≥ 80% |
| 场景 | 列表/搜索/详情/章节/页面 |
| 失败记录 | ABI、ClassLoader、依赖、登录、Cloudflare |

### 7.3 验收口径

“兼容”不是一句口号：

- 每个源必须跑完 **列表 → 详情 → 章节 → 页面/正文**；
- 记录耗时、错误类型、headers 是否可用；
- 通过率低于 80% 时，**G1 不进入 G2**；
- 不把“能加载扩展”算作“源可用”。

## 8. 阅读器

### 8.1 漫画

在现有 `PagedReaderScene` 上补：

- 缩放/拖动/双击；
- 上一章/下一章；
- 预取窗口；
- 图片 headers；
- 下载内容本地优先；
- 页面错误重试；
- 阅读器设置：模式、方向、预取、缓存。

### 8.2 小说

新增 `core/reader-novel`：

- 文档块：标题/段落/图片/分隔；
- 分页或滚动；
- 字体、字号、行距、背景；
- 字符偏移进度；
- 章节切换；
- 正文来源：Legado；
- 下载后离线。

小说不做：

- TTS；
- EPUB/TXT；
- 批注/摘录；
- 翻译。

### 8.3 共享

- 阅读器 chrome；
- 章节面板；
- 设置 sheet；
- 进度存储接口；
- 错误/重试。

漫画和小说共享 **chrome/导航/设置**，不共享页面渲染模型。

## 9. 下载

### 9.1 数据模型

```text
download
  id, sourceName, contentId, chapterId, contentType,
  state, pageCount, totalBytes, downloadedBytes,
  error, createdAt, updatedAt

download_page
  downloadId, index, remoteUrl, localPath, headersJson
```

小说：

```text
novel_download
  downloadId, chapterContentPath, charCount
```

### 9.2 状态

```text
QUEUED -> RUNNING -> PAUSED -> RUNNING -> COMPLETED
                         -> FAILED -> QUEUED
                         -> CANCELED
```

### 9.3 平台

- Android：WorkManager + 前台/进度通知；
- Desktop：协程队列 + 文件锁；
- 统一 `DownloadRepository` / `DownloadManager` 接口；
- 下载页面读取本地，不依赖源在线；
- 删除章节时清理文件与 DB 行；
- headers 不写日志。

### 9.4 范围

做：

- 手动选择章节；
- 队列/暂停/继续/取消/重试；
- 删除；
- 存储占用；
- 离线阅读；
- 基础通知。

不做：

- 自动追更；
- CBZ；
- 云备份；
- 多设备同步。

## 10. 数据与离线优先

新增表：

- `source_config`：引擎、源名、配置 JSON、启用、排序、版本；
- `download` / `download_page` / `novel_download`。

Repository 顺序：

```text
本地已下载 -> 源在线 -> 错误
```

要求：

- 断网时已下载内容可读；
- 在线时也能识别本地章节，不重复下载；
- 下载进度和阅读进度分表，互不覆盖；
- 源被删除/禁用时，下载内容仍可读。

## 11. 里程碑

| Gate | 内容 | 估计 |
| --- | --- | --- |
| G0 | 当前 P1 基线 | 已完成 |
| G1a | Legado T1 + 源管理 + 测试集 | 3–4 周 |
| G1b | Mihon Android + 安装/诊断 + 测试集 | 3–4 周 |
| G2 | 漫画阅读体验 + 小说阅读器 | 3–4 周 |
| G3 | 漫画/小说下载 + 离线 | 3–4 周 |
| G4 | 双端集成、UI、QA、release | 3–4 周 |

单人顺序开发约 **15–20 周**（3.5–4.5 个月）；两人并行（源/阅读器）可压到 **10–12 周**。

### Gate 退出条件

**G1**

- 10 Legado + 10 Mihon 矩阵 ≥ 80%；
- 源安装/启用/禁用/删除/测试可用；
- 源层无 crash；错误可读。

**G2**

- 漫画连续读 3 章；缩放/跨章；
- 小说连续读 3 章；字号/进度；
- 退出重进恢复。

**G3**

- 漫画一章 + 小说一章下载；
- 飞行模式可读；
- 暂停/继续/删除可用。

**G4**

- Android release 可安装；
- Desktop 可启动；
- iOS 构建通过；
- 关键路径测试 + 截图。

## 12. 测试策略

新增：

- Legado 规则 fixture 测试（URL/selector/JSONPath/JS）；
- Mihon 扩展 ABI/ClassLoader 测试（Android instrumentation 或 Robolectric/JVM 可行部分）；
- 下载状态机测试；
- 断点/暂停/恢复测试；
- 离线 Repository 测试；
- 小说排版/进度测试；
- 迁移测试（新增表）。

兼容矩阵是**手工/半自动验收**，不能只靠单测。

## 13. 风险

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| Legado 规则差异大 | 小说源通过率低 | Tier 定义 + 10 源矩阵 + 失败分类 |
| JS 引擎跨端 | iOS/Desktop 复杂 | Android/Desktop actual；iOS stub |
| Mihon ClassLoader/Injekt | 扩展加载失败 | 限定 lib 版本 + 10 扩展矩阵 |
| Mihon Desktop | 拖垮 MVP | 移出 MVP，单独立项 |
| 小说 HTML 复杂 | 排版/进度问题 | 只支持受控 HTML 子集 + 清洗 |
| 下载存储/恢复 | 数据损坏/空间 | 文件锁 + DB 状态 + 清理 |
| Android 后台限制 | 下载中断 | WorkManager + 前台通知 |
| 源站点变化 | 测试失效 | fixture + 兼容报告 + 重试 |

## 14. 完成定义

- [ ] G1 源矩阵 ≥ 80%，报告入库；
- [ ] 源管理页可用；
- [ ] 漫画阅读器：缩放/跨章/预取；
- [ ] 小说阅读器：3 章连续阅读；
- [ ] 收藏/进度/下载持久化；
- [ ] 漫画/小说离线阅读；
- [ ] Android release；
- [ ] Desktop 启动；
- [ ] iOS 构建；
- [ ] 关键路径测试通过；
- [ ] 文档、截图、已知限制。

## 15. 当前不做但保留

- Mihon Desktop；
- 视频；
- 备份/WebDAV/Drive；
- 追踪/统计/迁移；
- OCR/翻译/TTS；
- EPUB/TXT；
- CBZ；
- 自动追更；
- 全量 Legado JS / 全版本 Mihon。

这些是 P2/P3，不进入 MVP 验收，但不删除模块边界。

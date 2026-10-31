# 11 · P1 状态：reader-core 已复制

日期：2026-10-31
阶段：P1（Core extraction）

## 1. 已完成

| 项 | 状态 | 证据 |
| --- | --- | --- |
| 复制 `reader-core` 主源码 | ✅ | 54 个 Kotlin 文件，包名暂保留 `org.skepsun.kototoro.reader.core` |
| 复制 `reader-core` 测试 | ✅ | 34 个测试文件，共 193 个测试通过 |
| 隔离守卫测试适配新目录 | ✅ | `ReaderCoreIsolationGuardTest` 路径改为 `core/reader/...` |
| `:core:reader` 接入 App | ✅ | `feature:reader → core:reader`，Android/Desktop/iOS 构建通过 |
| 三端离线构建 | ✅ | Android APK / Desktop 测试 / iOS framework 全部通过 |
| 版本恢复 | ✅ | Kotlin 2.4.21、Compose 1.12.2、Ktor 3.6.1 保持计划值 |

## 2. reader-core 统计

```text
commonMain: 54 files
desktopTest: 34 files
total: 88 Kotlin files / 9,307 lines
tests: 193 passed
```

复制策略：

- 包名暂保留 `org.skepsun.kototoro.reader.core`，减少一次性改名风险；P1 结束后统一重命名。
- 测试源集从 `jvmTest` 改为 `desktopTest`（目标名为 `desktop`）。
- 只改了两处路径常量（隔离守卫测试的 fixture / source 目录）。
- 没有改任何几何/场景/进度算法。

## 3. 构建验证

```bash
./gradlew :core:reader:desktopTest --offline --no-configuration-cache
# 193 tests passed

./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :app:shared:linkDebugFrameworkIosSimulatorArm64 \
          --offline --no-configuration-cache
# BUILD SUCCESSFUL
```

## 4. 阻塞项：core-source / core-net

尝试复制 `core-source` 与 `core-net` 时，Maven Central 对本机 IP 返回 **403**：

```text
This IP has been blocked for excessive or automated consumption of Maven Central...
```

这是本轮自动化查询触发的外部访问限制，不是代码问题。为避免继续请求或规避限制，已：

- 停止对 Maven Central 的批量查询；
- 暂不把 `core-source` / `core-net` 的真实源码接入构建；
- 两个模块保持空壳，避免 `./gradlew build` 因缺失依赖失败；
- `feature:*` 暂时不依赖 `core:data`，直到数据层可构建。

恢复网络访问后，P1 的下一批是：

1. `core-source`：SourceRuntime / 协议 DTO / SourceEndpoint / 动态筛选。
2. `core-net`：WebDAV / Drive / MAL 共享 HTTP 协议。
3. `core-backup` / `core-domain`。
4. 接 fixture 源，跑通“浏览 → 详情 → 章节 → 页面”。

## 5. 当前构建边界

```text
app:shared
  ├─ core:ui
  ├─ core:design
  ├─ feature:library   (UI only)
  ├─ feature:discover  (UI only)
  ├─ feature:reader    (UI only; depends core:reader)
  ├─ feature:settings  (UI only)
  └─ feature:video/downloads/sync/extensions (UI placeholders)

core:reader   ← 真实 Kototoro 算法
core:database ← Room 占位
core:source   ← 空壳（等待网络）
core:network  ← 空壳（等待网络）
core:data     ← 空壳（等待 source/network）
```

## 6. 下一步

- ~~补 `build-logic` 约定插件~~ 已完成，见 [12-build-logic.md](12-build-logic.md)。
- ~~用 Navigation Compose 替掉 `remember` 切 tab~~ 已接入 Navigation Compose KMP，见 [13-navigation.md](13-navigation.md)。
- 网络恢复后继续 `core-source` / `core-net`。
- 在 `feature:reader` 里用 `reader-core` 做最小几何演示（不接真实图片）。

## 7. 结论

P1 的第一步已经落地：**Kototoro 的阅读器几何真正进入了新架构，并通过原测试**。

这一步的价值不在 UI，而在于验证了“复制核心代码 + 适配目录/测试 + 三端编译”的迁移路径可行。后续的 source/network 复制可以沿用同一流程。

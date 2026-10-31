# 11 · P1 状态：reader-core / core-source / core-net 已复制

日期：2026-10-31
阶段：P1（Core extraction）

## 1. 已完成

| 模块 | 文件 | 测试 | 状态 |
| --- | ---: | ---: | --- |
| `:core:reader` | 54 主源码 + 34 测试 | **193** | ✅ |
| `:core:source` | 36 主源码 + 15 测试 | **75** | ✅ |
| `:core:network` | 25 主源码 + 10 测试 | **105** | ✅ |
| `:core:data` | 5 主源码 + 2 测试 | **5** | ✅ |
| **合计** | **96 + 65 文件** | **384** | ✅ |

三端编译：

- Android：`:core:source` / `:core:network` 编译通过。
- Desktop：`desktopTest` 全部通过。
- iOS：`:core:source:compileKotlinIosSimulatorArm64`、`:core:network:compileKotlinIosSimulatorArm64` 通过。

## 2. 复制策略

- 包名暂保留 `org.skepsun.kototoro.*`，先保证行为与测试一致；稳定后统一重命名。
- 测试源集从 `jvmTest` 改为 `desktopTest`（模块目标名为 `desktop`）。
- `reader-core` 只改隔离守卫测试的两处路径常量。
- `core-source` / `core-net` 不做算法修改，只补依赖：
  - serialization JSON + protobuf
  - okio
  - Ktor core / ktor-client-mock
  - coroutines-test
- 依赖版本回到计划值：Kotlin 2.4.21、Compose 1.12.2、Ktor 3.6.1、serialization 1.12.0、okio 3.18.2。

## 3. 测试结果

```text
:core:reader:desktopTest    193 passed
:core:source:desktopTest     74 passed
:core:network:desktopTest   105 passed
----------------------------------------
total                       384 passed
```

验证命令：

```bash
./gradlew :core:reader:desktopTest \
          :core:source:desktopTest \
          :core:network:desktopTest

./gradlew :core:source:compileKotlinIosSimulatorArm64 \
          :core:network:compileKotlinIosSimulatorArm64
```

## 4. Maven 访问说明

本轮中途，Maven Central 曾对本机 IP 返回 403（自动化请求过多触发）。
已停止批量查询，未绕过限制；访问恢复后重新拉取依赖，完成 source/network 的构建与测试。

## 5. 当前构建边界

```text
app:shared
  ├─ core:ui / core:design
  ├─ feature:library / discover / reader / settings
  └─ feature:video / downloads / sync / extensions (UI placeholders)

core:reader    ← 真实 Kototoro 阅读器几何
core:source    ← 真实 SourceRuntime / 协议 DTO
core:network   ← 真实 WebDAV / Drive / MAL HTTP 协议
core:database  ← Room 占位
core:data      ← ContentRepository + LibraryRepository + Room; App uses MangaDex + Coil + Koin
```

说明：

- 三个 core 模块已能独立构建/测试，但 **还没有被 feature 消费**；
- `feature:*` 暂未依赖 `core:data`，避免在 Repository 接口未定时形成临时耦合；
- 下一步是 `core:data` 把 source/network/reader 串成可用的 Repository。

## 6. 下一步

1. 复制 `core-backup` / `core-domain`。
2. `core:data` 接 Room：收藏/历史/进度。
3. 用真实 JSON 源替换 DemoSourceRuntime。
4. 页面迁到 ViewModel + Koin。
5. 增加 `Details → Reader(chapterId)`。
6. 接图片管线。

数据流细节见 [14-data-flow.md](14-data-flow.md)。

## 7. 结论

P1 的核心模块复制已完成一半以上：

- 阅读器几何：193 测试；
- 源协议：74 测试；
- 网络协议：105 测试。

这证明“复制 Kototoro 核心代码 + 新模块边界 + 新三方版本”可以稳定工作。剩下的是数据层与 Repository 编排，而不是重写算法。

# Phase 1 M0 清单：工具链与网络层

- 状态：**核心完成**（2026-10-28）；域名/组织注册与 GitHub CI 首跑待执行
- 关联：[ADR-0006](../adr/0006-unified-android-toolchain.md) · [ADR-0007](../adr/0007-network-and-cookie-storage.md) · [Phase 1 计划](../05-phase-1-plan.md)

## 1. 工具链

- [x] Android 应用锁定 AGP 9.4.1 + KGP/编译器 2.3.21 + Gradle 9.6 + JDK 21
- [x] compileSdk 37 / targetSdk 36 / minSdk 26
- [x] Compose BOM 2026.09.00（Compose 1.12.1）与 Compose compiler plugin 2.3.21
- [x] quickjs-kt 1.0.15（Kotlin 2.4 元数据）在 Kotlin 2.3.21 下编译
- [x] mediamp 0.5.1 与 Compose/QuickJS 共存
- [x] Ktor 3.6.0（Kotlin 2.3.21 构建）加入 Android 应用并实际发出请求
- [x] KMP Android library 使用 `kotlin { android {} }` 编译并产出 AAR
- [x] 放弃双工具链/AAR 包装方案（ADR-0006）

验证命令：

```bash
./gradlew -p experiments/phase1/toolchain-probe assembleDebug
./gradlew -p experiments/phase1/network-probe jvmTest assemble
```

运行时证据：

- `experiments/phase1/evidence/m0-toolchain-android.png`：`quickjs=3`、`ktor=200 body=miyomu-ok`、`player=org.openani.mediamp.MediampPlayer`
- `experiments/phase1/evidence/m0-network-android.png`：同上，含网络结果

## 2. 网络层与 Cookie

- [x] Ktor Client 3.6.0；Android = OkHttp，桌面 JVM = CIO
- [x] 统一 `createMiyomuHttpClient`（HttpCookies、HttpTimeout、User-Agent）
- [x] `FileCookiesStorage`：JSON、原子替换、Max-Age/Expires 清理、删除 Cookie、按 host 隔离
- [x] JVM 测试 8/8：持久化重载、过期、删除、host 隔离、落盘、损坏文件恢复、客户端重启后 Cookie 重发、User-Agent
- [x] 会话 Cookie 有意持久化（源代码登录跨重启）
- [ ] iOS Darwin 引擎（随 iOS 支持一起接）
- [ ] 代理 / 证书固定 / HTTP 缓存 / 重试（M1 源运行时产品化）

验证命令：

```bash
./gradlew -p experiments/phase1/network-probe jvmTest
./gradlew -p experiments/phase1/network-probe assemble
```

## 3. 标识符

- [x] 域名 RDAP 清查（2026-10-28）：`miyomu.app` / `.dev` / `.io` / `.net` 可注册；`miyomu.com` 已占用
- [x] GitHub 句柄清查：`miyomu` 被个人用户占用；`miyomu-app` / `miyomu-reader` / `getmiyomu` / `miyomuapp` 可用
- [x] 暂定包名 `app.miyomu`；建议 Maven group `io.github.miyomu-app`
- [x] App Store 检索无同名应用；公开网络检索未见 “Miyomu” 商标/产品命中（非正式商标检索）
- [ ] 注册 `miyomu.app` 与 GitHub org `miyomu-app`（需账号与付费，待用户执行）
- [ ] JP/US/EU/CN 正式商标检索

详见 [品牌备忘](../06-brand.md)。

## 4. CI

- [x] `.github/workflows/ci.yml`：桌面根构建、Android 探针/两个独立 Android Spike、iOS 模拟器测试
- [x] 所有 CI 命令已本地验证通过（2026-10-28）
- [ ] 推送到 GitHub 后确认 Actions 首次全绿（仓库尚未初始化）

本地已验证命令：

```bash
./gradlew build
./gradlew -p experiments/phase1/toolchain-probe assembleDebug
./gradlew -p experiments/phase1/network-probe jvmTest assemble
./gradlew -p experiments/spikes/s1-player-android assembleDebug
./experiments/spikes/s2-js-runtime-android/gradlew -p experiments/spikes/s2-js-runtime-android assembleDebug
./experiments/spikes/s2-js-runtime-ios/gradlew -p experiments/spikes/s2-js-runtime-ios iosSimulatorArm64Test
```

## 5. M0 未覆盖 / 移交 M1

- 把 S2 Spike 的测试用 `HttpURLConnection` / POSIX 客户端替换为正式 Ktor 网络层（M1 源运行时产品化）
- 播放器音轨与内嵌字幕选择（M1 播放器轨道 Spike）
- 源仓库索引/校验、每源隔离与配额（M1 源运行时产品化）
- KMP `:core` 数据层、SQLDelight、下载/同步骨架（M1）
- iOS App 壳 AVKit 验证（iOS 启动时）
- 真实源端到端（M1 验收）

## 结论

M0 的核心技术不确定性已消除：**单工具链可用，Ktor + 持久 Cookie 可用，命名无硬冲突**。可以进入 M1；外部注册与 CI 首跑不阻塞编码。

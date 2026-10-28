# Phase 0 启动清单

> 勾选即更新；每项附日期与证据链接。最后更新：2026-10-28

## 0. 文档与决策

- [x] 产品简报、目标架构、Phase 0 计划、风险登记、开放问题
- [x] ADR-0001 ~ 0005 初稿
- [x] S1/S2/S3 规格
- [x] Q1 Apache-2.0；Q2 Miyomu（2026-10-28 确认）；Q3 单人全职；Q4 Android + 桌面并行

## 1. 仓库与工具链

- [x] 目录与文档骨架
- [x] Gradle 版本目录与 S3 模块
- [x] `./gradlew --version` 通过（根 Gradle 9.6.0，JVM 21.0.12）
- [x] JDK 21 / Gradle 9.6.0 / Kotlin 插件 2.4.21 / CMP 1.12.2 已配置
- [x] Android SDK 37 与 AVD 可用
- [x] Xcode 26.6 与 iPhone 17 模拟器可用
- [x] `./gradlew build` 通过（S1/S2/S3 桌面模块）
- [x] Android 独立工程：Gradle 8.13 + AGP 8.13.2 + Kotlin 2.4.21（AGP 9.4 内置 Kotlin 2.2 与 quickjs-kt 2.4 元数据不兼容）；Ksoup 0.2.6；`android.useAndroidX=true`
- [x] iOS 独立工程：Gradle 8.13 + Kotlin/Native 2.4.10 + iosSimulatorArm64Test；Ksoup 0.2.6
- [ ] CI 最小流水线（可选）

## 2. S3：CMP + MD3E

- [x] 三屏骨架（库/详情/设置）
- [x] `./gradlew -p experiments :spikes:s3-ui:run` 可启动（窗口已创建；桌面锁屏，视觉验收待补）
- [x] 视觉验收（headless UI 截图：库/详情/设置/压力模式；真实窗口待解锁后复查）
- [x] 依赖审计表初版（Voyager/Injekt/OkHttp/Coil/WebView/RichText）
- [x] MD3E 组件可用性初版（alpha artifact 已验证 LoadingIndicator）
- [x] 1,000 项压力模式入口（Library → Stress 1,000）
- [x] 1,000 项网格性能数据（headless 滚动到 index 999：158ms）
- [x] 每屏迁移耗时记录（代理：三屏从零实现 <0.5 人日；真实 Aniyomi 移植待 Phase 1）
- [x] 报告更新：`docs/spikes/results/s3-cmp-ui.md`
- [x] 报告定稿与 Go/No-Go 建议（Conditional Go）

## 3. S1：mediamp

- [x] 创建 `experiments/spikes/s1-player` 模块，接入 mediamp 0.5.1
- [x] 生成测试素材（`fixtures/generate.sh`，媒体文件 gitignore）
- [x] Android 基础播放（MP4/MKV/HLS/HEVC）
- [x] macOS 基础播放（MP4/MKV/HLS/HEVC）
- [x] iOS 结论：裸 K/N 测试宿主无法驱动 AVKit；条件延期到 Phase 1 App 壳（非通过）
- [x] 外挂 SRT/ASS（Android 截图确认；桌面目视待补）
- [x] seek / 倍速 / 暂停（桌面 + Android）
- [x] 50 次连续 seek（Android 7.96s）
- [x] 30 分钟稳定性（Android 墙钟 30min；`FINISH after 1800000ms`）
- [x] 已识别缺口：音轨/内嵌字幕选择列入 Phase 1；PiP/后台待真机验证
- [x] 功能矩阵（桌面 + Android；iOS 条件未验证）
- [x] 报告定稿：`docs/spikes/results/s1-player.md`

## 4. S2：QuickJS 源运行时

- [x] 创建 `experiments/spikes/s2-js-runtime` JVM 模块，接入 quickjs-kt 1.0.15
- [x] 桌面运行 fixture 源（search → detail → chapters → images）
- [x] 源 KV
- [x] HTML DOM（桌面 + Android + iOS Ksoup）
- [x] Cookie 基础（桌面 + Android + iOS set/get/delete + 请求头；持久化未做）
- [x] Android HTML DOM + Cookie 宿主移植
- [x] iOS HTML DOM + Cookie 宿主移植
- [x] Android 运行（API 36 arm64 / 16KB AVD；JSON + HTML/Cookie）
- [x] iOS 运行（iPhone 17 模拟器 / iosSimulatorArm64；JSON + HTML/Cookie）
- [x] 超时、取消、死循环中断
- [x] 并发、内存、100 次实例创建/销毁
- [x] Venera 兼容 shim 初版（读路径子集，自写）
- [x] 报告初稿：`docs/spikes/results/s2-js-runtime.md`

## 5. 决策门

- [x] 三份报告完成并复核（S1 iOS 条件未验证，已记录替代方案）
- [x] 更新 ADR 状态（接受/修改/废弃）
- [x] 更新产品简报范围
- [x] 输出 Phase 1 任务列表与排期：`docs/05-phase-1-plan.md`
- [x] 记录决策：**Go（缩范围）** —— Android + 桌面，iOS 后置；音轨选择/网络层为 Phase 1 必做
- [x] Q1–Q4 用户确认：Apache-2.0；Phase 1 前定名；单人全职；Android + 桌面并行

## 常用命令

```bash
# 版本检查
./gradlew --version

# S3 运行（macOS 桌面窗口）
./gradlew -p experiments :spikes:s3-ui:run

# 全量构建（Phase 0 验证代码）
./gradlew -p experiments build

# Android 设备
adb devices

# iOS 模拟器
xcrun simctl list devices available
```

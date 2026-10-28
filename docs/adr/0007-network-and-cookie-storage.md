# ADR-0007：Ktor 网络层与持久 Cookie 存储

- 状态：接受（M0 已验证；iOS 引擎待接入）
- 日期：2026-10-28

## 背景

Phase 0 的 S2 使用测试专用 `HttpURLConnection`（Android）与 POSIX socket（iOS）证明可联网，但产品层需要：

- 统一的 KMP HTTP 客户端；
- 每个源隔离的 Cookie 容器，跨重启保留登录态；
- 统一的超时、User-Agent、重定向与后续的缓存/代理配置。

## 决策

- 使用 **Ktor Client 3.6.0**；引擎按平台：Android = OkHttp，JVM 桌面 = CIO，iOS = Darwin（后续接入）。
- 统一入口 `createMiyomuHttpClient(cookieStorage, engine)`，安装 `HttpCookies` 与 `HttpTimeout`，默认 User-Agent `Miyomu/0.1`。
- Cookie 通过 Ktor `CookiesStorage` 接口持久化；M0 实现 `FileCookiesStorage`：
  - JSON（kotlinx-serialization）序列化 `Cookie` 与写入时间；
  - 每次变更写临时文件后原子替换；
  - 支持 `Max-Age` / `Expires` 过期清理与删除 Cookie（`Max-Age=0`）；
  - 按源使用独立文件；会话 Cookie 有意持久化，保证源登录跨重启有效。
- 文件放在应用私有目录（Android `filesDir`、桌面应用数据目录），按源分配子目录；不得写入日志、备份或同步。
- 源隔离边界：一个源一个 `CookiesStorage` 实例/文件；删除源或退出登录时清除对应文件。

## 验证

- `experiments/phase1/network-probe` JVM 测试 8/8：持久化重载、`Max-Age` 过期、删除 Cookie、按 host 隔离、落盘、损坏文件恢复、Ktor 客户端重启后自动重发 Cookie、User-Agent。
- Android KMP library `assemble` 成功（OkHttp 引擎编译通过）。
- Android 应用运行时 `ktor=200 body=miyomu-ok`。

## 后果

正面：

- 源代码只面对受控 `Network` API，不直接持有平台 HTTP 客户端。
- Cookie 行为可单元测试，不依赖真实站点。

负面 / 约束：

- M0 的 Cookie 文件是明文（依赖 Android 应用沙箱 / 桌面用户权限）；敏感场景后续可加平台 keystore 加密。
- `FileCookiesStorage` 每次写入整文件，源多、Cookie 多时需要分片/配额与写入合并。
- Kotlinx-io 在 JVM 的 `atomicMove` 依赖 `File.renameTo`；Windows 已加「删除后重命名」回退，仍有极小窗口，生产实现应改用平台原子替换。
- 未实现代理、证书固定、HTTP 缓存、重试策略；这些在 M1 源运行时产品化时补齐。

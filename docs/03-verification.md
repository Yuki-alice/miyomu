# 验证记录（窄范围）

> 本文只记录“测过什么、没测什么”。**不是可用性报告。** 当前状态见 [00-current-status.md](00-current-status.md)。

## 模块 / fixture 测试

- 210 个 JVM 测试通过（2026-10-30）。
- 分布：`ui` 43、`novel` 30、`source-novel` 29、`source-runtime` 22、`reader-novel` 16、`player` 12、`data` 10、`network` 10、`source-repo` 9、`reader-comic` 8、`local` 7、`danmaku` 4、`download` 4、`source-manager` 3、`domain` 3。
- 这些测试主要用 fixture、内存数据或 headless UI，**不覆盖**真实用户导入、真实第三方源、真实网络稳定性或重启后的完整状态。

## Venera 源

- 13 个参考源：13/13 可零改构造；静态 API 审计 0 缺口。
- hitomi：search 25 → details 1 章 → pages 30 页，桌面 JVM 与 Android 模拟器能跑通这条路径。
- comick：HTTP 522；manga_dex：TLS `ProtocolVersion` alert（本机网络/站点问题）。
- 这些只覆盖构造、静态 API 面和 hitomi 单路径，**不证明**阅读器整体可用，也不证明其他源能真实阅读。

## 手动冒烟（预置数据 / 本地 fixture）

- 本地 TXT：扫描 → 详情 → 阅读器。
- legado 本地 fixture：浏览 → 发现 → 分类 → 详情 → 章节 → 阅读。
- 本地 MKV：详情 → 播放器。
- 设置、备份、源仓库、下载队列等页面能打开。
- 这些路径用的是我们准备的数据，不是普通用户的真实使用路径。

## 不能由上述结果推出的结论

- 不能证明全新安装后能导入真实书/真实源。
- 不能证明阅读、播放、搜索、进度、书架、源配置在重启后仍然可用。
- 不能证明真实第三方源覆盖、登录态、错误处理、性能或长期稳定性。
- 不能把测试数量、构造成功或静态审计当作功能完成/可用。

## 复现命令

```bash
# 模块测试（不代表可用性验收）
./gradlew :shared:ui:jvmTest :shared:source-runtime:jvmTest :shared:source-novel:jvmTest \
  :shared:novel:jvmTest :shared:reader-novel:jvmTest :shared:data:jvmTest

# Venera 静态审计 + 构造（需要本地 reference/）
python3 tools/venera_compat.py reference/venera-yuan/*.js --summary
./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
```

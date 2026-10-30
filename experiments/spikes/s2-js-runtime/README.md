# S2 JS Runtime Spike

QuickJS + Venera 风格源的桌面验证。旧规格/报告已随旧文档删除（git `14c495e`）。

## 已验证（2026-10-28，JVM 桌面）

- JSON 自写源：搜索 → 详情 → 章节图片 URL → 源级 KV 全部通过。
- 5 个 300ms 请求并发 309ms；脚本异常可捕获；死循环 1.5s 内取消。
- 冷启动 486ms；100 次 QuickJS 实例创建/销毁 24ms。
- **HTML 源**：Ksoup 0.2.6 驱动的 `HtmlDocument`/元素句柄 DOM，搜索/详情/章节图片、CSS 选择器、属性/class/text/children/parent 全部通过。
- **Cookie**：set/get/delete + HTTP `Cookie` 请求头往返通过；仍为内存实现，无持久化。
- **未测/未移植**：Android/iOS 的 HTML DOM 与 Cookie 宿主、持久化、源仓库。

## 运行

```bash
./gradlew -p experiments :spikes:s2-js-runtime:run
```

## 结构

```
src/main/kotlin/.../FixtureServer.kt   本地 JSON/HTML fixture
src/main/kotlin/.../Main.kt            宿主绑定与测试序列
src/main/resources/runtime/preamble.js 最小 ComicSource/Network shim（自写）
src/main/resources/runtime/demo_source.js  自写测试源
```

## 注意

- 异步调用必须 `await __invoke(...)`；加载脚本用 `evaluate<Any?>`。
- 源实例化后再注册（基类构造函数拿不到子类字段）。
- 不要把真实 Venera 源或 GPL 代码提交到仓库。

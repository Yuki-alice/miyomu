# ADR-0013: JS 源包与仓库索引格式

- 状态：**接受**（2026-10-29）
- 关联：[ADR-0003](0003-js-extension-runtime.md)（JS + QuickJS 唯一运行时）· [ADR-0008](0008-venera-source-runtime.md)（Venera 风格读路径）· [架构文档](../01-architecture.md)（"索引 JSON + 脚本 URL，用户手动添加"）

## 背景

Phase C 需要让用户添加源仓库、安装/更新/卸载 JS 源。运行时（`:shared:source-runtime`）只接收
一段脚本文本，不关心它从哪里来；仓库与安装完全在宿主侧。需要定下：

1. 仓库索引 JSON 的字段与相对 URL 语义；
2. 安装落盘布局与元数据；
3. 更新判定与完整性校验；
4. 安全边界（索引不可信）。

## 决策

### 仓库索引

仓库就是一个 **索引 JSON 的 URL**（用户填写完整 URL，不拼接默认路径）。格式：

```json
{
  "name": "Example repo",
  "version": 1,
  "sources": [
    {
      "id": "demo",
      "name": "Demo source",
      "version": "1.0.0",
      "lang": "en",
      "url": "demo.js",
      "sha256": "…可选，十六进制…",
      "description": "…可选…",
      "iconUrl": "…可选…"
    }
  ]
}
```

- `url` 可为绝对 URL 或相对索引 JSON 的路径（按 URI 规则解析）。
- `version` 是不透明字符串；与已安装版本不同即视为有更新（不做 semver 解析）。
- `sha256` 可选；提供时必须匹配。
- 索引中的未知字段忽略，便于演进。
- Miyomu **不内置、不推荐任何仓库**；用户自行添加。

### 安装布局与元数据

```
<sources 根>/
  manifest.json          # { "sources": [...], "repositories": ["索引 URL", ...] }
  sources/<id>/source.js
  cookies.json
```

- 源 id 必须匹配 `^[a-zA-Z0-9][a-zA-Z0-9._-]*$`（它同时是目录名，防止路径穿越）。
- 安装 = 下载脚本 → 校验 sha256（若有）→ 原子写入 `source.js` → 更新 `manifest.json`；
  `manifest.json` 同样原子写入（临时文件 + 原子移动）。
- 更新复用安装路径：保留 `installedAt`，刷新 `updatedAt`/版本/哈希。
- 卸载删除 `sources/<id>` 并移除清单条目；已安装源不因移除仓库而卸载。

### 安全边界

- 索引与脚本都不可信：id 先校验再落盘；URL 只允许 http/https。
- 单个脚本的运行时配额沿用 ADR-0008（30s、64MiB、响应/图片上限）。
- 脚本完整性由可选的 sha256 提供；仓库被篡改时用户仍可能安装恶意脚本，因此安装界面明确
  提示"第三方源由用户自行承担风险"，且 Miyomu 不托管内容。

## 后果

正面：

- 格式简单，静态托管即可；仓库作者无需服务端。
- 相对 URL 让一个仓库目录可以整体搬家。
- 与运行时解耦：安装层只产出 `source.js` + 元数据，加载逻辑后续接入。

负面 / 未完成：

- 没有签名体系（只有可选哈希），无法证明仓库作者身份。
- 版本比较是不透明字符串，回滚/降级由用户手动选择版本。
- 安装后的源尚未加载进运行时（C2：搜索/详情/阅读接线）。

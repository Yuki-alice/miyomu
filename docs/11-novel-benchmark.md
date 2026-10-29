# 小说端功能对标调研(2026-10-30)

- 目的:深化小说端前,对齐社区优秀小说/轻小说软件与 Google Play Books 的功能基线
- 方法:本地 `reference/` 仓库源码/README(GPL 项目仅行为参考)+ 公开资料
- 结论:确定本批实现的四个功能(§3),其余进入路线图(§4)。**§3 已于 2026-10-30 实现完毕**(书签 3 测试、书内搜索 3 测试、目录跳转 UI 测试)

## 1. 对标对象与功能矩阵

| 功能 | Google Play Books | legado(阅读) | NovelDokusha | Book's Story | Miyomu 现在 |
|---|---|---|---|---|---|
| 本地 TXT/EPUB | ✅(上传) | ✅ | ✅(EPUB) | ✅(7 格式) | ✅ |
| 分页 + 滚动双模式 | ✅ | ✅ | 滚动为主 | ✅ | ✅ |
| 字号/行距/边距 | ✅ | ✅ + 字距/缩进 | ✅ | ✅(最细) | ✅(4 项) |
| 阅读主题 | ✅(夜灯/暗色) | ✅ 多主题 | ✅ 明暗 | ✅ 色板 | ✅ 四档 |
| 目录跳转 | ✅ | ✅ | ✅ | ✅ | ❌ → 本批 |
| 书内搜索 | ✅ | ✅ | ✅ | ✅ | ❌ → 本批 |
| 书签 | ✅ | ✅ | ❌ | ✅ | ❌ → 本批 |
| 高亮/笔记 | ✅(可导出) | ✅ 划线 | ❌ | ❌ | ❌(需数据表,合并后) |
| TTS 朗读 | ✅ Read Aloud | ✅ 多引擎 | ✅ + 后台 | ❌ | ❌(M3) |
| 夜灯/护眼滤镜 | ✅ Night Light | ✅ 蓝光/亮度 | ❌ | ❌ | ❌ → 本批(滤镜) |
| 音量键翻页 | ❌ | ✅ | ❌ | ❌ | ❌ → 本批 |
| 翻页动画 | 轻微 | 仿真/覆盖/平移/滚动 | 滚动 | 滑动 | 平移(Pager 默认) |
| 在线书源 | 商店书源 | ✅ 书源生态 | ✅ 25+ 源 | Gutenberg | ❌(M2/M3) |
| 进度同步 | 账号云 | WebDAV | 备份文件 | ❌ | WebDAV(M3) |
| 轻小说插画 | ✅(固定版式书) | 部分 | 部分 | PDF/EPUB 图 | ❌(EPUB 图,路线图) |
| 阅读统计 | ❌ | ✅ 时长/章节 | ❌ | ❌ | ❌ |

调研来源:legado-with-MD3 与 Book's Story 的 README 与源码结构、NovelDokusha README(注:该项目 2026 年已停止维护,仅作 IA 参考)、[Google Play Books Night Light](https://mobilesyrup.com/2015/12/16/google-play-books-adds-night-light-to-help-you-read-in-the-dark)、[Read Aloud](https://goodereader.com/blog/electronic-readers/google-play-books-introduces-read-aloud-feature)。

## 2. 关键观察

1. **中文阅读软件(legado 系)的阅读器纵深最值得抄行为**:目录/书签/书内搜索三件套是每家标配,也是我们当前最大的体验缺口。
2. Google Play Books 的差异化在**内容服务**(笔记导出、字典/翻译查询、Read Aloud、云同步),这些都是数据层或平台能力,不在纯 UI 批次。
3. 轻小说场景(EPUB 插图、纵向排版、角注)是明确的进阶方向,需要解析层输出"内容块"结构,单独立项。
4. 许可边界不变:legado / Book's Story / NovelDokusha 均 GPL,**只抄行为与交互,不碰代码**;实现全部自写。

## 3. 本批实现(2026-10-30)

1. **目录/书签面板**:阅读器内呼出章节列表(标题过滤、当前章高亮、点击跳转)+ 书签列表(跳转/删除)。
2. **书签**:当前阅读位置一键添加/移除,摘要取位置处文本;按书存 `JsonPreferenceStore`(JSON 数组,零 Room schema 冲突,不碰共享数据层)。
3. **书内搜索**:跨章节标题 + 正文搜索(解析已缓存,TXT 惰性子串/EPUB 预提取,成本可控),结果点击直达"章节 + 偏移"。
4. **护眼暖色滤镜**:关/弱/中/强四档叠加层(跨平台纯 Compose);**音量键翻页**开关(Android 生效,桌面无此键)。

## 4. 路线图(2026-10-30 第二批:EPUB 插画、FB2/HTML/MD 格式、legado 书源引擎读路径均已落地,见 docs/10 §7)

- 翻页动画(覆盖/仿真):自绘 page delegate,中等成本,下一批候选。
- EPUB 插图:解析层 `NovelChapter` 从纯文本升级为内容块(text/image),分页器支持图片块高度;轻小说刚需,独立切片。
- 高亮/笔记:需要 Room 新表,等与漫画分支合并后统一动 schema。
- 亮度控制(Android window attributes,expect/actual)、自动翻页、TTS(M3)、在线书源(M2/M3)。

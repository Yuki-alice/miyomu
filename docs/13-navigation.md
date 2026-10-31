# 13 · Navigation

日期：2026-10-31
状态：基础导航已完成（内部实现，待替换为 Navigation Compose）

## 1. 背景

计划使用 `org.jetbrains.androidx.navigation:navigation-compose:2.10.0`，但当前环境：

- 该依赖未在 Gradle 缓存中；
- Maven Central 对本机 IP 返回 403；
- 不能为了一个依赖绕过访问限制。

因此先实现一个**零依赖、同语义的内部导航器**，把页面切换/返回栈稳定下来；网络恢复后可平移到 Navigation Compose。

## 2. 路由模型

`core:ui/navigation/MiyomuRoute.kt`：

```kotlin
sealed interface MiyomuRoute {
    val key: String

    data object Home : MiyomuRoute
    data object Library : MiyomuRoute
    data object Browse : MiyomuRoute
    data object Updates : MiyomuRoute
    data object More : MiyomuRoute

    data class Details(val contentId: String) : MiyomuRoute
}
```

- 顶层路由：Home / Library / Browse / Updates / More。
- Details 是 push 路由，带 `contentId`。
- `key` 用于后续状态保存/深链。

## 3. 导航器

`MiyomuNavigator`：

```kotlin
class MiyomuNavigator(start: MiyomuRoute = MiyomuRoute.Home) {
    val current: MiyomuRoute
    val canGoBack: Boolean
    fun push(route: MiyomuRoute)
    fun pop(): Boolean
    fun selectTopLevel(route: MiyomuRoute)
}
```

语义：

- `selectTopLevel` 清空栈，只保留一个顶层路由；
- `push` 叠加详情；
- `pop` 返回上一层；
- 顶层导航不产生重复栈。

`rememberMiyomuNavigator()` 把实例绑定到组合生命周期。

## 4. 壳层接线

`MiyomuApp()`：

```kotlin
val navigator = rememberMiyomuNavigator()
MiyomuBackHandler(enabled = navigator.canGoBack) { navigator.pop() }

MiyomuAppShell(
    selected = navigator.current.toDestination(),
    onSelect = { navigator.selectTopLevel(it.toRoute()) },
) {
    MiyomuNavHost(navigator) { route ->
        when (route) {
            Home -> HomeScreen(onOpenDetails = { navigator.push(Details(it)) })
            Library -> LibraryScreen(onOpenDetails = { navigator.push(Details(it)) })
            Browse -> DiscoverScreen()
            Updates -> LibraryScreen(...)
            More -> SettingsScreen()
            is Details -> DetailsScreen(route.contentId, onBack = { navigator.pop() })
        }
    }
}
```

- 底栏/rail 仍由 `MiyomuAppShell` 决定；
- 详情页时，`Details` 映射到 `AppDestination.LIBRARY`，所以 Library 保持高亮；
- `MiyomuNavHost` 目前只渲染当前路由，后续再加转场/状态保存。

## 5. 返回处理

`MiyomuBackHandler` 是 `expect/actual`：

| 平台 | 实现 |
| --- | --- |
| Android | `androidx.activity.compose.BackHandler` |
| Desktop | 暂空，页面内返回按钮 |
| iOS | 暂空，页面内返回按钮；后续接边缘手势 |

Android 上在 Details 按系统返回会 pop，而不是退出 App。

## 6. 验证

- `NavigatorTest`：push/pop/selectTopLevel 的栈语义。
- `ScreenshotTest`：Home 渲染仍通过。
- 三端构建：

```bash
./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :core:reader:desktopTest \
          :app:shared:linkDebugFrameworkIosSimulatorArm64 \
          --offline --no-configuration-cache
# BUILD SUCCESSFUL
```

- iOS Xcode 模拟器构建通过。

## 7. 与 Navigation Compose 的差异

| 能力 | 当前内部导航 | Navigation Compose |
| --- | --- | --- |
| 顶层切换 | ✅ | ✅ |
| push/pop | ✅ | ✅ |
| 系统返回 | Android ✅ / 其他页面按钮 | ✅ |
| 状态保存 | ⏳ | ✅ |
| 深链 | ⏳ | ✅ |
| 转场动画 | ⏳ | ✅ |
| 多 back stack | ❌ | ✅ |

## 8. 迁移计划

网络恢复后：

1. 添加 `org.jetbrains.androidx.navigation:navigation-compose:2.10.0`。
2. 用 `NavHost`/`composable(route)` 替换 `MiyomuNavHost`。
3. 保留 `MiyomuRoute` 作为路由参数模型，或改成 `@Serializable` 类型安全路由。
4. 加入 deep link、SavedStateHandle、转场。
5. 删除 `MiyomuNavigator`。

## 9. 下一步

- 详情页替换为真实 `core:data` 数据。
- Reader 路由：`Details → Reader(chapterId)`。
- 深链：`miyomu://details?source=...&id=...`。
- 桌面快捷键：Esc/Alt+Left 返回。

# 13 · Navigation

日期：2026-10-31
状态：Navigation Compose KMP 已接入

## 1. 技术选择

使用：

```text
org.jetbrains.androidx.navigation:navigation-compose:2.10.0
```

选它的原因：

- KMP 支持 Android/Desktop/iOS；
- 与 `NavHostController`、`SavedStateHandle`、Lifecycle 集成；
- 后续可以升级为类型安全路由（`@Serializable` route）；
- 不需要自己维护返回栈、系统返回、状态恢复。

> 本轮曾因 Maven 访问限制临时写过零依赖导航器；依赖恢复后已删除，当前代码使用 Navigation Compose。

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
- `key` 是字符串 route，后续可迁移到类型安全路由。

## 3. NavHost

`core:ui/navigation/MiyomuNavHost.kt`：

```kotlin
@Composable
fun MiyomuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable (MiyomuRoute) -> Unit,
) {
    NavHost(navController, startDestination = MiyomuRoute.Home.key, modifier) {
        composable(MiyomuRoute.Home.key) { content(MiyomuRoute.Home) }
        composable(MiyomuRoute.Library.key) { content(MiyomuRoute.Library) }
        composable(MiyomuRoute.Browse.key) { content(MiyomuRoute.Browse) }
        composable(MiyomuRoute.Updates.key) { content(MiyomuRoute.Updates) }
        composable(MiyomuRoute.More.key) { content(MiyomuRoute.More) }
        composable(
            route = "details/{contentId}",
            arguments = listOf(navArgument("contentId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.read { getString("contentId") }.orEmpty()
            content(MiyomuRoute.Details(id))
        }
    }
}
```

注意：KMP 的 `SavedState` 不是 Android `Bundle`，读取参数要用 `androidx.savedstate.read { getString(...) }`。

## 4. 壳层接线

```kotlin
val navController = rememberNavController()
val backStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = backStackEntry?.destination?.route

MiyomuAppShell(
    selected = currentRoute.toDestination(),
    onSelect = { navController.selectTopLevel(it) },
) {
    MiyomuNavHost(navController) { route ->
        when (route) {
            Home -> HomeScreen(onOpenDetails = { navController.openDetails(it) })
            Library -> LibraryScreen(onOpenDetails = { navController.openDetails(it) })
            Browse -> DiscoverScreen()
            Updates -> LibraryScreen(onOpenDetails = { navController.openDetails(it) })
            More -> SettingsScreen()
            is Details -> DetailsScreen(route.contentId, onBack = { navController.popBackStack() })
        }
    }
}
```

顶层切换：

```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

- 顶层切换不会堆栈；
- 详情 push；
- 返回用 `popBackStack()`；
- 详情页时 Library 保持高亮。

## 5. 返回处理

- Android：Navigation Compose 自动接管系统返回；详情页按返回会 pop。
- Desktop：详情页提供返回按钮；后续加 Esc/Alt+Left。
- iOS：详情页提供返回按钮；后续接边缘手势。

## 6. 验证

- `ScreenshotTest` 渲染 `MiyomuApp`，验证 `NavHost` 初始化 + Home 首屏。
- `:app:desktop:test` 通过。
- `:core:reader:desktopTest` 193 个测试通过。
- 三端构建：

```bash
./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :core:reader:desktopTest \
          :app:shared:linkDebugFrameworkIosSimulatorArm64
# BUILD SUCCESSFUL
```

- iOS Xcode 模拟器构建通过。

## 7. 后续

1. 详情页接真实 `core:data` 数据。
2. ~~增加 `Details → Reader(chapterId)` 路由~~ 已完成，见 [15-reader-route.md](15-reader-route.md)。
3. 深链：`miyomu://details?source=...&id=...`。
4. 迁移到类型安全路由（`@Serializable` route）。
5. Desktop 返回快捷键；iOS 边缘返回手势。
6. 共享元素/容器变换（封面 → 详情）。

## 8. 结论

Navigation 已从“手写 tab 状态”升级为 Navigation Compose KMP：

- 路由、返回栈、系统返回由库负责；
- 顶层与详情分离；
- 后续接 Reader 和深链时不需要再改壳层结构。

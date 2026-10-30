# Phase 1 探针

M0 的可丢弃验证代码，不属于正式架构。决策记录见 [02 关键决策](../../docs/02-decisions.md)。

## toolchain-probe

Android 应用，验证单轨工具链能否同时容纳：

- AGP 9.4.1 + Kotlin 2.3.21 + Compose 1.12；
- quickjs-kt 1.0.15（Kotlin 2.4 元数据）；
- mediamp 0.5.1；
- Ktor 3.6.0 + OkHttp。

```bash
./gradlew -p experiments/phase1/toolchain-probe assembleDebug
adb install -r experiments/phase1/toolchain-probe/build/outputs/apk/debug/miyomu-toolchain-probe-debug.apk
```

启动后应显示 `quickjs=3`、`ktor=200`、`player=org.openani.mediamp.MediampPlayer`。

网络行需要本机测试服务器：

```bash
python3 -c 'from http.server import BaseHTTPRequestHandler,HTTPServer
class H(BaseHTTPRequestHandler):
    def do_GET(self):
        body=b"miyomu-ok"
        self.send_response(200)
        self.send_header("Content-Length",str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def log_message(self,*a): pass
HTTPServer(("0.0.0.0",8765),H).serve_forever()'
```

模拟器通过 `10.0.2.2:8765` 访问。`usesCleartextTraffic` 仅用于探针，不进入正式应用。

## network-probe

KMP library（Android + JVM），验证 Ktor 网络层与持久 Cookie：

```bash
./gradlew -p experiments/phase1/network-probe jvmTest
./gradlew -p experiments/phase1/network-probe assemble
```

- `createMiyomuHttpClient`：HttpCookies、超时、User-Agent；Android 用 OkHttp，JVM 用 CIO。
- `FileCookiesStorage`：JSON 落盘、Max-Age/Expires、删除 Cookie、按 host 隔离。
- 8 个 JVM 测试覆盖持久化重载、过期/删除、host 隔离、损坏恢复、客户端重启后 Cookie 重发、User-Agent。

证据截图在 [evidence](evidence/)。

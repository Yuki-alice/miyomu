package app.miyomu.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

const val MiyomuUserAgent: String = "Miyomu/0.1"

fun createMiyomuHttpClient(
    cookieStorage: CookiesStorage,
    engine: HttpClientEngine = platformHttpEngine(),
): HttpClient = HttpClient(engine) {
    expectSuccess = false

    install(HttpCookies) {
        storage = cookieStorage
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }

    defaultRequest {
        header(HttpHeaders.UserAgent, MiyomuUserAgent)
    }
}

expect fun platformHttpEngine(): HttpClientEngine

package org.projectv.spike.js

fun main() {
    val server = FixtureServer(port = 18080)
    println("FIXTURE_SERVER_READY ${server.baseUrl}")
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
    try {
        Thread.sleep(Long.MAX_VALUE)
    } catch (_: InterruptedException) {
        server.close()
    }
}

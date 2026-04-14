package com.dailyback

import com.dailyback.app.bootstrap.module
import com.dailyback.app.config.AppConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val appConfig = AppConfig.fromEnvironment()
    embeddedServer(
        factory = Netty,
        host = appConfig.server.host,
        port = appConfig.server.port,
        module = {
            module(appConfig = appConfig)
        },
    ).start(wait = true)
}

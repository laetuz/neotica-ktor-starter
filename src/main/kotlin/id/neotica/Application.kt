package id.neotica

import id.neotica.application.Plugins
import id.neotica.di.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Koin must be installed before any plugin depends on injected services.
    install(Koin) {
        modules(appModule)
    }

    Plugins.installAll(this)
    Routing.configure(this)
}
package id.neotica.application

import id.neotica.AppConfig
import id.neotica.domain.model.Role
import id.neotica.domain.model.TokenPrincipal
import id.neotica.service.TokenService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get

/**
 * Application setup is split into small functions, each installing one Ktor
 * plugin. `installAll()` is called from `Application.module()`.
 */
object Plugins {

    fun installAll(app: Application) {
        installSerialization(app)
        installCallLogging(app)
        if (AppConfig.authEnabled) {
            installAuthentication(app)
        }
    }

    private fun installSerialization(app: Application) {
        app.install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private fun installCallLogging(app: Application) {
        app.install(CallLogging)
    }

    private fun installAuthentication(app: Application) {
        val tokenService: TokenService = app.get()
        app.install(Authentication) {
            jwt("auth-jwt") {
                verifier(tokenService.verifier)
                validate { credential ->
                    val userId = credential.payload.subject ?: return@validate null
                    val role = credential.payload.getClaim("role").asString()
                        ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
                        ?: Role.USER
                    TokenPrincipal(userId = userId, role = role)
                }
            }
        }
    }
}
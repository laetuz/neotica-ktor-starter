package id.neotica.di

import id.neotica.AppConfig
import id.neotica.data.repository.InMemoryNoteRepository
import id.neotica.domain.repository.NoteRepository
import id.neotica.route.HelloRoute
import id.neotica.route.NotesRoute
import id.neotica.route.PublicRoute
import id.neotica.service.AuthService
import id.neotica.service.GreetingService
import id.neotica.service.NoteService
import id.neotica.service.TokenService
import org.koin.dsl.module

/**
 * Central Koin wiring — built per application boot so the `AUTH_ENABLED` flag
 * is re-evaluated each time `Application.module()` runs (e.g. in tests).
 *
 * Greeting and notes components are always available. Auth components are
 * registered ONLY when `AUTH_ENABLED=true` — keeping the login snippet dormant
 * by default.
 */
fun appModule() = module {
    single { GreetingService() }
    single { HelloRoute(get()) }

    single<NoteRepository> { InMemoryNoteRepository() }
    single { NoteService(get()) }
    single { NotesRoute(get()) }

    if (AppConfig.authEnabled) {
        single { TokenService(secret = AppConfig.jwtSecret, issuer = AppConfig.baseUrl) }
        single { AuthService(get()) }
        single { PublicRoute(get(), get()) }
    }
}
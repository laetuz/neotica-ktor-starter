package id.neotica.di

import id.neotica.AppConfig
import id.neotica.data.InMemoryTaskRepository
import id.neotica.domain.repository.TaskRepository
import id.neotica.route.PublicRoute
import id.neotica.route.TaskRoute
import id.neotica.service.AuthService
import id.neotica.service.TaskService
import id.neotica.service.TokenService
import org.koin.dsl.module

/**
 * Central Koin wiring. Interfaces are bound to their implementations here so
 * swapping storage/auth only touches this file.
 */
val appModule = module {
    single<TaskRepository> { InMemoryTaskRepository() }
    single { TokenService(secret = AppConfig.jwtSecret, issuer = AppConfig.baseUrl) }
    single { AuthService(get()) }
    single { TaskService(get()) }

    single { PublicRoute(get(), get()) }
    single { TaskRoute(get()) }
}
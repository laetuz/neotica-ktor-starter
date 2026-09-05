# neotica-ktor-starter

A best-practice [Ktor](https://ktor.io/) backend starter, intended to demonstrate
the conventions discussed in the "Building Ktor Backends" talk. No database yet —
the data layer is an in-memory repository behind an interface, so a real
Persist/Exposed implementation can be swapped in later without touching routes.

## Stack

| Concern | Choice |
|---|---|
| Server | Ktor 3.1.x, Netty, `EngineMain` + `application.yaml` |
| Language / JVM | Kotlin 2.1.x, JVM 21 |
| DI | Koin (constructor injection) |
| JSON | kotlinx.serialization via ContentNegotiation |
| Auth | JWT (HMAC256) with a `role` claim |
| Logging | SLF4J + Logback, Ktor `CallLogging` |
| Build | Gradle version catalog (`gradle/libs.versions.toml`) |
| Tests | `ktor-server-test-host` + GitHub Actions |

## Quick start

```bash
cp .env.example .env          # fill JWT_SECRET + BASE_URL
./gradlew run                 # starts on http://localhost:8080
./gradlew test                # runs the test suite
```

## Conventions (the teaching points)

1. **Layering.** Routes live in `route/`, business logic in `service/`, contracts
   (interfaces + models) in `domain/`, implementations in `data/`, wiring in `di/`.
   `domain` never depends on `data` — swap storage by editing only `di/AppModule.kt`.
2. **Don't block the event loop.** Any blocking I/O (file, network, JDBC) goes in
   `withContext(Dispatchers.IO)`. The in-memory repository is already coroutine-safe.
3. **One error contract.** `StatusPages` maps typed exceptions to a JSON
   `ErrorResponse`. Internal `Throwable` messages are never leaked to clients.
4. **Real auth.** JWT secret comes from env (see `.env.example`), the issuer is the
   configured `BASE_URL`, and admin operations are gated by a `role` claim via
   `requireAnyRole(...)`. Never hardcode secrets or use trivial defaults.
5. **Log with SLF4J only** — no `println` in main source.
6. **Test on every change.** A `testApplication` smoke, auth, validation, and
   role-gating test ship with the template; CI runs `./gradlew build`.

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/healthz` | – | Liveness probe |
| POST | `/auth/login` | – | Body `{username,password}` → `{token, role}` |
| GET | `/tasks` | JWT | List own tasks |
| POST | `/tasks` | JWT | Create task; blank title → 400 |
| DELETE | `/tasks/{id}` | JWT **ADMIN** | Delete a task |
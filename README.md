# neotica-ktor-starter

A best-practice [Ktor](https://ktor.io/) backend starter, intended to demonstrate
the conventions discussed in the "Building Ktor Backends" talk. Kept deliberately
minimal: a newcomer-friendly greeting endpoint, a full CRUD example (`/notes`),
a thin route → service → repository split, and JWT auth that is **optional**
(off by default).

## Stack

| Concern | Choice |
|---|---|
| Server | Ktor 3.5.0, Netty, `EngineMain` + `application.yaml` |
| Language / JVM | Kotlin 2.4.0, JVM 21 |
| DI | Koin (constructor injection) |
| JSON | kotlinx.serialization via ContentNegotiation |
| Auth | JWT (HMAC256) with a `role` claim |
| Logging | SLF4J + Logback, Ktor `CallLogging` |
| Build | Gradle version catalog (`gradle/libs.versions.toml`) |
| Tests | `ktor-server-test-host` + GitHub Actions |

## Quick start

```bash
./gradlew run           # starts on http://localhost:8080 (auth off)
./gradlew test          # runs the test suite
./gradlew build         # full build + tests
```

## Endpoint usage

All responses are JSON (`kotlinx.serialization`). Errors use the unified JSON
`ErrorResponse` (`{"code": "...", "message": "..."}`).

### Directory lookup

| Endpoint | Route class | Service | Model |
|---|---|---|---|
| `/hello` | `src/main/kotlin/id/neotica/route/HelloRoute.kt` | `service/GreetingService.kt` | `domain/model/GreetingResponse.kt` |
| `/notes` | `src/main/kotlin/id/neotica/route/NotesRoute.kt` | `service/NoteService.kt` | `domain/model/Note.kt` |
| `/auth/login` | `src/main/kotlin/id/neotica/route/PublicRoute.kt` | `service/AuthService.kt` | `domain/model/Models.kt` |

### `GET /hello`

Returns a greeting. `name` is an optional query param.

```bash
curl localhost:8080/hello               # {"message":"Hello, World!"}
curl "localhost:8080/hello?name=Ktor"   # {"message":"Hello, Ktor!"}
```

Blank or missing `name` → `World`. No auth.

### Notes CRUD — `/notes`

Notes are stored in-memory (process-local; lost on restart). Currently public —
no auth required.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `GET` | `/notes` | – | `200` → `[Note, ...]` | – |
| `POST` | `/notes` | `{ "text": "..." }` | `201` → `Note` | `400` blank text |
| `PUT` | `/notes/{id}` | `{ "text": "..." }` | `200` → updated `Note` | `400` blank text · `404` unknown id |
| `DELETE` | `/notes/{id}` | – | `204` | `404` unknown id |

A `Note` looks like:

```json
{
  "id": "3f2c8a1e-...",
  "text": "buy milk",
  "createdAt": 1768670000000
}
```

Usage:

```bash
# List all notes
curl localhost:8080/notes

# Create a note
curl -X POST localhost:8080/notes \
  -H 'Content-Type: application/json' \
  -d '{"text":"buy milk"}'

# Update a note (use the id returned above)
curl -X PUT localhost:8080/notes/3f2c8a1e-... \
  -H 'Content-Type: application/json' \
  -d '{"text":"buy milk and eggs"}'

# Delete a note
curl -X DELETE -i localhost:8080/notes/3f2c8a1e-...   # 204 No Content
```

The `Note` id is generated as a UUID on create — copy it from the `POST` response.

### `POST /auth/login` (optional)

Only mounted when `AUTH_ENABLED=true`. See [Optional auth](#optional-auth).

```bash
curl -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"x"}'   # {"token":"...","role":"ADMIN"}
```

Role mapping: any username → `USER`; `admin` → `ADMIN`.

## Optional auth

The app boots with **no** env file (auth defaults to off). Set `AUTH_ENABLED=true`
in `.env` to activate the JWT snippet:

```bash
cp .env.example .env    # then set AUTH_ENABLED=true, JWT_SECRET, BASE_URL
./gradlew run

curl -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"x"}'   # {"token":"...","role":"ADMIN"}
```

## Conventions (the teaching points)

1. **Layering.** Routes live in `route/`, thin business logic in `service/`, models
   in `domain/model/`, data-access contracts in `domain/repository/`, and their
   implementations in `data/repository/`. `/notes` demonstrates the full split —
   swap `InMemoryNoteRepository` for an Exposed impl by editing only
   `di/AppModule.kt`. See `AGENTS.md` → "Database design (Exposed)" for the
   persistence blueprint.
2. **Don't block the event loop.** Any blocking I/O (file, network, JDBC) goes in
   `withContext(Dispatchers.IO)`.
3. **One error contract.** `StatusPages` maps typed exceptions to a JSON
   `ErrorResponse`. Internal `Throwable` messages are never leaked to clients.
4. **Optional, real auth.** JWT secret comes from env (see `.env.example`), the
   issuer is the configured `BASE_URL`, and role gates use `requireAnyRole(...)`.
   Never hardcode secrets or use trivial defaults.
5. **Log with SLF4J only** — no `println` in main source.
6. **Test on every change.** `testApplication` tests cover public behavior plus
   auth-disabled and auth-enabled modes; CI runs `./gradlew build`.

## Endpoints at a glance

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/hello` | – | `?name=` optional; defaults to `World` |
| GET | `/notes` | – | List all notes |
| POST | `/notes` | – | Create note |
| PUT | `/notes/{id}` | – | Update note |
| DELETE | `/notes/{id}` | – | Delete note |
| POST | `/auth/login` | – | Only when `AUTH_ENABLED=true`; `{username,password}` → `{token, role}` |
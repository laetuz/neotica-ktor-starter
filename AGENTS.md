# neotica-ktor-starter — agent guidance

## Stack
- **Ktor 3.5.0** (Netty) + **Koin** DI + **Kotlin 2.4.0** / JVM 21
- **Gradle 9.1** with version catalog (`gradle/libs.versions.toml`) — a single source of truth for all versions
- **kotlinx.serialization** JSON, **JWT auth** (com.auth0.jwt, HMAC256), **SLF4J + Logback**

## Key commands
```sh
./gradlew run           # start dev server on :8080
./gradlew test          # run all tests (ApplicationTest)
./gradlew build         # full build + tests
./gradlew shadowJar     # build fat JAR
```

## Architecture

| Layer | Location | Notes |
|---|---|---|
| Entrypoint | `Application.kt` | EngineMain; `install(Koin)` → `Plugins.installAll(this)` → `Routing.configure(this)` |
| Plugin config | `application/Plugins.kt` | Serialization, CallLogging, JWT auth (auth only when `AUTH_ENABLED=true`) |
| Routing + errors | `Routing.kt` | Installs StatusPages, mounts route classes |
| Routes | `route/` | `HelloRoute`, `NotesRoute`, `PublicRoute` — classes with `fun mount(route: Route)` |
| Services | `service/` | `GreetingService`, `NoteService`, `AuthService`, `TokenService` (business logic) |
| Contracts | `domain/repository/` | `NoteRepository` interface |
| Data impls | `data/repository/` | `InMemoryNoteRepository` (swap for Exposed later) |
| Domain contracts | `domain/` | `GreetingResponse`, `Note`, `TokenPrincipal`, login models |
| DI wiring | `di/AppModule.kt` | Greeting + notes always; auth components only when `AUTH_ENABLED=true` |

All code lives under package `id.neotica`.

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/hello` | none | Generic starter endpoint: `{"message":"Hello, World!"}`; `?name=Ktor` → `{"message":"Hello, Ktor!"}` |
| `GET` | `/notes` | none | List all notes |
| `POST` | `/notes` | none | Create note; `{ "text": "..." }`; blank → 400 |
| `PUT` | `/notes/{id}` | none | Update note; `{ "text": "..." }`; blank → 400, missing → 404 |
| `DELETE` | `/notes/{id}` | none | Delete note; missing → 404 |
| `POST` | `/auth/login` | none | Only when `AUTH_ENABLED=true`; `{username,password}` → `{token,role}` |

Role mapping: any username → `USER`; `admin` → `ADMIN`.

## Setup & env
- The app boots with NO env file (auth defaults to off). `AUTH_ENABLED` gates the JWT auth snippet:
  - `false` (default) — only `GET /hello` is mounted.
  - `true` — mounts `POST /auth/login`; requires `JWT_SECRET` and `BASE_URL`.
- `.env` loaded by custom `EnvLoader` — reads `.env` first, then `System.getenv`, then `System.getProperty` (tests use the property fallback).
- Template: `.env.example`. **NEVER commit `.env` or use real/weak secrets** (`lol` is forbidden).

## Conventions — hard rules (MUST follow)

1. **Layering** — routes never touch storage/business directly; thin services in `service/`, repository interfaces in `domain/repository/`, implementations in `data/repository/`, models in `domain/model/`. `/notes` is the reference example (route → `NoteService` → `NoteRepository` interface → `InMemoryNoteRepository`). Swapping storage = edit only `di/AppModule.kt`. When persistence lands, follow the [Database design (Exposed)](#database-design-exposed--hard-rules) section below.
2. **Events / I/O** — never block the event loop. All blocking I/O goes in `withContext(Dispatchers.IO)`.
3. **Errors** — ONE contract: throw typed exceptions; `StatusPages` maps them to JSON `ErrorResponse`. **NEVER** leak `Throwable.message` to clients on 5xx. The `status(NotFound)` handler must pass an explicit status (`call.respond(HttpStatusCode.NotFound, ...)`) — an unmatched route otherwise responds 200.
4. **Auth** — JWT is OPTIONAL (`AUTH_ENABLED`). When enabled: JWT issuer = `BASE_URL`, role claim via `requireAnyRole(...)`. `/auth/login` is only mounted under the flag.
5. **Logging** — SLF4J + CallLogging. **NEVER use `println`** in main source.
6. **Tests** — every new endpoint gets a `testApplication` test (public behavior + auth-disabled/auth-enabled modes). CI runs `./gradlew build`. Tests set env props immediately before `module()` to avoid leaking state between test classes.
7. **Commits** — `feat(x.y.z): Imperative description.` (SemVer-dotted, period-terminated).

## Database design (Exposed) — hard rules

The starter has no database yet, but the persistence blueprint below matches the
conventions used across the ecosystem (neostore, neometrics-api, orpheum-api) and
MUST be followed when a data layer is added.

### API flavor
- The ecosystem uses the **Exposed v1 legacy** API: imports `org.jetbrains.exposed.v1.*`, **NOT** `org.jetbrains.exposed.sql.*`. Match this when persistence lands.
- The modern Exposed 2.x DSL (`org.jetbrains.exposed.sql.*`) is only acceptable if the whole data layer is deliberately upgraded — **never mix** the two packages in one project.
- When persistence lands, add the Exposed v1 flavor to `gradle/libs.versions.toml` (see neostore: `exposed = 1.1.1`) alongside Postgres driver, HikariCP, and Flyway.

### Placement

| Artifact | Location |
|---|---|
| Repository interface | `domain/repository/FooRepository.kt` |
| Repository impl | `data/repository/FooRepositoryImpl.kt` |
| DAO (Table + Entity) | `data/dao/{feature}/FooTable.kt`, `FooEntity.kt` |
| Mapper (extension function) | `domain/repository/mapper/FooMapper.kt` |

### Hard rules

1. **Mappers** are extension functions (`fun AppEntity.toApp(): AppModel`) mapping DAO → domain model. DAOs never leak above the data layer.
2. **Tables** are objects: `object FooTable : UUIDTable("snake_case_plural")`. Columns are `snake_case`; mark `.nullable()` and `.uniqueIndex()` explicitly.
3. **Foreign keys** use `reference("col", OtherTable, onDelete = ReferenceOption.CASCADE | RESTRICT)` — CASCADE for owned children (versions, screenshots), RESTRICT for guarded refs (categories).
4. **Timestamps**: `long("created_at").clientDefault { Clock.System.now().toEpochMilliseconds() }` with `@OptIn(ExperimentalTime::class)`.
5. **Entities**: `class FooEntity(id) : UUIDEntity(id)` with `companion object : UUIDEntityClass<FooEntity>(FooTable)`; scalar cols via `var col by FooTable.col`; FK refs via `var other by OtherEntity referencedOn FooTable.fk`; child collections via `val children by OtherEntity.referrersOn(OtherTable.fk)`.
6. **Every repository method** wraps work in `db.dbQuery { transaction { ... } }` — the `NeoDatabase` interface (`suspend fun <T> dbQuery(block: () -> T): T`) backed by `DatabaseImpl` running `withContext(Dispatchers.IO)`. **Never** call `transaction {}` in routes/services; never block the event loop.
7. **No raw SQL**: always the Exposed DSL — `Entity.find { Table.col.eq(x) }`, `.where {}`, `.orderBy(Pair(Table.col, SortOrder.DESC))`, `.count()`, `.singleOrNull()`. **Never `exec()`**.
8. **Missing rows** surface as `null` per the repository contract, not as thrown exceptions.
9. **Migrations**: Flyway files in `src/main/resources/db/migrations/`, named `V{n}__snake_case_description.sql`, handwritten SQL, additive (V1→V9 style). A schema change = a new file; never edit an applied migration.
10. **DI wiring** in `di/AppModule.kt`: `single<NeoDatabase> { DatabaseImpl() }` and `singleOf(::FooRepositoryImpl) { bind(FooRepository::class) }`.

### Example (modeled on neostore)

```kotlin
// data/dao/app/AppTable.kt
object AppTable : UUIDTable("apps") {
    val packageName = varchar("package_name", 255).uniqueIndex()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val category = reference("category", CategoryTable, onDelete = ReferenceOption.RESTRICT)
    @OptIn(ExperimentalTime::class)
    val createdAt = long("created_at").clientDefault { Clock.System.now().toEpochMilliseconds() }
}

// data/dao/app/AppEntity.kt
class AppEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AppEntity>(AppTable)
    var packageName by AppTable.packageName
    var title by AppTable.title
    var description by AppTable.description
    var category by CategoryEntity referencedOn AppTable.category
    val versions by AppVersionEntity.referrersOn(AppVersionTable.appId)
}

// domain/repository/mapper/AppMapper.kt
fun AppEntity.toApp(): AppModel = AppModel(
    id = id.value.toString(),
    packageName = packageName,
    title = title,
    description = description,
    createdAt = createdAt,
)

// data/repository/AppRepositoryImpl.kt
class AppRepositoryImpl(private val db: NeoDatabase) : AppRepository {
    override suspend fun findByPackageName(packageName: String): AppModel? = db.dbQuery {
        AppEntity.find { AppTable.packageName.eq(packageName) }
            .singleOrNull()
            ?.toApp()
    }
}
```

## Gotchas
- Ktor **3.1.2 breaks shadow tasks on Gradle 9.1** — keep version catalog on 3.5.0 / Kotlin 2.4.0.
- `AppConfig` reads env lazily on each access, and `appModule()` is built per application boot, so tests can flip `AUTH_ENABLED` per case.
- **A leftover `.env` overrides test system properties** (EnvLoader reads `.env` first). Tests set `AUTH_ENABLED`/`JWT_SECRET`/`BASE_URL` via `System.setProperty` — so run `./gradlew test` with **no `.env` present** (or on a clean checkout). This bit us once; a committed checkout has none since `.env` is gitignored.
- `Routing` resolves routes via `app.get<T>()`, not `KoinComponent`.
- Route classes must be registered in `di/AppModule.kt` or Koin throws `NoDefinitionFoundException` at startup.
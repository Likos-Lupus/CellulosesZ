# CellulosesZ Architecture

This is the engineering architecture contract for CellulosesZ (Minecraft 26.1.2, Fabric + NeoForge,
JDK 25, Kotlin 2.4.20). It supersedes the original single-source-tree design; see
`docs/adr/0002-modular-monolith.md`.

## Shape

CellulosesZ is a **coarse-grained modular monolith**:

- One repository, one product.
- Two distributable artifacts: Fabric and NeoForge.
- Seven fixed compile-time modules that establish Kotlin `internal` boundaries and a forbidden-edge
  dependency graph.
- Two Stonecutter cells that exist only to produce the loader-specific distributions.
- One explicit composition root. No runtime module graph, no service locator, no scanning.

```text
platform/fabric  ─┐
                  ├─▶ application (composition root, root config, /cellulosesz)
platform/neoforge ┘        │
   ┌───────────────┬───────┼───────────────┬───────────────┐
   ▼               ▼       ▼               ▼               ▼
movement   communication  administration  utility   (minecraft-core, foundation)
   └───────────────┴───────┴───────────────┴───────────────┘
                           ▼
                    minecraft-core
                           ▼
                      foundation
```

## Modules

| Module           | Responsibility                                                                                                                                                                                                                                                                               | May depend on                                                                |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `foundation`     | Plain Kotlin/JVM: `AtomicFile`, `KeyedMutex`, `StorageJson`, generic `ConfigStore<T>`. Knows nothing about Minecraft.                                                                                                                                                                        | –                                                                            |
| `minecraft-core` | `RuntimeKernel`, `MinecraftServerDispatcher`, `PlayerResolver`, `Messages`, command feedback helpers, and the command declaration DSL (`command(...)` → immutable `CommandDefinition` → Brigadier) plus the Markdown help pipeline.                                                          | foundation                                                                   |
| `movement`       | teleport/home/warp/spawn/request (`/tpa`, `/tpahere`). Owns the teleport pipeline: `TeleportCoordinator`, `MinecraftTeleportBackend`, safety, pending/cooldown/history.                                                                                                                      | foundation, minecraft-core                                                   |
| `communication`  | private messaging (`/msg`, `/reply`, `/r`) with reply state, per-player preferences (`/ignore`, `/msgtoggle`), durable mail (`/mail`), staff support (`/helpop`) and announcements (`/broadcast`, `/broadcastworld`).                                                                        | foundation, minecraft-core                                                   |
| `administration` | moderation (`/kick`, `/kickall`, `/ban`, `/tempban`, `/unban`, `/banip`, `/tempbanip`, `/unbanip`, `/mute`, `/tempmute`, `/unmute`, `/muteinfo`), operator control (`/kill`, `/gamemode`, console-only `/sudo`), social spy, vanish, and player state (`/heal`, `/feed`, `/fly`, `/god`).    | foundation, minecraft-core                                                   |
| `utility`        | kits (`/kit`, `/kits`, `/showkit`, `/createkit`, `/updatekit`, `/delkit`, `/kitreset`) with a v2 catalog and durable per-player claims, item utilities (`/repair`, `/more`, `/condense`), portable workstations, and inventory inspection (`/enderchest`, view-only `/invsee`, `/disposal`). | foundation, minecraft-core                                                   |
| `application`    | Composition root, root `CellulosesConfig`, `/cellulosesz status \| reload`.                                                                                                                                                                                                                  | foundation, minecraft-core, movement, communication, administration, utility |

No feature module may depend on another feature module. `minecraft-core` and `foundation` never
depend on features. Loader APIs (`net.fabricmc.*`, `net.neoforged.*`, `net.minecraft.*` in
`foundation`) are forbidden inside `modules/**` and enforced by `verifyArchitecture`.

## Boundaries and visibility

- Every module enables `kotlin { explicitApi() }`.
- Implementations, repositories, command registrars, and file DTOs are `internal`.
- Each feature exposes a small facade (`MovementFeature`, `CommunicationFeature`,
  `AdministrationFeature`, `UtilityFeature`) plus the config types the root schema needs.
- The application never constructs a feature repository/service directly.

## Threading and coroutines

- One root coroutine owner: `RuntimeKernel` (`SupervisorJob` + `Dispatchers.Default`), cancelled on
  server stop. `RuntimeKernel.launch` runs orchestration on the default dispatcher; repositories
  switch to `Dispatchers.IO` themselves.
- Minecraft state is only touched on the server thread via `RuntimeKernel.onServerThread`.
- Filesystem I/O uses `Dispatchers.IO` (injected where practical); the server thread never blocks on
  I/O while `RUNNING`. The one deliberate exception is the bounded storage lifecycle: TOML initial
  load, Hikari connect, schema create/probe and endpoint migration run blocking during
  `SERVER_STARTING` (and pool close during `SERVER_STOPPING`) so that no game action can run before
  durable state is ready. These blocking entry points are named `*Blocking` and are only called from
  the composition root.
- Teleport transient state (pending teleports, cooldowns, requests) is also server-thread confined;
  it is plain in-memory maps with no locking.
- `ServerPlayer` is never held across suspension: capture UUID + immutable input, do the I/O, then
  re-resolve on the server thread.
- Forbidden: `GlobalScope`, production `runBlocking`, `CompletableFuture`, `Executors`,
  `ServiceLoader`, classpath scanning, reflection scanning.

## Teleport pipeline

Every CellulosesZ-initiated player move goes through exactly one path:

```text
TeleportIntent → preflight → optional delay → late destination resolve
              → safety → commit → history → cooldown → TeleportOutcome
```

- `TeleportCoordinator` is the only orchestrator. Homes, warps, spawn, `/back`, requests, and direct
  commands only describe an intent (subject, destination, cause, policy).
- `MinecraftTeleportBackend` is the only class allowed to call the Minecraft teleport API; this is
  enforced by `verifyArchitecture`.
- Expected outcomes (offline, unsafe, cooldown, cancelled, unknown dimension) are sealed values, not
  exceptions; exceptions are reserved for storage/IO/invariant faults.
- `TeleportDestination.Player` is resolved at commit time, never snapshotted at request time.

## Administration and moderation

- Minecraft's native account/IP ban lists are the single source of truth for bans; there is no
  CellulosesZ ban database, so login enforcement and `/pardon` interoperability come from vanilla.
- Mutes are persisted per player under
  `<world>/cellulosesz/administration/moderation/mutes/<uuid>.json`. A mute is published to runtime
  state only after its file is written; unmute deletes the file before unpublishing. Corrupt records
  are isolated, never overwritten, and reported rather than treated as "not muted".
- Punishment expiry uses wall-clock `Clock`/`Instant` (never monotonic gameplay time); expected
  failures are sealed outcomes.
- Cross-feature enforcement is composed in `application`: `communication` receives typed
  capabilities (`PrivateMessageSenderGate` for mutes, `PrivateMessageReachability` for vanish) and
  emits a `PrivateMessageObservation` for social spy; the public-chat gate cancels muted chat.
  `administration` never depends on `communication` or `movement`, and communication never imports
  administration.
- Vanish suppresses entity tracking via `ServerPlayerVisibilityMixin`, the only mixin (written in
  Java because Mixin's Kotlin support is incomplete); it reads a small static vanish snapshot.
  Command execution as another player (`/sudo`) is console-only and has a single dispatch path.
- `verifyArchitecture` forbids native ban access outside `MinecraftBanBackend`,
  kill/game-mode/command execution outside `MinecraftPlayerControlBackend`, and file IO in command
  files.

## Utility

- Kit definitions are server-owned catalog state under `utility/kits.json` (schema v2); each kit has
  a stable `KitId`, a `KitReusePolicy` (`Always`/`Once`/`Cooldown`) and an immutable list of
  `ItemStack` snapshots. Delivery always copies those snapshots.
- Player claim history is separate per-player state under `utility/kit-claims/<uuid>.json`. A
  cooldown/one-time claim writes a `RESERVED` record before delivery and a `DELIVERED` record after;
  a reservation is never rolled back, so a crash fails closed rather than duplicating items.
- Overflow is explicit: `REJECT` is all-or-nothing, `DROP` drops the remainder. Silent partial
  delivery is impossible.
- `KitItemCodec` encodes/decodes `ItemStack.CODEC` through a registry-aware `RegistryOps`, so item
  components (enchantments, custom data) round-trip.
- Legacy `<world>/cellulosesz/kits.json` (v1) is migrated once into `utility/kits.json` (v2) with
  the legacy file renamed to `kits.json.v1.bak`.
- Item utilities, portable workstations and inventory inspection mutate Minecraft state only through
  their backends. `/invsee` is strictly read-only. Only vanilla menu types are used, so no client
  screen code is required.
- `verifyArchitecture` confines `StorageJson`/`AtomicFile` in `utility` to the two kit repositories
  and live kit inventory mutation to `MinecraftKitInventoryBackend`.

## Config

- Configuration is TOML at `<config-dir>/cellulosesz/cellulosesz.toml`. The default file is a
  packaged, commented template (`application/src/main/resources/defaults/cellulosesz.toml`) written
  once on first boot; a test asserts the template decodes to the Kotlin defaults.
- `foundation.ConfigStore<T>` is format-agnostic (a `ConfigDecoder<T>`; `TomlConfigCodec` for TOML).
  It starts uninitialized; `current` throws until `initialLoadBlocking()` succeeds, so no feature
  ever runs against an unloaded config. Reload decodes and validates the whole candidate and
  validates the transition before publishing; a failed reload keeps the previous snapshot.
- There is no config `schemaVersion`: new fields are defaults, unknown keys are rejected.
- Root schema (`CellulosesConfig`) lives in `application`; feature settings types live with their
  owners and validate their own subtree, including the `[database]` section
  (`foundation.database.DatabaseSettingsValidation`).

## Persistence

- Machine business state is stored through JDBC + HikariCP (`foundation.database.DatabaseRuntime`).
  Backends: SQLite (default, single-connection pool), H2, MySQL, MariaDB, PostgreSQL — each with its
  own independent `[database.<backend>]` TOML section; only `database.type` is initialized.
- Fixed `cz_*` tables carry a `namespace` column (never a per-namespace table name). Repositories
  own all SQL; commands and services never import `java.sql`, and only foundation knows Hikari.
- The schema is created with `CREATE TABLE IF NOT EXISTS` and verified with a compatibility probe;
  there is no schema-version migration in this pre-release project.
- Changing the active endpoint (backend/path/host/database/schema/namespace) triggers a cold-start
  migration copied table-by-table in a single target transaction, guarded by
  `cz_storage_migration_journal`. The previous endpoint is recorded in
  `<world>/cellulosesz/.storage/last-successful.json`. The source is read-only and is never deleted;
  a non-empty target without a matching journal record is refused. A password or pool change
  reconnects without moving data.
- `AtomicFile` is retained only for the default TOML template and the `last-successful` sidecar.
  Corrupt rows raise a typed `*DataException` and are never silently repaired.
- Player identity is a durable index (`cz_players`/`cz_player_names`) hydrated into the in-memory
  resolver at boot; command handling never queries the database for identity.

## Permissions and health

- Authorization has a single seam: `core.permission.PermissionBridge.test(source, spec)` wrapped by
  `PermissionService`; commands check `source.hasPermission(permissions, spec)`. The node catalog is
  `CommandPermissions` (`cellulosesz.command.<primary>`). Undefined nodes fall back to the spec's
  `VanillaPermissionFallback`, so installing no permission manager changes nothing.
- Fabric integrates via `fabric-permissions-api`; NeoForge registers every node with the NeoForge
  `PermissionAPI` on `PermissionGatherEvent.Nodes`. LuckPerms is a compatibility target through
  those APIs; common modules never import a loader or `net.luckperms.*`. Non-player sources
  (console/RCON)
  use the vanilla fallback.
- `ApplicationHealth` carries an explicit bootstrap state (UNINITIALIZED → CONFIG_READY →
  BOOTSTRAPPING_STORAGE → STORAGE_READY → LOADING_STATE → READY → STOPPING → STOPPED, or FAILED)
  plus a secret-free `summary()`. A fatal storage failure never reaches READY.
- Bootstrap order at `SERVER_STARTING`: initialize the pool (fail fast), create/probe schema,
  migrate if the endpoint changed, hydrate the identity cache, then load feature state.
  `/cellulosesz status`
  prints the health summary; it never prints passwords or JDBC credentials.

## Text and localization

- The text foundation lives in `minecraft-core`: `core.text` (keys, `MessageTheme`, `TextSettings`)
  and `core.text.i18n` (canonical `LanguageId`, bundled `TranslationCatalog`, the tiny template
  compiler, locale policy, `LocalizedMessages`). The Adventure platform bridge is confined to
  `core.text.adventure` (`AdventureRuntime`, feedback helpers).
- Adventure (`adventure-api` 4.26.1, `adventure-platform-mod` 6.9.0) is the rich-text output. Both
  loaders nest the self-contained Adventure platform mod as Jar-in-Jar; no Adventure classes are
  flattened (`inspectArtifacts` enforces this).
- Locale policy: explicit per-player preference → configured server default; console uses the server
  default. The Minecraft client locale is not consulted. Overrides are stored in
  `cz_player_languages`, hydrated at startup, and **never queried on the render path**.
- Ordinary messages use a two-role template: plain text is `primary`, `[[...]]` marks a `secondary`
  span, `{0}`, `{1}`, ... are positional arguments. Bundled UTF-8 `.properties` live under
  `cellulosesz/i18n`; `en_us` is the baseline and every locale must match its key and per-key
  placeholder sets. Argument values are bound literally and never re-parsed.
- The legacy `Messages.prefixed/raw` API remains for incremental migration; new localized code uses
  `MessageKey` + `LocalizedMessages`.
- Commands are declared with the immutable `command(...)` DSL in `core.command.dsl`; the
  `BrigadierCompiler` rebuilds a native Brigadier tree on every registration event and aliases are
  compiled as equivalent trees. Commands never carry a second string grammar, and `suggests {}` is
  only legal inside an argument scope (`@CommandDslMarker`).
- Long command documentation is a separate pipeline: CommonMark resources under each module's
  `cellulosesz/help/<lang>/...` are parsed with commonmark-java and rendered to Adventure. Short
  command feedback keeps using the `{0}`/`[[...]]` template parser; Markdown never interprets those.
- Vanilla `/help` is selectively enhanced: for a query that names a CellulosesZ command, the
  paginated Markdown document is shown; every other query (including no-argument `/help`) is
  delegated unchanged to the saved vanilla executor. The overlay uses only public Brigadier merge
  behaviour — no Mixin and no second help command.

## Side and environment

- CellulosesZ is a **server-side utility that is client-loadable**. The Fabric metadata declares
  `"environment": "*"` and NeoForge loads on both physical sides, so the jar may sit in a client's
  `mods/` folder without crashing.
- There is **no client UI and no client-only logic**. All functionality is registered on server-side
  Architectury events (`LifecycleEvent.SERVER_*`, `CommandRegistrationEvent`,
  `PlayerEvent.PLAYER_QUIT`), and world paths are resolved lazily. Consequently the mod is active on
  any server instance (dedicated or integrated) and completely inert on a client that is not
  hosting.
- No `EnvType`/`Dist` guard is required; the server-event boundary is the gate. If a future feature
  needs one, it belongs in `application` (or the platform layer), never in `modules/**`.

## Build and distribution

- `settings.gradle.kts` includes the seven `modules/*` projects and creates the two Stonecutter
  cells. Build-tool plugin versions are hoisted into the settings `plugins {}` block (from
  `gradle.properties`) so the whole build shares one plugin classloader.
- Loader cells compile only `platform/<loader>/` and flatten each internal module's compiled output
  into the distribution jar. Loader/API mods are never bundled. CellulosesZ's own runtime libraries
  (HikariCP, tomlkt, the five JDBC drivers, and — on Fabric — fabric-permissions-api) ship as
  controlled nested/Jar-in-Jar dependencies (never flattened), so SQLite native resources and
  service metadata survive. NeoForge additionally ships the Kotlin runtime as Jar-in-Jar.
- Artifacts: `versions/<cell>/build/libs/cellulosesz-<version>+26.1.2-<loader>.jar`.

## Verification

| Task                      | Purpose                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `verifyArchitecture`      | Source rules, foundation import ban, loader import ban, mixin-package/loader-shim Java allowlist, single teleport path, single ban/control path, command file IO ban, JDBC ownership (repositories/infrastructure only, Hikari in foundation only), Adventure platform locality (`core/text/adventure` only), utility kit inventory single path, command-declaration coverage (unique `command(...)` names, every `documentation` id has `en_us`+`zh_cn` Markdown, no orphan help files) and README command-catalog freshness. |
| `checkModuleDependencies` | Compile-time project-dependency allowlist.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `checkModules`            | Runs every module's `check` (including unit tests, incl. SQLite/H2 storage tests) once.                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `chiseledBuild`           | Builds both loader distributions.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `inspectArtifacts`        | Flattened classes present; loader/Minecraft classes absent; both cells carry the Hikari/sqlite/tomlkt nested runtimes; NeoForge Jar-in-Jar intact.                                                                                                                                                                                                                                                                                                                                                                             |

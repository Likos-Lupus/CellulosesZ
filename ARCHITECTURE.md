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

| Module           | Responsibility                                                                                                                                                          | May depend on              |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `foundation`     | Plain Kotlin/JVM: `AtomicFile`, `KeyedMutex`, `StorageJson`, generic `ConfigStore<T>`. Knows nothing about Minecraft.                                                   | –                          |
| `minecraft-core` | `RuntimeKernel`, `MinecraftServerDispatcher`, `PlayerResolver`, `Messages`, command feedback helpers.                                                                   | foundation                 |
| `movement`       | teleport/home/warp/spawn/request (`/tpa`, `/tpahere`). Owns the teleport pipeline: `TeleportCoordinator`, `MinecraftTeleportBackend`, safety, pending/cooldown/history. | foundation, minecraft-core |
| `communication`  | messaging (`/msg`, `/reply`) and conversation state.                                                                                                                    | foundation, minecraft-core |
| `administration` | moderation (`/heal`, `/feed`, `/kick`) and player state (`/fly`, `/god`).                                                                                               | foundation, minecraft-core |
| `utility`        | kits and item utilities (`/repair`).                                                                                                                                    | foundation, minecraft-core |
| `application`    | Composition root, root `CellulosesConfig`, `/cellulosesz status                                                                                                         | reload`.                   | foundation, minecraft-core, movement, communication, administration, utility |

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
  I/O.
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

## Config

- `foundation.ConfigStore<T>` is generic: decode candidate, validate the whole candidate, then
  atomically publish; a failed reload keeps the previous snapshot and generation.
- Root schema (`CellulosesConfig`) lives in `application`; feature settings types
  (`MovementSettings` with its `homes`/`teleport`/`requests`/`history` children,
  `MessagingSettings`) live with their owners, and each owner validates its own subtree.
- JSONC with comments and trailing commas; unknown keys rejected; defaults encoded.

## Persistence

- Config: `<config-dir>/cellulosesz/cellulosesz.jsonc`.
- Data: `<world>/cellulosesz/` — `movement/` (`homes/<uuid>.json`, `warps.json`, `spawn.json`,
  `teleport-history/<uuid>.json`) and `kits.json`.
- Writes go through `AtomicFile` (temp file + fsync + atomic move); per-file serialization uses
  `KeyedMutex` or a repository `Mutex`.
- Corrupt machine data raises a typed `*DataException` and is never silently overwritten.

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
  into the distribution jar. External mods are never bundled; NeoForge ships the Kotlin runtime as
  Jar-in-Jar.
- Artifacts: `versions/<cell>/build/libs/cellulosesz-<version>+26.1.2-<loader>.jar`.

## Verification

| Task                      | Purpose                                                                                       |
|---------------------------|-----------------------------------------------------------------------------------------------|
| `verifyArchitecture`      | Source rules, foundation import ban, loader import ban, Java allowlist, single teleport path. |
| `checkModuleDependencies` | Compile-time project-dependency allowlist.                                                    |
| `checkModules`            | Runs every module's `check` (including unit tests) once.                                      |
| `chiseledBuild`           | Builds both loader distributions.                                                             |
| `inspectArtifacts`        | Flattened classes present; externals absent; NeoForge Jar-in-Jar intact.                      |

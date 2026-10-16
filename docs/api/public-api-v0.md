# CellulosesZ Public API v0 (experimental)

The API is a small, read-only contract for mods that want to integrate with CellulosesZ. It is an
enhancement, not a requirement: mods that do not use it coexist through the compatibility guarantees
described in ADR 0014/0017/0018.

> Status: experimental (`major = 0`). Fabric publishes the provider today; NeoForge discovery is
> pending a target-version spike (see ADR 0016), so NeoForge is not release-ready yet.

## Depending on it

```kotlin
repositories {
    mavenLocal() // during development; a release repository in production
}

dependencies {
    compileOnly("top.likoslupus:cellulosesz-api:0.1.0-SNAPSHOT")
}
```

The API classes ship inside the CellulosesZ mod jar. **Do not** bundle or shade `cellulosesz-api`
into your own mod, and do not add your own copy to `mods/`. Declare CellulosesZ as a required or
optional Loader mod dependency so the classes are present at runtime.

## Model

- `ApiQuery<T>` is the result of every read: `Available(value)`, `NotReady` or `Stopping`. An
  `Available(null)` from `PlayerLookup.findById` means "known to be unknown", which is different
  from
  `NotReady`.
- Reads are valid only while `availability()` is `READY`. Each server session has its own API view:
  once a session stops, its view returns `Stopping` **forever** — a later session never revives it.
  Do not cache an API handle across a world change; obtain it again from the provider after the next
  ready signal.
- A `whenReady` listener is invoked **at most once per session**, after the session is ready. The
  callback may run on any thread (the thread that makes the session ready, or your own if it is
  already ready), so never assume the server thread; schedule any world access on the server thread
  and do not trigger another teleport from inside a callback. A listener that throws is logged in
  isolation and never affects the provider or other listeners.

## Obtaining the API (Fabric)

```kotlin
FabricLoader.getInstance().objectShare.whenAvailable("cellulosesz:api-v0") { key, value ->
    val provider = value as? CellulosesZApiProvider ?: return@whenAvailable
    provider.whenReady { api -> /* store api; it is ready for this session */ }
}
```

## Obtaining the API (NeoForge)

NeoForge exposes the provider as an FML extension point on the `cellulosesz` `ModContainer`. A
consumer detects CellulosesZ by mod id first (so a build without it never links the shim class), then
reads the extension:

```java
if (!ModList.get().isLoaded("cellulosesz")) return;

ModList.get().getModContainerById("cellulosesz")
        .flatMap(container ->
            container.getCustomExtension(CellulosesZApiExtension.class)
        )
        .ifPresent(extension ->
            extension.provider()
                    .whenReady(api -> { /* ... */ })
        );
```

`CellulosesZApiExtension` is a thin NeoForge-only shim bundled in the CellulosesZ NeoForge jar. It
is the one loader-specific compile type; the `CellulosesZApiProvider` it returns is the same
pure-JVM contract as Fabric. Do not shade it into your mod. `getCustomExtension` returns `Optional`;
the `isLoaded` guard keeps the shim out of the class-loading path when CellulosesZ is absent.

The `whenAvailable` callback may run on an arbitrary thread; treat it as a signal only and forward
to your own server-thread bootstrap before touching world state.

## Lifecycle

| `ApiAvailability` | Meaning                                                      |
|-------------------|--------------------------------------------------------------|
| `NOT_READY`       | The mod is loaded but no session is ready yet                |
| `READY`           | A server session is ready; reads return `Available`          |
| `STOPPING`        | The session is shutting down; reads return `Stopping`        |
| `STOPPED`         | The session ended; session-scoped subscriptions are released |

`Subscription.close()` is idempotent and safe from any thread. Subscriptions are session-scoped; a
new world yields a new ready signal and new subscriptions.

## What v0 deliberately excludes

- No write/request API (no `requestTeleport`). Any future write API needs caller identity,
  permission checks and an audit design first.
- No messaging, mute, economy, moderation or storage access.
- `VisibilityLookup.canSee` answers CellulosesZ's own rule only; it is not a global "hidden players"
  list and says nothing about other mods' rendering.

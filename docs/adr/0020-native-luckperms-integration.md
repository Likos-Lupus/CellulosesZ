# 0020 — Native LuckPerms integration (optional, diagnostics-first)

- **Status**: Accepted
- **Date**: 2026-10-16
- **Related**: `core/permission/CommandPermissions`, `platform/common`, ADR 0014, ADR 0016

## Context

CellulosesZ already resolves every permission through the loader-standard bridge: the Fabric
Permissions API (`me.lucko:fabric-permissions-api`) and the NeoForge `PermissionAPI`. Both of these
transparently delegate to LuckPerms when it is installed, so command authorization is already
correct with or without LuckPerms.

Operators still need to answer "which backend decided this, and what did LuckPerms itself think?".
That requires reading the native LuckPerms API — a value that the standard bridge never exposes.

## Decision

Add a **native, optional, diagnostics-only** LuckPerms integration.

1. **The platform bridge stays the sole authority.** Every command and business check keeps calling
   `PermissionService.allows(...)` → `PermissionBridge.test(...)`. The native LuckPerms value is
   reported *alongside* the effective decision and never used to override it (no second parallel
   OR/AND).
2. **`compileOnly` and never bundled.** `net.luckperms:api` is a `compileOnly` dependency of each
   distribution cell; it is not nested, shaded or relocated. A server without LuckPerms never links
   the adapter because the adapter class is only instantiated after the loader confirms the mod is
   present (`FabricLoader.isModLoaded("luckperms")` / `ModList.get().isLoaded("luckperms")`).
3. **Loader-neutral adapter in `platform/common`.** The adapter uses only `net.luckperms.api`
   (loader-independent) plus our own data types; the only loader-specific part is the detection
   check in each `PlatformServices`. `net.luckperms.` imports are banned in `modules/**` by
   `verifyArchitecture`, so the adapter cannot leak into a business module.
4. **Immutable snapshots, no live references.** `PermissionInspector.inspect(...)` reads the current
   online context per call and returns an immutable `PermissionInspection`; it holds no
   `ServerPlayer` or world reference and never blocks.
5. **Capability-gated.** Because the native path is unverified against a running LuckPerms instance
   in this environment, the native capability is not advertised as supported until the plan's
   live-server (L1) matrix passes. Until then it is best-effort diagnostics.

## Consequences

- `cellulosesz.command.cellulosesz.permissions` (moderator) exposes
  `/cellulosesz permissions status` and `/cellulosesz permissions check <node>`. `check` accepts
  only catalogued nodes, so it cannot probe other mods.
- A raw LuckPerms `undefined` is surfaced distinctly from `deny`; the effective decision (with the
  vanilla fallback) is shown separately.
- Adding a derived cache later (invalidated by `UserDataRecalculateEvent` on the server thread) is
  possible without changing the public API; today no such cache exists, so the event subscription is
  a no-op placeholder closed on shutdown.

## Not decided here

- Any write access to LuckPerms (setting groups/nodes/meta). CellulosesZ is not a second permission
  editor.
- Exposing `net.luckperms.*` types through `:modules:api`. The public API stays pure JVM.

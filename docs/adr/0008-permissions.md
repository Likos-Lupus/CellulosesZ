# ADR 0008: Permissions

- Status: Accepted
- Date: 2026-10-09

## Context

Commands previously checked a single capability, `canUseModeratorCommands()`, which mapped directly
to `Permissions.COMMANDS_MODERATOR`. There was no way for a third-party permission manager
(LuckPerms) to grant or deny individual CellulosesZ commands, and no stable node catalog.

## Decision

1. **CellulosesZ owns a stable node catalog** in `core.permission.CommandPermissions`:
   `cellulosesz.command.<primary-literal>`. Every command has a node, including player commands
   (fallback `ALLOW_ALL`), so an admin can explicitly deny one later.
2. **Undefined means vanilla, never denied.** A `PermissionSpec` carries a
   `VanillaPermissionFallback`. When no provider defines the node, the fallback reproduces exactly
   the pre-P0 vanilla behaviour.
3. **A single seam:** `PermissionBridge.test(source, spec)` is the only loader/provider entry point.
   `PermissionService` wraps it, and commands check `source.hasPermission(permissions, spec)`.
   Common modules never import a loader or third-party permission API.
4. **Fabric** uses `fabric-permissions-api`
   (`me.lucko.fabric.api.permissions.v0.Permissions.check`), which transparently supports LuckPerms
   and other providers and falls back to vanilla otherwise. The API jar is nested in the Fabric
   distribution.
5. **NeoForge** registers every node with the NeoForge `PermissionAPI` on
   `PermissionGatherEvent.Nodes`; the node default resolver reproduces the vanilla fallback. A
   LuckPerms NeoForge provider supplies values automatically. Non-player sources always use the
   vanilla fallback (console/RCON are never denied for lack of a player UUID).
6. **`PlatformServices`** (constructed in the thin platform shim) carries the `PermissionBridge` and
   loader name into the composition root; there is no reflection or service locator.
7. **LuckPerms is a compatibility target, not a hard dependency.** Common code never links
   `net.luckperms.*`; compatibility is achieved through the two platform APIs above.
8. **A per-module `CommandDescriptor` catalog** (`CommandDescriptor`/`CommandCategory` in
   `core.command`; `MovementCommandCatalog`, `CommunicationCommandCatalog`,
   `AdministrationCommandCatalog`, `UtilityCommandCatalog`) binds each top-level literal to its
   `PermissionSpec`, category and player-only flag. `CellulosesZCommandCatalog` (application)
   aggregates them plus the `/cellulosesz` root. The catalogs are declarative metadata and register
   nothing.

## Consequences

- Installing no permission manager changes nothing.
- `verifyArchitecture` forbids feature command files from importing loader/third-party permission
  APIs; they must go through CellulosesZ authorization helpers.
- `verifyArchitecture` also compares the per-module catalogs against the actual
  `dispatcher.register` calls: a registered command without a descriptor (or a descriptor without a
  registration) fails the build. Aliases (`/r`) are covered by their primary descriptor.
- The node catalog and the aggregated command catalog are testable as plain data (uniqueness,
  lowercase, `cellulosesz.` prefix, one descriptor per command node).

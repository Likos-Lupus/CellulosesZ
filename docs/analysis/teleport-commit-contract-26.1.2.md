# Teleport commit contract (Minecraft 26.1.2)

Verified against the mapped/deobfuscated 26.1.2 artifacts in the local Gradle/Loom cache
(`minecraft-merged-deobf` for vanilla/Fabric and `neoforge-26.1.2.112-minecraft-merged-deobf` for
NeoForge), not against older documentation.

## `ServerPlayer.teleport(TeleportTransition)`

Signature (`javap`):

```
public net.minecraft.server.level.ServerPlayer teleport(net.minecraft.world.level.portal.TeleportTransition)
```

The method returns a `ServerPlayer`, **not a boolean**, and it is nullable in practice. Decompiled
control flow:

- **Vanilla / Fabric**: returns `null` only when `this.isRemoved()`; otherwise returns `this`.
- **NeoForge**: first calls `net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(entity,
  newLevel.dimension())` and returns `null` when that cancellable hook vetoes the travel (the check
  runs for same-dimension moves too), then returns `null` when `isRemoved()`, otherwise `this`.

Both same-dimension and cross-dimension paths return `this` at the end; the method is synchronous
and completes on the calling thread.

### Consequences for CellulosesZ

- A **non-null** return means the move was applied. The applied position is read from the returned
  player (`level().dimension()`, `position()`, `yRot/xRot`), which is what
  `BackendMoveResult.Success`
  now carries. The requested/`resolved` position is never used as the receipt.
- A **null** return means the move was definitely not committed (a platform veto and/or a removed
  player). It maps to `BackendMoveResult.RejectedByPlatform` → `TeleportOutcome.RejectedByPlatform`,
  which records no history and consumes no cooldown.
- The null-on-cancel signal is **loader-dependent**: only NeoForge surfaces a cancellable
  travel-to-dimension veto through the return value. On Fabric, a mod that cancels higher up cannot
  be observed through this method, so CellulosesZ does not claim to detect it; it reports what the
  platform's own call reported. This is why the plan's optional `CommitUnverified`/"indeterminate"
  branch is unnecessary here: the verified native contract gives a definite non-null/ null answer.

## Atomicity boundary

Because the call is synchronous and side-effecting, `MinecraftTeleportBackend.move` performs the
final safety resolution and the teleport call inside one `kernel.onServerThread { }` block with no
suspension between them. A pre-commit veto (P1-C `TeleportPreflight`) runs immediately before the
call, on the same server thread, and can only allow or deny — never suspend, retry or rewrite the
destination.

# Vanilla `/tp` takeover spike — Minecraft 26.1.2

Status: **partial / not implemented.** `tp` remains `TakeoverSupport.CANDIDATE`, so
`[commands.vanillaOverrides].commands = ["tp"]` is rejected at startup. `/cztp` and
`/cellulosesz:tp` are available regardless.

## Evidence gathered (deobfuscated client/server merged jar, `minecraft-merged-deobf-26.1.2.jar`)

- Target class: `net.minecraft.server.commands.TeleportCommand`.
- Single entry point: `public static void register(CommandDispatcher<CommandSourceStack>)`.
- `javap -c` shows the method's constant pool loads two literals in order:
    - `#7  // String teleport`
    - `#104 // String tp`
      It registers the `teleport` node first and then registers `tp`. In vanilla the second call is
      a
      `redirect` to the first node, so `tp` and `teleport` share one executor and argument tree.

## Consequence

A Mixin at `HEAD` of `TeleportCommand.register` cannot suppress only `tp`: it cancels the whole
method, which would also remove `teleport`. Suppressing only `tp` needs a targeted injection at the
second `dispatcher.register` call — for example
`@At(value = "CONSTANT", args = "stringValue=tp")` on the `register` invocation, or a MixinExtras
`@ModifyExpressionValue` / `@Redirect` scoped to that call. The inverse mistake (removing
`teleport`) is exactly the regression the compatibility plan forbids.

## Decision

- Do **not** enable `tp` takeover. `VanillaCommandCatalog.TP.takeoverSupport == CANDIDATE`, and
  `ConfigValidation` rejects configuring it.
- Keep the protected `/cztp` fallback and the `/cellulosesz:tp` namespaced route (both implemented
  and unit-tested).
- Before promoting `TP` to `VERIFIED`, deliver a Mixin that suppresses only the `tp` registration,
  plus an integration test proving `teleport` still executes vanilla syntax.

## Still required before `tp` takeover can ship

1. Confirm via bytecode which `dispatcher.register` call is `tp` and that it is a redirect.
2. A targeted Mixin (or equivalent) that leaves `teleport` intact — verified on Fabric and NeoForge.
3. A route-level permission (`cellulosesz.command.override.tp`, already declared) so taking over the
   vanilla entry never widens an ordinary player's access.
4. Live-server tests for `/tp <player>`, `/teleport ...`, `/execute as ... run tp`, command blocks
   and functions.

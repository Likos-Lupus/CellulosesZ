# /cellulosesz permissions

Read-only permission diagnostics: which backend resolves CellulosesZ nodes and, for a specific node,
the effective decision plus any native provider opinion.

## Subcommands

- `/cellulosesz permissions` or `/cellulosesz permissions status`: show the active permission
  backend and whether a native LuckPerms provider is present.
- `/cellulosesz permissions check <node>`: inspect one CellulosesZ permission node.

## Behavior

The effective decision always comes from the platform permission API (Fabric Permissions API or the
NeoForge PermissionAPI); the platform bridge is the only thing that authorizes anything. When a
native LuckPerms provider is loaded, the command additionally shows the provider's raw tri-state
opinion (`allow`, `deny` or `undefined`) for the player's current context. A raw `undefined` is not
a denial: the platform still applies the vanilla fallback.

`check` accepts only nodes declared in the CellulosesZ catalog, so it can't be used to probe other
mods' nodes.

## Notes

Moderators only. Only node names, the effective boolean and the raw opinion are printed; player IPs,
chat and credentials never are.

## Example

- `/cellulosesz permissions`
- `/cellulosesz permissions check cellulosesz.command.tp`

## Related

- `/cellulosesz status`
- `/cellulosesz commands`

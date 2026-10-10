# CellulosesZ command baseline (Minecraft 26.1.2)

Snapshot of the pre-DSL command surface, captured before migrating the explicit Brigadier
registration to the `command(...) { ... }` declaration DSL. It exists so the migration can be
checked for behavioural regressions instead of only structural ones.

## Top-level commands (70 primaries + `/r` alias)

- **Movement (20):** `sethome`, `home`, `delhome`, `homes`, `back`, `tp`, `tphere`, `tppos`, `tpa`,
  `tpahere`, `tpaccept`, `tpdeny`, `tpcancel`, `warp`, `warps`, `setwarp`, `delwarp`, `spawn`,
  `setspawn`, `delspawn`.
- **Communication (8):** `msg`, `reply` (alias `r`), `msgtoggle`, `ignore`, `mail`, `helpop`,
  `broadcast`, `broadcastworld`.
- **Administration (21):** `kick`, `kickall`, `ban`, `tempban`, `unban`, `banip`, `tempbanip`,
  `unbanip`, `mute`, `tempmute`, `unmute`, `muteinfo`, `kill`, `gamemode`, `sudo`, `heal`, `feed`,
  `fly`, `god`, `socialspy`, `vanish`.
- **Utility (20):** `kit`, `kits`, `showkit`, `createkit`, `updatekit`, `delkit`, `kitreset`,
  `repair`, `more`, `condense`, `enderchest`, `disposal`, `invsee`, `workbench`, `anvil`,
  `grindstone`, `stonecutter`, `loom`, `cartographytable`, `smithingtable`.
- **Root (1):** `cellulosesz` with `status`, `reload`, `language [list|server|<language>]`.

## Source-accurate differences to preserve (not "fix") during migration

- `/mail` root/`read`/`clear` are player-only via executor feedback; `send`/`sendtemp` remain
  available to non-player sources that pass the integration gate.
- `/createkit` and `/updatekit` accept an argument that is directly executable and also carries
  `once` / `cooldown <duration>` branches.
- `/tp <first>` and `/tp <first> <second>` are two different execution paths; the second keeps its
  existing extra authorisation inside the handler.
- `/reply` = alias `/r` with a greedy message tail.
- `/sudo` is exposed only to non-player sources (Brigadier requirement), independent of the handler.
- `/cellulosesz` root is `ALLOW_ALL`; `status`/`reload` are moderator sub-nodes; `language` is
  `ALLOW_ALL`.
- `/tp`, `/kill`, `/gamemode` share a root literal with Vanilla. Their registration **merges** with
  the existing node; ownership for help must be verified against the current dispatcher, not assumed
  from the name.

## Known metadata/behaviour gaps recorded at freeze time

Some player commands carried a `CommandPermissions` node in the catalog but did **not** yet attach a
matching `.requires` in the registered tree. Applying the DSL makes those nodes effective. This is a
deliberate correctness change (an explicit LuckPerms `DENY` will now be honoured) and must be called
out in the release notes rather than presented as a no-op refactor.

## `/help` baseline

See `docs/analysis/vanilla-help-26.1.2.md`. `/help`, `/help give`, `/help time` and `/help unknown`
must remain byte-for-byte Vanilla; only queries that resolve to a CellulosesZ-owned executable path
use the Markdown documentation.

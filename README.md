# CellulosesZ

[![Stars](https://img.shields.io/github/stars/Likos-Lupus/CellulosesZ?style=flat-square&label=Stars&labelColor=444444&color=eac54f)](https://github.com/Likos-Lupus/CellulosesZ/)
[![Release](https://img.shields.io/github/v/release/Likos-Lupus/CellulosesZ?style=flat-square&labelColor=444444&label=Release&include_prereleases)](https://github.com/Likos-Lupus/CellulosesZ/releases)
[![GitHub CI](https://img.shields.io/github/actions/workflow/status/Likos-Lupus/CellulosesZ/ci.yml?style=flat-square&labelColor=444444&branch=master&label=GitHub%20CI)](https://github.com/Likos-Lupus/CellulosesZ/actions/workflows/ci.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-22ff84?style=flat-square&labelColor=444444)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-eac54f?style=flat-square&labelColor=444444)]()
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-f16436?style=flat-square&labelColor=444444)]()

A **Kotlin-first server utility mod** for **Minecraft 26.1.2**, built as a single product for both
**Fabric** and **NeoForge**.

CellulosesZ provides the everyday quality-of-life commands a survival or SMP server needs — homes,
teleports, warps, spawn, messaging and light moderation — with safe, crash-resistant persistence and
a strict server-thread model. It is a from-scratch rewrite and shares no code, config or data format
with the legacy CellulosesZ or with EssentialsX.

## Features

- **Homes** — `/sethome`, `/home`, `/delhome`, `/homes` with per-player, per-UUID JSON storage.
- **Teleport requests** — `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel` with
  direction-aware requests, a per-target queue limit, lazy expiry, and cleanup when a player
  disconnects.
- **Direct teleports** — `/back`, `/tp`, `/tphere`, `/tppos`, all routed through the same teleport
  pipeline as homes, warps, spawn and requests.
- **Safe teleports** — destinations are validated against collisions, fluids, build height and the
  world border, with an optional delay that cancels on movement or damage.
- **Private messaging** — `/msg`, `/reply` and `/r` with session reply state, plus per-player
  preferences (`/ignore`, `/msgtoggle`) that persist across restarts.
- **Mail** — `/mail`, `/mail read`, `/mail send`, `/mail sendtemp`, `/mail clear`: durable,
  offline-capable messaging with bounded mailboxes, expiry and per-sender rate limiting.
- **Staff communication** — `/helpop` for staff support, and moderator `/broadcast` /
  `/broadcastworld`.
- **Warps** — `/warp`, `/warps`, `/setwarp`, `/delwarp` as server-global named positions.
- **Spawn** — `/spawn`, `/setspawn`, `/delspawn` with a configurable spawn and vanilla fallback.
- **Kits** — `/kit`, `/kits`, `/showkit`, `/createkit`, `/updatekit`, `/delkit`, `/kitreset`; a v2
  catalog with a per-kit reuse policy (unlimited / one-time / cooldown), durable per-player claims,
  and explicit all-or-nothing or drop-overflow delivery.
- **Moderation** — `/heal`, `/feed`, `/kick`.
- **Player state** — `/fly`, `/god`.
- **Item utilities** — `/repair [hand|all] [player]`, `/more [amount]`, `/condense`.
- **Portable workstations** — `/workbench`, `/anvil`, `/grindstone`, `/stonecutter`, `/loom`,
  `/cartographytable`, `/smithingtable` (disabled by default).
- **Inventory inspection** — `/enderchest`, `/disposal`, and read-only `/invsee <player>`.
- **Admin root** — `/cellulosesz status`, `/cellulosesz reload`.

## Design highlights

- **Dual-loader, first-class** — one codebase, two Stonecutter distribution cells.
- **Server-thread safe** — Minecraft state is only mutated on the server thread; no `ServerPlayer`
  is ever held across a suspension.
- **One teleport path** — every player move goes through a single `TeleportCoordinator`; only the
  teleport backend may call the Minecraft teleport API, enforced by `verifyArchitecture`.
- **Crash-safe persistence** — every durable write goes through a temp file, `fsync` and an atomic
  move; corrupt machine data is never silently overwritten.
- **Strict, typed config** — JSONC with comments and trailing commas, unknown keys rejected, and a
  transactional reload that keeps the previous snapshot on failure.
- **No runtime magic** — no reflection scanning, no service locator, no runtime module graph.

## Requirements

- **Minecraft**: `26.1.2`
- **Java**: `25`
- **Fabric**: Fabric Loader `0.19.5+`, Fabric API `0.155.3+26.1.2`, Fabric Language Kotlin
  `1.14.1+kotlin.2.4.20`
- **NeoForge**: `26.1.2.112+` (the Kotlin runtime is bundled Jar-in-Jar — KotlinForForge is **not**
  required)
- **Architectury API**: `20.1.16+`

## Installation

1. Install **Minecraft 26.1.2** with either **Fabric** or **NeoForge**.
2. Install the matching **Architectury API** for your loader.
3. Download the loader-specific jar from
   the [Releases](https://github.com/Likos-Lupus/CellulosesZ/releases)
   page:
    - `cellulosesz-<version>+26.1.2-fabric.jar`
    - `cellulosesz-<version>+26.1.2-neoforge.jar`
4. Place it in your server's `mods/` directory and start the server.

CellulosesZ is a **server-side** mod; players do not need to install it.

## Environment / Side

CellulosesZ ships as a **client-loadable, server-gated** mod: the jar declares Fabric
`"environment": "*"`, so it loads on both physical sides, but all functionality hangs off
server-side events. It therefore:

- is **fully active** on a dedicated server, and on the integrated server of a singleplayer/LAN
  world;
- is **inert** on a client that is merely connected to a remote server (no commands, no I/O).

For singleplayer/LAN on Fabric, install the jar alongside **Fabric API**, **Fabric Language Kotlin**
and **Architectury API**, since `environment: "*"` makes them hard dependencies on the client. On
NeoForge the same jar loads on both sides; the Kotlin runtime is bundled, so no extra dependency is
needed there. This mod adds **no client UI**.

## Commands

| Command                                        | Permission     | Description                                    |
|:-----------------------------------------------|:---------------|:-----------------------------------------------|
| `/sethome [name]`                              | Player         | Save the current position as a home.           |
| `/home [name]`                                 | Player         | Teleport to a saved home.                      |
| `/delhome <name>`                              | Player         | Delete a saved home.                           |
| `/homes`                                       | Player         | List your homes.                               |
| `/tpa <player>`                                | Player         | Request to teleport to another player.         |
| `/tpahere <player>`                            | Player         | Request a player to teleport to you.           |
| `/tpaccept [player]`                           | Player         | Accept a pending teleport request.             |
| `/tpdeny [player]`                             | Player         | Deny a pending teleport request.               |
| `/tpcancel`                                    | Player         | Cancel your outgoing teleport request.         |
| `/back`                                        | Player         | Return to your previous location.              |
| `/tp <player>`                                 | Player         | Teleport to another player.                    |
| `/tp <player> <target>`                        | Moderator      | Teleport a player to another player.           |
| `/tphere <target>`                             | Moderator      | Teleport a player to you.                      |
| `/tppos <x> <y> <z> [dim]`                     | Moderator      | Teleport to coordinates.                       |
| `/msg <player> <message>`                      | Player         | Send a private message.                        |
| `/reply <message>`                             | Player         | Reply to the last private message.             |
| `/r <message>`                                 | Player         | Alias of `/reply`.                             |
| `/ignore`                                      | Player         | List ignored players.                          |
| `/ignore add <player>`                         | Player         | Stop receiving private messages from a player. |
| `/ignore remove <player>`                      | Player         | Reverse `/ignore add`.                         |
| `/msgtoggle`                                   | Player         | Show your private-message receive state.       |
| `/msgtoggle on`                                | Player         | Enable receiving private messages.             |
| `/msgtoggle off`                               | Player         | Disable receiving private messages.            |
| `/mail`                                        | Player         | Show a mailbox summary.                        |
| `/mail read [page]`                            | Player         | Read and mark mail as read.                    |
| `/mail send <player> <message>`                | Player/Console | Send durable mail.                             |
| `/mail sendtemp <player> <duration> <message>` | Player/Console | Send expiring mail.                            |
| `/mail clear`                                  | Player         | Delete all your mail.                          |
| `/helpop <message>`                            | Player/Console | Contact online moderators.                     |
| `/broadcast <message>`                         | Moderator      | Announce to all players.                       |
| `/broadcastworld <dimension> <message>`        | Moderator      | Announce to one dimension.                     |
| `/warp <name>`                                 | Player         | Teleport to a warp.                            |
| `/warps`                                       | Player         | List warps.                                    |
| `/setwarp <name>`                              | Moderator      | Create or update a warp.                       |
| `/delwarp <name>`                              | Moderator      | Delete a warp.                                 |
| `/spawn`                                       | Player         | Teleport to the configured spawn.              |
| `/setspawn`                                    | Moderator      | Set the spawn to your current position.        |
| `/delspawn`                                    | Moderator      | Reset the spawn to vanilla.                    |
| `/kit <name>`                                  | Player         | Claim a kit.                                   |
| `/kits`                                        | Player         | List kits with per-player availability.        |
| `/showkit <name>`                              | Player         | Preview a kit's contents.                      |
| `/createkit <name> [once\|cooldown <dur>]`     | Moderator      | Create a kit from your inventory.              |
| `/updatekit <name> [once\|cooldown <dur>]`     | Moderator      | Update a kit, preserving its reuse history.    |
| `/delkit <name>`                               | Moderator      | Delete a kit.                                  |
| `/kitreset <name> [player]`                    | Moderator      | Reset a kit's cooldown / used state.           |
| `/heal [player]`                               | Moderator      | Restore health.                                |
| `/feed [player]`                               | Moderator      | Restore hunger.                                |
| `/kick <player> [reason]`                      | Moderator      | Disconnect a player.                           |
| `/fly [player]`                                | Moderator      | Toggle flight.                                 |
| `/god [player]`                                | Moderator      | Toggle invulnerability.                        |
| `/repair [hand\|all] [player]`                 | Moderator      | Repair the held or all items.                  |
| `/more [amount]`                               | Moderator      | Fill the held stack (never oversized).         |
| `/condense`                                    | Player         | Condense items into compact blocks.            |
| `/enderchest`                                  | Player         | Open your ender chest.                         |
| `/disposal`                                    | Player         | Open a temporary trash menu.                   |
| `/workbench`, `/anvil`, …                      | Player         | Open a portable workstation (if enabled).      |
| `/invsee <player>`                             | Moderator      | View a player's inventory (read-only).         |
| `/cellulosesz status`                          | Moderator      | Show runtime state and config generation.      |
| `/cellulosesz reload`                          | Moderator      | Reload the configuration transactionally.      |

Moderator commands use the vanilla Minecraft 26.1.2 permission `COMMANDS_MODERATOR`.

## Configuration

The config file is created on first start at:

```text
<config-dir>/cellulosesz/cellulosesz.jsonc
```

It is JSONC (comments and trailing commas allowed), strictly validated, and rejects unknown keys.
Per-world data is stored under:

```text
<world>/cellulosesz/
├─ movement/
│  ├─ homes/<uuid>.json
│  ├─ warps.json
│  ├─ spawn.json
│  └─ teleport-history/<uuid>.json
├─ communication/
│  ├─ preferences/<uuid>.json
│  └─ mail/<uuid>.json
├─ administration/
│  └─ moderation/
│     ├─ mutes/<uuid>.json
│     └─ audit/<utc-day>/...
└─ utility/
   ├─ kits.json
   └─ kit-claims/<uuid>.json
```

A legacy `<world>/cellulosesz/kits.json` (schema v1) is migrated once into `utility/kits.json`
(schema v2); the legacy file is renamed to `kits.json.v1.bak`.

## Building from Source

The project uses [Stonecutter](https://stonecutter.kikugie.dev/)
and [Architectury Loom](https://github.com/architectury/architectury-loom)
with a single shared set of compile-time modules. Build both loader distributions:

```bash
# Linux / macOS
./gradlew chiseledBuild

# Windows (PowerShell / Command Prompt)
.\gradlew.bat chiseledBuild
```

Built jars are written to:

```text
versions/26.1.2-fabric/build/libs/cellulosesz-<version>+26.1.2-fabric.jar
versions/26.1.2-neoforge/build/libs/cellulosesz-<version>+26.1.2-neoforge.jar
```

### Verification tasks

| Task                      | Purpose                                                                                                        |
|:--------------------------|:---------------------------------------------------------------------------------------------------------------|
| `verifyArchitecture`      | Source rules, foundation import ban, loader import ban, Java allowlist, communication repository IO ownership. |
| `checkModuleDependencies` | Compile-time module dependency allowlist.                                                                      |
| `checkModules`            | Unit tests of every architecture module, run once.                                                             |
| `chiseledBuild`           | Builds both loader distributions.                                                                              |
| `inspectArtifacts`        | Asserts flattened classes, no bundled externals, NeoForge Jar-in-Jar.                                          |

CI runs all of the above on JDK 25.

### Development runs

The two distribution cells are `:26.1.2-fabric` and `:26.1.2-neoforge`:

```bash
./gradlew :26.1.2-fabric:runServer
./gradlew :26.1.2-neoforge:runServer
```

## Documentation

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — the architecture contract and module boundaries.
- [`docs/adr/`](docs/adr/) — architecture decision records.

## Acknowledgements

CellulosesZ's feature set is **inspired by** the long-standing problem domain that mods such as
**[EssentialsX](https://essentialsx.net/)** have served for years — homes, warps, spawn, teleport
requests, private messaging and light moderation. The EssentialsX team and community deserve credit
for establishing what a mature Minecraft server utility mod is expected to offer.

That is the full extent of the relationship. CellulosesZ is an independent, from-scratch
implementation and deliberately provides:

- **no EssentialsX API or command compatibility**,
- **no reading or writing of EssentialsX userdata or configuration**,
- **no import/export or migration path**,
- **no reuse of EssentialsX source code**.

EssentialsX is referenced here only as prior art for the problem space, never as an implementation
or compatibility target.

## License

CellulosesZ is licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).

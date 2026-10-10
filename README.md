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
- **Localized messages** — server-side translations (`en_us`, `zh_cn`) with `/cellulosesz language`
  and a per-player preference; Adventure-rendered primary/secondary colors. CellulosesZ language
  controls CellulosesZ-owned text only, not the player's Minecraft client language.
- **Admin root** — `/cellulosesz status`, `/cellulosesz reload`.

## Design highlights

- **Dual-loader, first-class** — one codebase, two Stonecutter distribution cells.
- **Server-thread safe** — Minecraft state is only mutated on the server thread; no `ServerPlayer`
  is ever held across a suspension.
- **One teleport path** — every player move goes through a single `TeleportCoordinator`; only the
  teleport backend may call the Minecraft teleport API, enforced by `verifyArchitecture`.
- **Crash-safe persistence** — every durable write goes through a temp file, `fsync` and an atomic
  move; corrupt machine data is never silently overwritten.
- **Strict, typed config** — TOML with comments, unknown keys rejected, and a transactional reload
  that keeps the previous snapshot on failure.
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

<!-- BEGIN COMMAND CATALOG -->

| Command               | Access    | Documentation                  |
|:----------------------|:----------|:-------------------------------|
| `/back`               | Player    | `movement/back`                |
| `/delhome`            | Player    | `movement/delhome`             |
| `/delspawn`           | Moderator | `movement/delspawn`            |
| `/delwarp`            | Moderator | `movement/delwarp`             |
| `/home`               | Player    | `movement/home`                |
| `/homes`              | Player    | `movement/homes`               |
| `/sethome`            | Player    | `movement/sethome`             |
| `/setspawn`           | Moderator | `movement/setspawn`            |
| `/setwarp`            | Moderator | `movement/setwarp`             |
| `/spawn`              | Player    | `movement/spawn`               |
| `/tp`                 | Player    | `movement/tp`                  |
| `/tpa`                | Player    | `movement/tpa`                 |
| `/tpaccept`           | Player    | `movement/tpaccept`            |
| `/tpahere`            | Player    | `movement/tpahere`             |
| `/tpcancel`           | Player    | `movement/tpcancel`            |
| `/tpdeny`             | Player    | `movement/tpdeny`              |
| `/tphere`             | Moderator | `movement/tphere`              |
| `/tppos`              | Moderator | `movement/tppos`               |
| `/warp`               | Player    | `movement/warp`                |
| `/warps`              | Player    | `movement/warps`               |
| `/broadcast`          | Moderator | `communication/broadcast`      |
| `/broadcastworld`     | Moderator | `communication/broadcastworld` |
| `/helpop`             | Player    | `communication/helpop`         |
| `/ignore`             | Player    | `communication/ignore`         |
| `/mail`               | Player    | `communication/mail`           |
| `/msg`                | Player    | `communication/msg`            |
| `/msgtoggle`          | Player    | `communication/msgtoggle`      |
| `/reply` (alias `/r`) | Player    | `communication/reply`          |
| `/ban`                | Moderator | `administration/ban`           |
| `/banip`              | Moderator | `administration/banip`         |
| `/feed`               | Moderator | `administration/feed`          |
| `/fly`                | Moderator | `administration/fly`           |
| `/gamemode`           | Moderator | `administration/gamemode`      |
| `/god`                | Moderator | `administration/god`           |
| `/heal`               | Moderator | `administration/heal`          |
| `/kick`               | Moderator | `administration/kick`          |
| `/kickall`            | Moderator | `administration/kickall`       |
| `/kill`               | Moderator | `administration/kill`          |
| `/mute`               | Moderator | `administration/mute`          |
| `/muteinfo`           | Moderator | `administration/muteinfo`      |
| `/socialspy`          | Moderator | `administration/socialspy`     |
| `/sudo`               | Moderator | `administration/sudo`          |
| `/tempban`            | Moderator | `administration/tempban`       |
| `/tempbanip`          | Moderator | `administration/tempbanip`     |
| `/tempmute`           | Moderator | `administration/tempmute`      |
| `/unban`              | Moderator | `administration/unban`         |
| `/unbanip`            | Moderator | `administration/unbanip`       |
| `/unmute`             | Moderator | `administration/unmute`        |
| `/vanish`             | Moderator | `administration/vanish`        |
| `/anvil`              | Player    | `utility/anvil`                |
| `/cartographytable`   | Player    | `utility/cartographytable`     |
| `/condense`           | Player    | `utility/condense`             |
| `/createkit`          | Moderator | `utility/createkit`            |
| `/delkit`             | Moderator | `utility/delkit`               |
| `/disposal`           | Player    | `utility/disposal`             |
| `/enderchest`         | Player    | `utility/enderchest`           |
| `/grindstone`         | Player    | `utility/grindstone`           |
| `/invsee`             | Moderator | `utility/invsee`               |
| `/kit`                | Player    | `utility/kit`                  |
| `/kitreset`           | Moderator | `utility/kitreset`             |
| `/kits`               | Player    | `utility/kits`                 |
| `/loom`               | Player    | `utility/loom`                 |
| `/more`               | Moderator | `utility/more`                 |
| `/repair`             | Moderator | `utility/repair`               |
| `/showkit`            | Player    | `utility/showkit`              |
| `/smithingtable`      | Player    | `utility/smithingtable`        |
| `/stonecutter`        | Player    | `utility/stonecutter`          |
| `/updatekit`          | Moderator | `utility/updatekit`            |
| `/workbench`          | Player    | `utility/workbench`            |
| `/cellulosesz`        | Player    | `core/cellulosesz`             |

<!-- END COMMAND CATALOG -->

Each command has a stable permission node (`cellulosesz.command.<command>`). When a node is
undefined it falls back to the exact vanilla behaviour (`ALLOW_ALL` for player commands,
`COMMANDS_MODERATOR`
for moderator commands), so installing no permission manager changes nothing. On Fabric the nodes
are served through `fabric-permissions-api` (LuckPerms-compatible); on NeoForge through the NeoForge
`PermissionAPI`.

The catalog above and the per-command documentation are generated from the command declarations.
Every command has an `en_us` and `zh_cn` Markdown document; `/help <command>` shows it (paginated
with `--page N`) while plain `/help` and every non-CellulosesZ query keep vanilla behaviour.

## Configuration and storage

The config file is created on first start at:

```text
<config-dir>/cellulosesz/cellulosesz.toml
```

It is TOML, strictly validated, and rejects unknown keys. The active storage backend is selected by
`[database].type` — `sqlite` (default), `h2`, `mysql`, `mariadb`, or `postgresql`; each backend has
its own `[database.<backend>]` section and only the active one is initialized. Business data lives
in fixed `cz_*` tables (with a `namespace` column) rather than files:

```text
cz_players, cz_player_names            # durable player identity
cz_homes, cz_warps, cz_spawn, cz_teleport_history
cz_messaging_preferences, cz_ignored_players, cz_mailboxes, cz_mail_messages
cz_mutes, cz_moderation_audit, cz_moderation_audit_details
cz_kits, cz_kit_items, cz_kit_claims
cz_storage_migration_journal
```

The SQLite database defaults to `<world>/cellulosesz/cellulosesz.db`. Changing the active endpoint
(backend, path, host/database/schema, or namespace) migrates data on the next cold start; the
previous endpoint is remembered in `<world>/cellulosesz/.storage/last-successful.json`, the source
is never deleted, and a password or pool change reconnects without moving data.
`/cellulosesz reload`
cannot hot-swap the active database — a change to it requires a restart.

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

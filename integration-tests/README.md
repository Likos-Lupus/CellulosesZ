# Compatibility integration tests

This directory describes the **live-server** compatibility scenarios for CellulosesZ and the
artifacts they produce. The Gradle task `compatSmokeBothLoaders` runs the *L1-structural* subset
(built-jar assertions) on every build; the scenarios here require a real Fabric/NeoForge dedicated
server and are run on a runner that can install the loaders (nightly / pre-release).

## Levels

| Level         | Where                                    | What it proves                                                                                       |
|---------------|------------------------------------------|------------------------------------------------------------------------------------------------------|
| L0            | `./gradlew checkModules`                 | Pure JVM logic: route planning, config transition, jail policy, teleport coordinator, preflight gate |
| L1-structural | `./gradlew compatSmokeBothLoaders`       | Built distribution shape: loader metadata, mixin configs, no flattened MC classes                    |
| L1-live       | this directory, a dedicated server       | Startup, command routes, `/me` modes, `/help`, reload contract                                       |
| L2            | this directory + locked third-party mods | Land-claim/teleport veto, display-name/vanish interaction                                            |

## Layout

```
integration-tests/
  README.md
  fixtures/          # minimal, same-shape fixture mods (Fabric + NeoForge)
  scenarios/         # machine-readable scenario definitions
  expected/          # report schema
  scripts/           # runner + assertions
```

## Running a live scenario

1. Install the target loader and Minecraft 26.1.2 in a clean directory; copy the built
   `versions/26.1.2-<loader>/build/libs/cellulosesz-*.jar` into `mods/`.
2. Add only the fixtures declared by the scenario. Never add a copy of a `cellulosesz-api` jar — the
   API classes ship inside the CellulosesZ jar.
3. Accept the EULA **only inside this sandbox** and start the server; wait for a verifiable
   `Done`/ready signal, not a fixed sleep.
4. Drive the asserted behaviour (a fixture command, a `/me` variant, a cancelled teleport) and
   collect the server log plus a sorted command-route snapshot.
5. Emit a report in the shape of `expected/compat-report.schema.json` with `PASS`/`FAIL` and, when a
   locked third-party mod has no build for this Minecraft version, `NOT_AVAILABLE` (never `PASS`).

## Honesty rules

- A structural pass is **not** a claim of runtime compatibility.
- `NOT_AVAILABLE` is used only for third-party mods without a matching build; L1 fixtures must
  always run.
- Reports must not contain player UUIDs, chat text, IP addresses or database credentials.

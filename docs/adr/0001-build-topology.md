# ADR 0001 — Single source tree with two Stonecutter loader cells

> **Status: Superseded by [ADR 0002](0002-modular-monolith.md).**
> The single-`src/common` compilation unit was replaced by a coarse-grained modular monolith; the
> two Stonecutter loader cells remain as the distribution axis.

## Context

CellulosesZ targets both Fabric and NeoForge for Minecraft 26.1.2 from one codebase. It is written
in Kotlin and is expected to grow by feature, not by Gradle module. Minecraft 26.x ships
deobfuscated, which changes what a build tool must do for the loader variants.

## Decision

- Keep **one shared source tree** (`src/common/kotlin`) and grow by bounded-context packages.
- Model the loader split as **two Stonecutter cells** (`26.1.2-fabric`, `26.1.2-neoforge`), each
  with its own `build.<loader>.gradle.kts`, not as common/loader Gradle modules.
- Use **Architectury Loom 1.17.491 (no-remap)** for both cells, with `loom.platform` injected per
  cell from `settings.gradle.kts` because the cells share the root `gradle.properties`.
- Ship the Kotlin runtime to NeoForge users as **Jar-in-Jar**; Fabric users get it via Fabric
  Language Kotlin.

## Alternatives

- Many Gradle modules (`api`, `core`, `common`, `fabric`, `neoforge`, one per feature) — rejected:
  creates module graph and dependency indirection without independent release artifacts.
- A single Fabric-first build with a second port — rejected: the two loaders are first-class from
  the first commit.
- Remapping Loom — rejected: unnecessary outside legacy obfuscated distributions.

## Consequences

- Features cannot accidentally depend on loader APIs; `src/common/java` is forbidden.
- Loader-specific code is confined to `src/fabric`, `src/neoforge`, and (rarely) source-level
  Stonecutter conditions.
- Both artifacts are produced by a single `chiseledBuild`.

## Rollback

A future Minecraft version becomes a new Stonecutter cell rather than a change to the existing ones.
If a loader split must become a published artifact, it can be extracted into a Gradle module then.

## Validation

`verifyArchitecture` enforces the layout rules; `chiseledBuild` builds and tests both cells; CI
inspects both artifacts, including the NeoForge Jar-in-Jar payload.

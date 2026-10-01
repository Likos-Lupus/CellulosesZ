# ADR 0002 — Coarse-grained modular monolith with Stonecutter distribution cells

## Context

ADR 0001 put all shared Kotlin in one `src/common` compilation unit and treated packages as the
architectural boundary. That is insufficient: Kotlin's `internal` is a **module/compilation**
boundary, not a package boundary, so every "internal" implementation stayed visible to all other
packages. The original codebase also showed the opposite failure mode: 18 Gradle modules plus a
runtime module graph and a service registry.

CellulosesZ is one product that ships exactly two artifacts (Fabric and NeoForge). It needs real
compile-time boundaries without reintroducing runtime pluggability.

## Decision

Adopt a **coarse-grained modular monolith**:

- Fixed compile-time modules under `modules/`: `foundation`, `minecraft-core`, `movement`,
  `communication`, `administration`, `utility`, `application`.
- `foundation` is plain Kotlin/JVM and knows nothing about Minecraft, Architectury, or loaders.
- Minecraft-aware modules use Architectury Loom (no-remap) as **common** projects; loader APIs are
  forbidden inside `modules/**`.
- `application` is the only composition root and the only module allowed to depend on every feature.
- Feature modules depend only on `foundation` + `minecraft-core`; no feature-to-feature edges.
- The two Stonecutter cells (`26.1.2-fabric`, `26.1.2-neoforge`) are **distribution only**: they
  compile `platform/<loader>/` and flatten the internal module outputs into the distribution jar.
- No runtime module graph, no service locator, no classpath scanning, no dynamic enable/disable.
- Each feature module exposes a minimal public facade (plus its config types); everything else stays
  `internal`. `explicitApi()` is enabled on every module.

## Alternatives

- Keep one shared compilation unit (ADR 0001) — rejected: `internal` provides no real boundary, and
  implementations silently become shared API as the codebase grows.
- One Gradle module per feature (`:home`, `:warp`, …) — rejected: module explosion without
  independent artifacts; features like home/warp/spawn/tpa form one stable movement bounded context.
- Runtime module system with discovery — rejected: the exact complexity the rewrite removes.
- Separate Gradle module per loader with a published `common` artifact — rejected: two fixed cells
  are enough for a single product.

## Consequences

- Cross-module access must be an explicit `public` facade decision.
- `verifyArchitecture` and `checkModuleDependencies` enforce source rules and the dependency
  allowlist, and fail the build on violations.
- Tests live with their owner module and run once, not once per loader cell.
- The distribution jars contain flattened internal classes; external mods (Fabric API, Architectury,
  FLK) are never bundled, and NeoForge keeps the Kotlin runtime as Jar-in-Jar.
- A future second Minecraft version becomes a new Stonecutter cell / overlay decision, not a
  duplication of the architecture modules.

## Rollback

If a bounded context ever produces an independent release artifact, it can be published from its
existing module without restructuring the rest. If the module boundary proves wrong, merge modules;
the dependency allowlist is the single place to change.

## Validation

`verifyArchitecture`, `checkModuleDependencies`, `checkModules`, `chiseledBuild`, and
`inspectArtifacts` (which asserts flattened classes, absent external classes, and the NeoForge
Jar-in-Jar payload) all run in CI.

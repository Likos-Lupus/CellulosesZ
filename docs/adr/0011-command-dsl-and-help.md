# ADR 0011: Command declaration DSL, selective `/help` and Markdown documentation

## Status

Accepted.

## Context

CellulosesZ has ~70 top-level commands registered as hand-written Mojang Brigadier trees, with a
separate hand-maintained `CommandDescriptor` catalog per feature. That duplication drifts: the
catalog's `playerOnly`/`permission` metadata is not guaranteed to match the registered tree, command
literals and usage are copied into the README by hand, and there is no per-command documentation.

## Decision

1. **One declaration per command.** Commands are declared with a small Kotlin DSL:
   `command(name, category, permission, documentation, aliases, sourceAccess) { syntax }`. Root
   metadata lives on the call; the block only declares nodes, arguments, completions and executors.
   There is no second command-grammar string parser and no dynamic command framework.
2. **Immutable definitions compiled to real Brigadier.** `command(...)` produces a validated,
   immutable `CommandDefinition`; `BrigadierCompiler` rebuilds the native Brigadier tree on every
   registration event from the definition, so no node is bound to a stale dispatcher. Aliases are
   compiled as equivalent trees from the same definition; Brigadier `redirect` is not used.
3. **Typed argument keys.** `argument(name, word()/quotedString()/greedyString()/integer()/double()/
   dimension())` exposes an `ArgumentKey<T>` whose typed `get` is only valid on the executing path.
   `@CommandDslMarker` keeps `suggests {}` legal only inside an argument scope.
4. **Permissions stay in one place.** Commands reference the existing `CommandPermissions` nodes;
   the compiler turns them into `PermissionService` predicates. `SourceAccess.PLAYER/NON_PLAYER`
   expresses real source restrictions (for example console-only `/sudo`). Undefined nodes still fall
   back to exact vanilla behaviour.
5. **Selective `/help`.** `/help` remains the vanilla command through a public-API overlay: a
   `help` literal re-registered without its own executor (merge preserves the vanilla no-argument
   executor) plus a same-named `command` greedy child whose executor is replaced. For a query that
   names a CellulosesZ command, a paginated Markdown document is shown; every other query is
   delegated unchanged to the saved vanilla executor. No Mixin is used.
6. **Markdown documentation.** Each command has an `en_us` and `zh_cn` CommonMark document under its
   module's `cellulosesz/help` resources. Documents are parsed with commonmark-java and rendered to
   Adventure; short command feedback keeps using the existing `{0}`/`[[...]]` template parser. The
   two pipelines never share syntax.

## Consequences

- The hand-written `*CommandCatalog` objects and `CellulosesZCommandCatalog` are removed; the
  command list is derived from the DSL declarations and the README command catalog is generated from
  them (`generateCommandCatalog`), with `verifyArchitecture` failing when the README is stale.
- `verifyArchitecture` checks that every declared `command(name = ...)` is unique and every
  `documentation` id resolves to both language resources, with no orphan help files.
- commonmark-java and the help resources ship in both loader distributions; `inspectArtifacts`
  asserts the nested commonmark runtime, the flattened help resources and no flattened
  `org/commonmark` classes.
- A number of player commands that previously had no `.requires` now carry their
  `cellulosesz.command.*`
  node (ALLOW_ALL fallback), so they become explicitly deniable under a permission manager without
  changing vanilla behaviour.

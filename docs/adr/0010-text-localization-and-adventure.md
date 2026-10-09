# ADR 0010: Text, Localization and Adventure

- Status: Accepted
- Date: 2026-10-09

## Context

CellulosesZ is a server-side mod with no matching client mod or resource pack, yet it needs
translatable, styled command feedback. Messages were previously rendered with a thin vanilla
`Messages.prefixed/raw` helper and had no localization, no color roles, and no persisted language
preference.

## Decision

1. **Adventure is the rich-text API** for the game-facing text foundation. For Minecraft 26.1.2 the
   versions are pinned: `adventure-api` 4.26.1 and `adventure-platform-mod` 6.9.0 (Fabric and
   NeoForge platform mods). The platform version is coupled to the Minecraft cell and must not be
   upgraded independently.
2. **Text ownership is `minecraft-core`.** `net.kyori.adventure.text.Component` is the stable output
   type; the platform audience bridge (`MinecraftServerAudiences`) is a runtime detail owned by the
   `core/text/adventure` package and the loader cells. Features never touch
   `net.kyori.adventure.platform.*`.
3. **CellulosesZ custom i18n is server-rendered.** Custom keys are never sent to the client. A
   bounded context selects a `LanguageId`, resolves bundled text, compiles it to a Component, and
   sends it. Adventure `GlobalTranslator`/client-locale support is deliberately not used for
   CellulosesZ-owned keys.
4. **Locale policy is server-owned:** an explicit persisted per-player preference wins, otherwise
   the configured server default; console/RCON use the server default. The Minecraft client locale
   is not consulted in this phase.
5. **Ordinary messages use a tiny two-role template language**, not Markdown and not MiniMessage:
   plain text is `primary`; `[[...]]` marks a `secondary` span; `{0}`, `{1}`, ... are positional
   arguments. Escapes: `\[`, `\]`, `\{`, `\}`, `\\`. Templates are compiled once at bootstrap.
6. **Arguments are structural and literal.** A template is compiled before arguments are bound;
   argument values become literal component text and are never re-parsed, so user input cannot gain
   formatting semantics.
7. **Translation resources are UTF-8 `.properties`** under
   `cellulosesz/i18n/messages_<language>.properties`, loaded from an explicit `BundledTranslations`
   manifest (no classpath scanning). `en_us` is the mandatory baseline; every locale must match its
   key set and per-key placeholder set (enforced by a pure-JVM test).
8. **Colors are semantic config, not translation content:** `[text.colors] primary/secondary` are
   strict `#RRGGBB` values; translations never contain RGB.
9. **Language preference is durable but off the render path:** an explicit override is stored in a
   dedicated `cz_player_languages` table (not on the identity row), hydrated into an in-memory cache
   at startup, and updated write-before-publish. Rendering never queries JDBC.
10. **The legacy `Messages.prefixed/raw` API remains** for incremental migration; new localized code
    uses `MessageKey` + `LocalizedMessages`.
11. **Future command-document Markdown is a separate subsystem** with its own parser and renderer;
    it may share only the final Adventure/audience infrastructure and must not call
    `MessageTemplateParser`.

## Consequences

- The distribution nests the Adventure platform mod (self-contained) as Jar-in-Jar on both loaders;
  no Adventure classes are flattened. `inspectArtifacts` enforces this.
- `/cellulosesz` is player-accessible; `status` and `reload` keep their own moderator nodes and
  `language` is available to all players.
- CellulosesZ language controls CellulosesZ-owned text only; it does not change the player's
  Minecraft client or vanilla item/block/entity translations.

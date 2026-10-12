# ADR 0012: Information and presentation system

- Status: Accepted
- Date: 2026-10-12
- Extends: ADR 0010 (text/localization/Adventure), ADR 0011 (command DSL and selective `/help`)

## Context

ADR 0010 introduced a server-owned locale policy, a short-message template language (`{0}`/
`[[...]]`), an Adventure boundary and a Markdown command-document pipeline. ADR 0011 added the
command DSL and a selective vanilla `/help` overlay. What remained was a uniform way to describe,
theme, structure and deliver user-visible information across every bounded context, and to retire
the decorative `[CellulosesZ]` prefix.

## Decision

1. **Semantic model.** Add `LocalizedText(MessageKey, List<TextArgument>)`, `Notice`, `NoticeTone`,
   `NoticePriority`, a constrained `UiAction`, and the structural views `ListPage`/`DetailView`.
   They are immutable, language-free and theme-free.
2. **Typed arguments.** `TextArgument` (literal/player/identifier/command/count/elapsed/position) is
   the only way dynamic values enter a message. Values are always inserted literally and are never
   re-parsed as templates, Markdown, MiniMessage or clickable commands.
3. **Theme tokens.** Replace the two-colour `primary`/`secondary` model with a preset palette plus a
   density and optional per-token overrides. There is no legacy colour compatibility: the project is
   pre-release, so carrying a `primary` migration path would be dead code. The short-message
   `[[...]]`
   emphasis role maps to the palette `value` token, not the `secondary` body tone.
4. **Per-module resources.** Translations are loaded from explicit `TranslationSource` anchors
   (`cellulosesz/i18n/<module>/messages_<lang>.properties`); the composition root lists the five
   owners. A missing file, a duplicate key across sources, or a placeholder-parity mismatch is a
   bootstrap failure.
5. **Per-recipient rendering.** A `RenderContext` (language + immutable theme + channel) is built
   for each recipient; broadcasts, private messages and staff notices are rendered once per
   recipient, never pre-rendered then copied.
6. **Single output boundary.** `InformationOutput` sends; it keeps synchronous command
   success/failure codes separate from asynchronous delivery. Adventure delivery does not preserve
   the vanilla failure slot, so vanilla-failure semantics are preserved deliberately, not by
   accident.
7. **Prefix retirement.** `Messages.PREFIX`, `Messages.prefixed` and `renderPrefixedFor` are removed
   after migration. `Messages.raw` is retained only as a clearly-labelled vanilla-compatibility
   output for disconnect reasons and menu titles.
8. **Whole-document output and density.** A command document is rendered and sent in full (no
   pagination); `[text] density` controls only vertical spacing, inserting a blank line between
   sections and before optional elements on `comfortable`, and keeping them adjacent on `compact`.

## Consequences

- Features depend on `TextPresentation` (constructor injection), not on Adventure or translation
  catalogs directly.
- The `[text.colors]` overrides are validated as `#RRGGBB`; unspecified tokens come from the
  selected preset. Every translatable word, including duration units, position templates and
  list/quote markers, lives in the catalog; only formatting logic stays in code.
- Retiring the prefix changes user-visible output; sourced labels (private
  message/announcement/staff)
  remain.
- Successful operations with a failed audit write, or a kit delivery with dropped stacks, render as
  success plus a warning, never as a plain failure.

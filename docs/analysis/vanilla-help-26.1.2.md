# Vanilla `/help` in Minecraft 26.1.2 — compatibility reconnaissance

Verified read-only against
`minecraft-merged-deobf-26.1.2.jar` (`javap -c net.minecraft.server.commands.HelpCommand`).

## Registered tree

```
help                        (no .requires, always visible)
├── executes <command A>    root smart-usage listing
└── command                 StringArgumentType.greedyString()
    └── executes <command B> parses the query, then prints smart usage for the matched node
```

- The query is a **single greedy string** named `command`. `/help mail send` arrives as the one
  value `"mail send"`; it is our job to tokenise it. `--page N` can therefore be a trailing token we
  interpret, without registering any Brigadier node.
- No `requires` predicate is attached anywhere in the tree, so `/help` is available to every source.
- Command A: `getSmartUsage(dispatcher.root, source)` over all root children.
- Command B: `dispatcher.parse(query, source)`; if the resulting node list is empty it throws
  `SimpleCommandExceptionType(Component.translatable("commands.help.failed"))`; otherwise it prints
  `getSmartUsage(lastMatchedNode, source)` for every entry.
- Both executors capture the `CommandDispatcher` by lambda binding, so holding the `Command` object
  and calling `run(context)` preserves the original behaviour exactly.

## Can the query executor be decorated?

`com.mojang.brigadier.tree.CommandNode` (Brigadier 1.3.10 / 1.3.11) exposes a public
`getCommand()` but the `command` field is **private and has no public setter**. There is therefore
no supported way to mutate the already-registered executor in place (e.g. `CommandNode#setCommand`
does not exist, and the private `children` map must never be touched reflectively).

The supported route uses Brigadier's documented `addChild` **merge** semantics. Registering a second
`help` literal that carries **no own executor** keeps the existing command A, while a same-named
`argument("command", greedyString())` child replaces command B:

```kotlin
val original = dispatcher.root.getChild("help")!!.getChild("command")!!.command!!

dispatcher.register(
    Commands.literal("help")            // no .executes -> merge keeps command A
            .then(
                Commands.argument("command", StringArgumentType.greedyString())
                        .executes(overlay(original)) // merge replaces command B
            )
)
```

`LiteralArgumentBuilder`/`RequiredArgumentBuilder` from `Commands` are the exact types Vanilla used,
so the merge matches the existing `help` and `command` nodes by name.

## Consequences for the design

- The selective overlay is feasible with **public API only**; no Mixin is required. A narrow
  `HelpCommandMixin` remains a documented fallback only if a runtime combination proves the merge
  unsafe.
- `minecraft-merged-deobf` does **not** shade Brigadier; the compiler must resolve
  `com.mojang.brigadier:*` from the loader classpath (`brigadier-1.3.10`/`1.3.11` are present in the
  Gradle cache), which the project already does transitively.
- `CommandRegistrationEvent.EVENT` is invoked as
  `(CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection)`;
  `CommandBuildContext extends HolderLookup.Provider`, so registry-aware argument types can be built
  during command registration.

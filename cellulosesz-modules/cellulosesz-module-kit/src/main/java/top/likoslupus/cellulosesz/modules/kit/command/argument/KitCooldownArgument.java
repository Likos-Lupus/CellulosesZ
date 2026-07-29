package top.likoslupus.cellulosesz.modules.kit.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import top.likoslupus.cellulosesz.modules.kit.application.KitCooldown;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Parses a non-negative cooldown in seconds or the one-time sentinel without a stringly-typed handoff.
 */
public final class KitCooldownArgument implements ArgumentType<KitCooldown> {

    private static final Collection<String> EXAMPLES = List.of(
            "0",
            "60",
            "once",
            "one-time"
    );
    private static final LongArgumentType SECONDS = LongArgumentType.longArg(0L);

    private KitCooldownArgument() {
    }

    public static KitCooldownArgument cooldown() {
        return new KitCooldownArgument();
    }

    public static KitCooldown get(CommandContext<?> context, String name) {
        return context.getArgument(name, KitCooldown.class);
    }

    @Override
    public KitCooldown parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();
        if (token.equalsIgnoreCase("once")
                || token.equalsIgnoreCase("one-time")
        ) {
            return new KitCooldown.Once();
        }
        reader.setCursor(start);
        return new KitCooldown.Seconds(SECONDS.parse(reader));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> context,
            SuggestionsBuilder builder
    ) {
        var remaining = builder.getRemainingLowerCase();
        if ("once".startsWith(remaining)) {
            builder.suggest("once");
        }
        if ("one-time".startsWith(remaining)) {
            builder.suggest("one-time");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

}

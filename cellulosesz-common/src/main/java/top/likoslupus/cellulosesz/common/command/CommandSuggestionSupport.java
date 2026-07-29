package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class CommandSuggestionSupport {

    private CommandSuggestionSupport() {
    }

    /**
     * Returns an empty completion set when a read-only snapshot is temporarily unavailable.
     */
    public static CompletableFuture<Suggestions> suggest(
            Supplier<? extends Collection<String>> values,
            SuggestionsBuilder builder
    ) {
        requireNonNull(values, "values");
        requireNonNull(builder, "builder");

        try {
            return suggest(values.get(), builder);
        } catch (RuntimeException _) {
            return builder.buildFuture();
        }
    }

    public static CompletableFuture<Suggestions> suggest(
            Collection<String> values,
            SuggestionsBuilder builder
    ) {
        requireNonNull(values, "values");
        requireNonNull(builder, "builder");

        var remaining = builder.getRemainingLowerCase();
        values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(remaining))
                .sorted()
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

}

package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class PaymentTargetsArgument implements ArgumentType<List<String>> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(new LiteralMessage("Invalid payment target list"));
    private final int maximumTokens;

    private PaymentTargetsArgument(int maximumTokens) {
        if (maximumTokens <= 0) throw new IllegalArgumentException("maximumTokens must be positive");
        this.maximumTokens = maximumTokens;
    }

    public static PaymentTargetsArgument targets(int maximumTokens) {
        return new PaymentTargetsArgument(maximumTokens);
    }

    @SuppressWarnings("unchecked")
    public static List<String> get(CommandContext<?> context, String name) {
        return (List<String>) context.getArgument(name, List.class);
    }

    @Override
    public List<String> parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var raw = reader.readUnquotedString();
        var unique = new LinkedHashMap<String, String>();

        for (var token : raw.split(",", -1)) {
            var normalized = token.trim();
            if (normalized.isEmpty()) {
                reader.setCursor(start);
                throw INVALID.createWithContext(reader);
            }

            unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
            if (unique.size() > maximumTokens) {
                reader.setCursor(start);
                throw INVALID.createWithContext(reader);
            }
        }

        if (unique.isEmpty()) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }

        return List.copyOf(unique.values());
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("Alice", "Alice,Bob");
    }

}

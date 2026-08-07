package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class PaymentTargets {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Invalid payment target list")
    );

    private PaymentTargets() {
    }

    public static List<String> parse(
            String raw,
            int maximumTokens
    ) throws CommandSyntaxException {
        if (maximumTokens <= 0) {
            throw new IllegalArgumentException("maximumTokens must be positive");
        }

        var unique = new LinkedHashMap<String, String>();
        for (var token : raw.split(",", -1)) {
            var normalized = token.trim();
            if (normalized.isEmpty()) {
                throw INVALID.create();
            }

            unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
            if (unique.size() > maximumTokens) {
                throw INVALID.create();
            }
        }

        if (unique.isEmpty()) {
            throw INVALID.create();
        }

        return List.copyOf(unique.values());
    }

}

package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Keeps an untouched snapshot of the vanilla command roots before CellulosesZ replaces commands with the same labels.
 *
 * <p>Platform implementations must use this bridge for internal calls to
 * vanilla commands. Dispatching those calls through the live server dispatcher can re-enter a CellulosesZ command and
 * recurse indefinitely.</p>
 */
public final class FabricVanillaCommandBridge {

    private static final List<String> REQUIRED_COMMANDS = List.of(
            "clear",
            "enchant",
            "give",
            "time",
            "weather"
    );

    private volatile CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    public void capture(CommandDispatcher<CommandSourceStack> source) {
        var snapshot = new CommandDispatcher<CommandSourceStack>();
        REQUIRED_COMMANDS.stream()
                .map(label -> source.getRoot().getChild(label))
                .filter(Objects::nonNull)
                .forEach(command -> snapshot.getRoot().addChild(command));
        dispatcher = snapshot;
    }

    public OptionalInt execute(String command, CommandSourceStack source) {
        if (command.isBlank()) return OptionalInt.empty();

        var normalized = command.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank()) return OptionalInt.empty();

        try {
            return OptionalInt.of(dispatcher.execute(normalized, source));
        } catch (CommandSyntaxException _) {
            return OptionalInt.empty();
        }
    }

}

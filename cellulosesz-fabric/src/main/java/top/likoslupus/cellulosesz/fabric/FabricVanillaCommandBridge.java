package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.*;

/**
 * Keeps untouched vanilla command roots before CellulosesZ replaces commands with the same labels.
 *
 * <p>Platform implementations must use this bridge for internal calls to vanilla commands. Dispatching those calls
 * through the live server dispatcher can re-enter a CellulosesZ command and recurse indefinitely.</p>
 */
public final class FabricVanillaCommandBridge {

    private static final List<String> REQUIRED_COMMANDS = List.of(
            "ban",
            "ban-ip",
            "clear",
            "enchant",
            "give",
            "kick",
            "pardon",
            "pardon-ip",
            "time",
            "weather"
    );

    private volatile CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private volatile Map<String, CommandNode<CommandSourceStack>> roots = Map.of();

    public void capture(CommandDispatcher<CommandSourceStack> source) {
        var capturedRoots = new LinkedHashMap<String, CommandNode<CommandSourceStack>>();
        source.getRoot().getChildren().forEach(command ->
                capturedRoots.put(command.getName(), command)
        );
        roots = Map.copyOf(capturedRoots);

        var snapshot = new CommandDispatcher<CommandSourceStack>();
        REQUIRED_COMMANDS.stream()
                .map(capturedRoots::get)
                .filter(java.util.Objects::nonNull)
                .forEach(command -> snapshot.getRoot().addChild(command));
        dispatcher = snapshot;
    }

    public void restore(
            CommandDispatcher<CommandSourceStack> target,
            String label
    ) {
        root(label).ifPresent(target.getRoot()::addChild);
    }

    public Optional<CommandNode<CommandSourceStack>> root(String label) {
        return Optional.ofNullable(roots.get(label));
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

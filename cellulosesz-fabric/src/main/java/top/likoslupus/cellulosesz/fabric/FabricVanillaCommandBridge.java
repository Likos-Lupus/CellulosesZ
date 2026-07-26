package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.platform.NativeCommandResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        source.getRoot().getChildren().forEach(command -> capturedRoots.put(command.getName(), command));
        roots = Map.copyOf(capturedRoots);

        var snapshot = new CommandDispatcher<CommandSourceStack>();
        REQUIRED_COMMANDS.stream()
                .map(capturedRoots::get)
                .filter(java.util.Objects::nonNull)
                .forEach(command -> snapshot.getRoot().addChild(command));
        dispatcher = snapshot;
    }

    public void restore(CommandDispatcher<CommandSourceStack> target, String label) {
        root(label).ifPresent(target.getRoot()::addChild);
    }

    public Optional<CommandNode<CommandSourceStack>> root(String label) {
        return Optional.ofNullable(roots.get(label));
    }

    public NativeCommandResult execute(String command, CommandSourceStack source) {
        if (command.isBlank()) return NativeCommandResult.parseFailure("Command is blank");

        var normalized = command.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank()) return NativeCommandResult.parseFailure("Command is blank");

        var labelEnd = normalized.indexOf(' ');
        var label = labelEnd < 0 ? normalized : normalized.substring(0, labelEnd);
        if (!roots.containsKey(label)) {
            return NativeCommandResult.notAvailable("Native command root is unavailable: " + label);
        }

        try {
            var code = dispatcher.execute(normalized, source);
            return code > 0
                    ? NativeCommandResult.success(code)
                    : NativeCommandResult.executionFailure(code, "Native command returned a non-success result code");
        } catch (CommandSyntaxException exception) {
            return NativeCommandResult.parseFailure(exception.getRawMessage().getString());
        } catch (RuntimeException exception) {
            var message = exception.getMessage();
            return NativeCommandResult.executionFailure(
                    0,
                    message == null || message.isBlank() ? exception.getClass().getSimpleName() : message
            );
        }
    }

}

package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps untouched vanilla command roots before CellulosesZ replaces commands with the same labels.
 */
public final class FabricVanillaCommandBridge {

    private volatile Map<String, CommandNode<CommandSourceStack>> roots = Map.of();

    public void capture(CommandDispatcher<CommandSourceStack> source) {
        var capturedRoots = new LinkedHashMap<String, CommandNode<CommandSourceStack>>();
        source.getRoot().getChildren()
                .forEach(command ->
                        capturedRoots.put(command.getName(), command)
                );
        roots = Map.copyOf(capturedRoots);
    }

    public void restore(CommandDispatcher<CommandSourceStack> target, String label) {
        root(label).ifPresent(target.getRoot()::addChild);
    }

    public Optional<CommandNode<CommandSourceStack>> root(String label) {
        return Optional.ofNullable(roots.get(label));
    }

}

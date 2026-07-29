package top.likoslupus.cellulosesz.fabric.bridge;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.common.command.CommandRootMutator;
import top.likoslupus.cellulosesz.fabric.mixin.CommandNodeAccessor;

public final class FabricCommandRootMutator implements CommandRootMutator {

    @Override
    @SuppressWarnings("unchecked")
    public void remove(CommandNode<CommandSourceStack> root, String label) {
        var accessor = (CommandNodeAccessor<CommandSourceStack>) root;
        accessor.cellulosesz$children().remove(label);
        accessor.cellulosesz$literals().remove(label);
        accessor.cellulosesz$arguments().remove(label);
    }

}

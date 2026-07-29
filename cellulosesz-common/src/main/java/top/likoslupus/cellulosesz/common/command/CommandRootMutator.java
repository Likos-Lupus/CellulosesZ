package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
public interface CommandRootMutator {

    void remove(CommandNode<CommandSourceStack> root, String label);

}

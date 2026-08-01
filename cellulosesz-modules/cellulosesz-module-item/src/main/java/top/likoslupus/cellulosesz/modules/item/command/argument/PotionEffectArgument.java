package top.likoslupus.cellulosesz.modules.item.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;

import java.util.Set;
import java.util.function.Supplier;

public final class PotionEffectArgument {

    private PotionEffectArgument() {
    }

    public static ArgumentType<String> effect(Supplier<Set<String>> values) {
        return new RegistryIdArgument(values);
    }

    public static String get(CommandContext<?> context, String name) {
        return RegistryIdArgument.get(context, name);
    }

}

package top.likoslupus.cellulosesz.common.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import static java.util.Objects.requireNonNull;

public final class MinecraftItems {

    private MinecraftItems() {
    }

    public static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(requireNonNull(stack, "stack").getItem()).toString();
    }

}

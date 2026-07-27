package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({
        ShapedRecipe.class,
        ShapelessRecipe.class,
        AbstractCookingRecipe.class,
        SingleItemRecipe.class,
        SmithingTransformRecipe.class
})
public interface RecipeResultAccessor {

    @Accessor("result")
    ItemStack cellulosesz$result();

}

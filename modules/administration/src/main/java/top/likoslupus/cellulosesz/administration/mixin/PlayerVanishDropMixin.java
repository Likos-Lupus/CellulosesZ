package top.likoslupus.cellulosesz.administration.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.administration.vanish.VanishState;

/**
 * Stops a vanished player from dropping items when {@code [administration.vanish].preventItemDrop}
 * is enabled. The rule lives in {@link VanishState}; the mixin only asks whether the given player
 * is blocked.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(Player.class)
public abstract class PlayerVanishDropMixin {

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_blockVanishedDrop(
            ItemStack itemStack,
            boolean thrownFromHand,
            CallbackInfoReturnable<ItemEntity> callback
    ) {
        if (VanishState.INSTANCE.shouldBlockDrop(((Player) (Object) this).getUUID())) {
            callback.setReturnValue(null);
        }
    }

}

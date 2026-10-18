package top.likoslupus.cellulosesz.administration.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.administration.vanish.VanishState;

/**
 * Stops a vanished player from picking up item entities when
 * {@code [administration.vanish].preventItemPickup} is enabled. The rule lives in
 * {@link VanishState}; the mixin only asks whether the given player is blocked.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityVanishMixin {

    @Inject(
            method = "playerTouch",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_blockVanishedPickup(
            Player player,
            CallbackInfo callback
    ) {
        if (VanishState.INSTANCE.shouldBlockPickup(player.getUUID())) {
            callback.cancel();
        }
    }

}

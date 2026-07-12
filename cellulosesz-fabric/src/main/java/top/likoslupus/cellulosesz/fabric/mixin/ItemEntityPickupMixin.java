package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Inject(
            method = "playerTouch",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$pickup(Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer
                && !FabricPlatformEventBridge.allowPickup(serverPlayer, (ItemEntity) (Object) this)
        ) {
            callback.cancel();
        }
    }

}

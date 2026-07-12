package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;

@Mixin(FishingHook.class)
public abstract class FishingHookEventMixin {

    @Inject(
            method = "retrieve",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$fish(ItemStack rod, CallbackInfoReturnable<Integer> callback) {
        var owner = ((FishingHook) (Object) this).getOwner();
        if (owner instanceof ServerPlayer player
                && !FabricPlatformEventBridge.allowFish(player, rod)) {
            callback.setReturnValue(0);
        }
    }

}

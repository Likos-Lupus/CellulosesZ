package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;

@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {

    @Inject(
            method = "getDisplayName",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$displayName(CallbackInfoReturnable<Component> callback) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        var displayName = FabricDisplayNameBridge.displayName(player.getUUID());
        if (displayName != null) callback.setReturnValue(displayName);
    }

}

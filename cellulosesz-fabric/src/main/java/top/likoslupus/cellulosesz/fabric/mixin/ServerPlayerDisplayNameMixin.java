package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDisplayNameMixin {

    @Inject(
            method = "getTabListDisplayName",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$tabDisplayName(CallbackInfoReturnable<Component> callback) {
        var self = (ServerPlayer) (Object) this;
        var displayName = FabricDisplayNameBridge.displayName(self.getUUID());
        if (displayName != null) callback.setReturnValue(displayName);
    }

}

package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerEventMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleChatCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$commandPreprocess(ServerboundChatCommandPacket packet, CallbackInfo callback) {
        if (!FabricPlatformEventBridge.allowCommand(player, packet.command())) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleSignUpdate",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$signUpdate(ServerboundSignUpdatePacket packet, CallbackInfo callback) {
        if (!FabricPlatformEventBridge.allowSignUpdate(
                player,
                packet.getPos(),
                packet.isFrontText(),
                packet.getLines()
        )) {
            callback.cancel();
        }
    }

}

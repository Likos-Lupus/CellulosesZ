package top.likoslupus.cellulosesz.player.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.player.afk.AfkState;

/**
 * Freezes an AFK player by cancelling their movement packets while
 * {@code players.afk.freezeWhileAfk} is on. Both the on-foot and the vehicle movement entry points
 * are covered so riding cannot be used to keep moving.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245). The
 * policy is only a flag lookup in {@link AfkState}; the AFK set itself is written by the Kotlin
 * service on the server thread.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AfkFreezeMixin {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "handleMovePlayer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$freezeMovePlayer(
            ServerboundMovePlayerPacket packet,
            CallbackInfo callback
    ) {
        if (AfkState.INSTANCE.shouldFreeze(this.player.getUUID())) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleMoveVehicle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$freezeMoveVehicle(
            ServerboundMoveVehiclePacket packet,
            CallbackInfo callback
    ) {
        if (AfkState.INSTANCE.shouldFreeze(this.player.getUUID())) {
            callback.cancel();
        }
    }

}

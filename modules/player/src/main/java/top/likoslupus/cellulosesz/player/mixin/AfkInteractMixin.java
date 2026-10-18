package top.likoslupus.cellulosesz.player.mixin;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.player.afk.AfkState;

/**
 * Reports interactions that may clear AFK when {@code players.afk.cancelOnInteract} is on. The
 * mixin never cancels the action; it only submits a trigger to {@link AfkState}, which the feature
 * drains on the next player tick so the state change (and its announcement) stays on the server
 * thread.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AfkInteractMixin {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "handleInteract",
            at = @At("HEAD")
    )
    private void cellulosesz$afkInteractEntity(
            ServerboundInteractPacket packet,
            CallbackInfo callback
    ) {
        cellulosesz$noteInteraction();
    }

    @Unique
    private void cellulosesz$noteInteraction() {
        var playerId = this.player.getUUID();
        if (AfkState.INSTANCE.isAfk(playerId)) {
            AfkState.INSTANCE.requestInteract(playerId);
        }
    }

    @Inject(
            method = "handleUseItem",
            at = @At("HEAD")
    )
    private void cellulosesz$afkUseItem(
            ServerboundUseItemPacket packet,
            CallbackInfo callback
    ) {
        cellulosesz$noteInteraction();
    }

    @Inject(
            method = "handleUseItemOn",
            at = @At("HEAD")
    )
    private void cellulosesz$afkUseItemOn(
            ServerboundUseItemOnPacket packet,
            CallbackInfo callback
    ) {
        cellulosesz$noteInteraction();
    }

    @Inject(
            method = "handleAttack",
            at = @At("HEAD")
    )
    private void cellulosesz$afkAttack(
            ServerboundAttackPacket packet,
            CallbackInfo callback
    ) {
        cellulosesz$noteInteraction();
    }

}

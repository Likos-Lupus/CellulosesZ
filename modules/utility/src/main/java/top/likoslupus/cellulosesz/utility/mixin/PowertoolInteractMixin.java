package top.likoslupus.cellulosesz.utility.mixin;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.utility.powertool.PowertoolInteractState;

/**
 * Runs a player's bound powertool command on the interaction that fired. The named interact entry
 * ({@link ServerGamePacketListenerImpl#handleInteract}) covers entity interactions; the dedicated
 * use/attack entries cover the air and block clicks so left/right semantics match Essentials. A
 * matched interaction is cancelled; when no powertool matched the interaction proceeds untouched.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245). All
 * policy (recursion rejection, throttling) lives in the Kotlin service behind
 * {@link PowertoolInteractState}.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PowertoolInteractMixin {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "handleInteract",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$powertoolInteract(
            ServerboundInteractPacket packet,
            CallbackInfo callback
    ) {
        if (PowertoolInteractState.INSTANCE.handle(this.player, false)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleUseItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$powertoolUseItem(
            ServerboundUseItemPacket packet,
            CallbackInfo callback
    ) {
        if (PowertoolInteractState.INSTANCE.handle(this.player, false)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleUseItemOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$powertoolUseItemOn(
            ServerboundUseItemOnPacket packet,
            CallbackInfo callback
    ) {
        if (PowertoolInteractState.INSTANCE.handle(this.player, false)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$powertoolAttack(
            ServerboundAttackPacket packet,
            CallbackInfo callback
    ) {
        if (PowertoolInteractState.INSTANCE.handle(this.player, true)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handlePlayerAction",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$powertoolPlayerAction(
            ServerboundPlayerActionPacket packet,
            CallbackInfo callback
    ) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }
        if (PowertoolInteractState.INSTANCE.handle(this.player, true)) {
            callback.cancel();
        }
    }

}

package top.likoslupus.cellulosesz.administration.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.administration.jail.JailRespawnState;

/**
 * Marks a player for an immediate confinement re-check whenever the server respawns them. Death,
 * the end portal and any other respawn path run through {@link PlayerList#respawn}; the tick that
 * follows drains the marker and re-anchors a still-jailed player at once instead of waiting for the
 * next throttled confinement check.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245). The
 * handler captures only the return value, so it does not depend on the exact respawn parameter
 * list.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListRespawnMixin {

    @Inject(
            method = "respawn",
            at = @At("RETURN")
    )
    private void cellulosesz_recheckConfinementOnRespawn(
            CallbackInfoReturnable<ServerPlayer> callback
    ) {
        var player = callback.getReturnValue();
        if (player != null) {
            JailRespawnState.INSTANCE.mark(player.getUUID());
        }
    }

}

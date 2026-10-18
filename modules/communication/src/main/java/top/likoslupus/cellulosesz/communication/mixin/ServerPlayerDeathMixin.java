package top.likoslupus.cellulosesz.communication.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.communication.lifecycle.DeathMessageState;

/**
 * Suppresses and/or replaces the vanilla chat death message. Every broadcast path in
 * {@code ServerPlayer.die} (server-wide, own team, other teams) is redirected through
 * {@link DeathMessageState}: when a custom format is configured the vanilla line is captured (for
 * the {@code {MESSAGE}} token) and dropped; otherwise the vanilla call proceeds untouched. At
 * {@code die} TAIL the feature sends the custom line and the dead player's coordinates.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            ),
            require = 1
    )
    private static void cellulosesz$deathBroadcast(
            PlayerList list,
            Component message,
            boolean overlay
    ) {
        if (DeathMessageState.INSTANCE.isSuppressing()) {
            DeathMessageState.INSTANCE.captureVanilla(message.getString());
        } else {
            list.broadcastSystemMessage(message, overlay);
        }
    }

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"
            ),
            require = 1
    )
    private static void cellulosesz$deathTeamBroadcast(
            PlayerList list,
            Player player,
            Component message
    ) {
        if (DeathMessageState.INSTANCE.isSuppressing()) {
            DeathMessageState.INSTANCE.captureVanilla(message.getString());
        } else {
            list.broadcastSystemToTeam(player, message);
        }
    }

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"
            ),
            require = 1
    )
    private static void cellulosesz$deathExceptTeamBroadcast(
            PlayerList list,
            Player player,
            Component message
    ) {
        if (DeathMessageState.INSTANCE.isSuppressing()) {
            DeathMessageState.INSTANCE.captureVanilla(message.getString());
        } else {
            list.broadcastSystemToAllExceptTeam(player, message);
        }
    }

    @Inject(
            method = "die",
            at = @At("TAIL")
    )
    private void cellulosesz$onDeath(DamageSource source, CallbackInfo callback) {
        DeathMessageState.INSTANCE.onDeath((ServerPlayer) (Object) this);
    }

}

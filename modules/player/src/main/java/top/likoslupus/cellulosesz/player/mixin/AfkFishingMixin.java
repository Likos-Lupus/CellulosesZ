package top.likoslupus.cellulosesz.player.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.player.afk.AfkState;

/**
 * Reports that a player cast a fishing hook, so {@code players.afk.cancelOnFish} can clear their
 * AFK state on the next tick. The player-casting constructor is the only one that carries an
 * owner.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(FishingHook.class)
public abstract class AfkFishingMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
            at = @At("TAIL")
    )
    private void cellulosesz$afkFish(
            Player player,
            Level level,
            int luck,
            int lureSpeed,
            CallbackInfo callback
    ) {
        if (player == null) {
            return;
        }
        var playerId = player.getUUID();
        if (AfkState.INSTANCE.isAfk(playerId)) {
            AfkState.INSTANCE.requestFish(playerId);
        }
    }

}

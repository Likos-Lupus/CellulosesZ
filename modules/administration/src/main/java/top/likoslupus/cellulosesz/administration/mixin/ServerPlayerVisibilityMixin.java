package top.likoslupus.cellulosesz.administration.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.administration.vanish.VanishState;

/**
 * Suppresses entity tracking for vanished players. {@code ServerEntity} consults
 * {@link ServerPlayer#broadcastToPlayer} before pairing an entity with a tracking player; returning
 * false makes the tracker drop the viewer. Vanished players still see each other.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 * {@code getUUID} is inherited from {@link Entity}, so it is reached through the declaring class
 * rather than via {@code @Shadow}, which would only resolve members declared on the target class.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerVisibilityMixin {

    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void cellulosesz_hideVanished(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (VanishState.INSTANCE.shouldHide(
                ((Entity) (Object) this).getUUID(),
                player.getUUID())) {
            callback.setReturnValue(false);
        }
    }

}

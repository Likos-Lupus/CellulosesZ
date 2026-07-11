package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

@Mixin(
        targets = "net.minecraft.server.level.ChunkMap$TrackedEntity"
)
public abstract class ChunkMapTrackedEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    @Inject(
            method = "updatePlayer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$filterVanishedPlayer(ServerPlayer player, CallbackInfo callback) {
        if (entity instanceof ServerPlayer target && FabricVanishBridge.hiddenFrom(player, target)) {
            removePlayer(player);
            callback.cancel();
        }
    }

    @Shadow
    public abstract void removePlayer(ServerPlayer player);

}

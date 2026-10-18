package top.likoslupus.cellulosesz.communication.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.communication.lifecycle.LifecycleMessageState;

/**
 * Captures the quitting player before the vanilla quit line is broadcast. In this Minecraft version
 * {@code removePlayerFromWorld} broadcasts {@code multiplayer.player.left} and only then calls
 * {@code PlayerList.remove}, so {@code PlayerListMessageMixin} alone cannot see the quitter first.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMessageMixin {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "removePlayerFromWorld",
            at = @At("HEAD")
    )
    private void cellulosesz$prepareQuit(CallbackInfo callback) {
        var quitting = this.player;
        if (quitting == null) {
            return;
        }
        LifecycleMessageState.INSTANCE.prepareQuit(
                quitting.getUUID(),
                quitting.getGameProfile().name(),
                quitting.getDisplayName().getString(),
                quitting.level().dimension().identifier().toString()
        );
    }

}

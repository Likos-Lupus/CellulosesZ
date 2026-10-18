package top.likoslupus.cellulosesz.communication.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.communication.lifecycle.LifecycleKind;
import top.likoslupus.cellulosesz.communication.lifecycle.LifecycleMessageState;

/**
 * Replaces or cancels the vanilla join/quit announcements.
 *
 * <p>{@code placeNewPlayer} captures the joining player before the join broadcast and
 * {@code ServerGamePacketListenerMessageMixin} captures the quitting player before the quit
 * broadcast (vanilla broadcasts the quit line <em>before</em> it calls {@code remove}, so
 * {@code remove} cannot be used to set the pending player). The two-argument
 * {@code broadcastSystemMessage} is then inspected by translation key; when the communication
 * feature has a decision, the vanilla broadcast is cancelled and the feature sends its own line.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMessageMixin {

    @Unique private static final String JOIN_KEY = "multiplayer.player.joined";
    @Unique private static final String JOIN_RENAMED_KEY = "multiplayer.player.joined.renamed";
    @Unique private static final String QUIT_KEY = "multiplayer.player.left";

    @Inject(
            method = "placeNewPlayer",
            at = @At("HEAD")
    )
    private void cellulosesz$prepareJoin(
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie,
            CallbackInfo callback
    ) {
        LifecycleMessageState.INSTANCE.prepareJoin(
                player.getUUID(),
                player.getGameProfile().name(),
                player.getDisplayName().getString(),
                player.level().dimension().identifier().toString()
        );
    }

    @Inject(
            method = "remove",
            at = @At("HEAD")
    )
    private void cellulosesz$clearPending(
            ServerPlayer player,
            CallbackInfo callback
    ) {
        LifecycleMessageState.INSTANCE.clearPending();
    }

    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz$handleJoinQuit(
            Component message,
            boolean overlay,
            CallbackInfo callback
    ) {
        var kind = cellulosesz$lifecycleKind(message);
        if (kind == null) {
            return;
        }
        if (LifecycleMessageState.INSTANCE.handle(kind)) {
            callback.cancel();
        }
    }

    @Unique
    private static LifecycleKind cellulosesz$lifecycleKind(Component message) {
        var contents = message.getContents();
        if (!(contents instanceof TranslatableContents translatable)) {
            return null;
        }
        return switch (translatable.getKey()) {
            case JOIN_KEY,
                 JOIN_RENAMED_KEY -> LifecycleKind.JOIN;
            case QUIT_KEY -> LifecycleKind.QUIT;
            default -> null;
        };
    }

}

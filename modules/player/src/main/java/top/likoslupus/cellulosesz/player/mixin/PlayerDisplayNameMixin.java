package top.likoslupus.cellulosesz.player.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.likoslupus.cellulosesz.player.nickname.NicknameState;

/**
 * Renders a player's nickname wherever the server asks for a display name, while {@code getName()}
 * deliberately stays untouched so command arguments keep resolving the real account name.
 *
 * <p>The global vanilla override only applies when {@code players.nicknames.applyToVanillaDisplay}
 * is enabled (default false); otherwise CellulosesZ renders nicknames only at its own controlled
 * sites. {@link NicknameState#displayNameForVanilla} combines both the flag and the map lookup.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 * {@code getUUID} is inherited from {@link Entity}, so it is reached through the declaring class
 * rather than via {@code @Shadow}, which would only resolve members declared on the target class.
 */
@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {

    @Inject(
            method = "getDisplayName",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cellulosesz_nicknameDisplayName(CallbackInfoReturnable<Component> callback) {
        Component nickname = NicknameState.INSTANCE.displayNameForVanilla(
                ((Entity) (Object) this).getUUID());
        if (nickname != null) {
            callback.setReturnValue(nickname);
        }
    }

}

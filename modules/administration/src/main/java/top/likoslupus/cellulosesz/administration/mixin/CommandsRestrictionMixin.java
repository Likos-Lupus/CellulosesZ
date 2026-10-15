package top.likoslupus.cellulosesz.administration.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.administration.jail.JailRestrictionSnapshot;
import top.likoslupus.cellulosesz.administration.jail.JailState;
import top.likoslupus.cellulosesz.administration.jail.RestrictCommandsState;

/**
 * Blocks command execution for confined players when {@code [administration.jail].restrictCommands}
 * is enabled. It hooks both dispatcher entry points so server commands and player chat commands are
 * covered.
 *
 * <p>The rule itself lives in the Kotlin {@link JailRestrictionSnapshot}: the mixin only reads the
 * current immutable snapshot and asks whether the first token is allowed. When the feature is
 * disabled, the source is the console, or the player is not confined, execution proceeds normally
 * (the check only ever refuses more, never allows more than the snapshot permits).
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(Commands.class)
public abstract class CommandsRestrictionMixin {

    @Inject(
            method = "performCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_restrictPerformCommand(
            ParseResults<CommandSourceStack> command,
            String commandString,
            CallbackInfo callback
    ) {
        if (cellulosesz_shouldCancel(command.getContext().getSource(), commandString)) {
            callback.cancel();
        }
    }

    @Unique
    private static boolean cellulosesz_shouldCancel(CommandSourceStack source, String command) {
        if (source == null) {
            return false;
        }
        var snapshot = RestrictCommandsState.INSTANCE.current();
        if (!snapshot.getEnabled()) {
            return false;
        }
        var player = source.getPlayer();
        return player != null
                && JailState.INSTANCE.confined().contains(player.getUUID())
                && !snapshot.isAllowed(command);
    }

    @Inject(
            method = "performPrefixedCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_restrictPerformPrefixedCommand(
            CommandSourceStack sender,
            String command,
            CallbackInfo callback
    ) {
        if (cellulosesz_shouldCancel(sender, command)) {
            callback.cancel();
        }
    }

}

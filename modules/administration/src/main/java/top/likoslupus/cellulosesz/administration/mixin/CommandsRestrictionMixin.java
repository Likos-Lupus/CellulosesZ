package top.likoslupus.cellulosesz.administration.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.administration.jail.JailState;
import top.likoslupus.cellulosesz.administration.jail.RestrictCommandsState;

import java.util.Set;

/**
 * Blocks command execution for confined players when {@code [administration.jail].restrictCommands}
 * is enabled. It hooks both dispatcher entry points so server commands and player chat commands are
 * covered, and only the first token is inspected. All failures fail open (an unknown source, a
 * console call or a disabled feature executes normally).
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(Commands.class)
public abstract class CommandsRestrictionMixin {

    @Unique
    private static final Set<String> cellulosesz_allowedJailCommands = Set.of("helpop");

    @Inject(
            method = "performCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_restrictPerformCommand(
            ParseResults<CommandSourceStack> parse,
            String command,
            CallbackInfo callback
    ) {
        if (cellulosesz_shouldCancel(parse.getContext().getSource(), command)) {
            callback.cancel();
        }
    }

    @Unique
    private static boolean cellulosesz_shouldCancel(CommandSourceStack source, String command) {
        if (source == null
                || !RestrictCommandsState.INSTANCE.isEnabled()
        ) {
            return false;
        }
        var player = source.getPlayer();
        return player != null
                && JailState.INSTANCE.confined().contains(player.getUUID())
                && !cellulosesz_allowedJailCommands.contains(cellulosesz_firstToken(command));
    }

    @Unique
    private static String cellulosesz_firstToken(String command) {
        if (command == null) {
            return "";
        }
        var trimmed = command.trim();
        var space = trimmed.indexOf(' ');
        return space < 0
                ? trimmed
                : trimmed.substring(0, space);
    }

    @Inject(
            method = "performPrefixedCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cellulosesz_restrictPerformPrefixedCommand(
            CommandSourceStack source,
            String command,
            CallbackInfo callback
    ) {
        if (cellulosesz_shouldCancel(source, command)) {
            callback.cancel();
        }
    }

}

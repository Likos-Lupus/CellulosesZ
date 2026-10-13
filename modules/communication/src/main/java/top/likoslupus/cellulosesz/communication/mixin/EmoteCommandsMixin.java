package top.likoslupus.cellulosesz.communication.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.EmoteCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.likoslupus.cellulosesz.communication.action.MeOverrideState;

/**
 * Cancels vanilla {@code /me} registration when CellulosesZ owns the command. The DSL registers its
 * own {@code me} literal with a {@code MessageArgument}; without cancelling, Brigadier would merge
 * the two nodes by name and keep whichever argument type was registered first, so the executors
 * could read the wrong type. Cancelling at HEAD means the vanilla node never enters the tree.
 *
 * <p>Written in Java because Mixin's Kotlin support is incomplete (SpongePowered/Mixin#245).
 */
@Mixin(EmoteCommands.class)
public abstract class EmoteCommandsMixin {

    @Inject(
            method = "register",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cellulosesz_cancelVanillaMe(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CallbackInfo callback
    ) {
        if (MeOverrideState.INSTANCE.isEnabled()) {
            callback.cancel();
        }
    }

}

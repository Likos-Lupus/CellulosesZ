package top.likoslupus.cellulosesz.communication.lifecycle

import net.minecraft.server.level.ServerPlayer

/**
 * The composition-root-installed death-message policy. [suppressesVanilla] is consulted from the
 * `ServerPlayer.die` redirects; [onDeath] runs at `die` TAIL on the server thread.
 */
internal interface DeathMessageController {

    /** True when the vanilla chat death message must be suppressed in favor of a custom one. */
    fun suppressesVanilla(): Boolean

    /** Runs after the death is committed. [vanillaMessage] is the captured vanilla text, or null. */
    fun onDeath(
        player: ServerPlayer,
        vanillaMessage: String?,
    )

    companion object {

        val Disabled: DeathMessageController = object : DeathMessageController {

            override fun suppressesVanilla(): Boolean = false

            override fun onDeath(
                player: ServerPlayer,
                vanillaMessage: String?,
            ) = Unit

        }

    }

}

/**
 * Process-wide bridge between [top.likoslupus.cellulosesz.communication.mixin.ServerPlayerDeathMixin]
 * and the communication feature. The vanilla death text is captured only when the message is being
 * suppressed, so `{MESSAGE}` can echo it without recomputing the combat tracker.
 */
internal object DeathMessageState {

    @Volatile private var controller: DeathMessageController = DeathMessageController.Disabled

    private var vanillaMessage: String? = null

    fun install(controller: DeathMessageController) {
        this.controller = controller
        vanillaMessage = null
    }

    fun uninstall() {
        controller = DeathMessageController.Disabled
        vanillaMessage = null
    }

    fun isSuppressing(): Boolean =
        controller.suppressesVanilla()

    fun captureVanilla(message: String) {
        vanillaMessage = message
    }

    fun onDeath(player: ServerPlayer) {
        controller.onDeath(player, vanillaMessage)
        vanillaMessage = null
    }

}

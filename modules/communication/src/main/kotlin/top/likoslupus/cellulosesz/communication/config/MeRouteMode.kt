package top.likoslupus.cellulosesz.communication.config

/**
 * Startup-bound decision about who owns the vanilla `/me` literal. It is a STATIC_BINDING derived
 * from [ActionMessageSettings]: a change requires a restart because it is decided while vanilla
 * builds its dispatcher, before CellulosesZ registers its own commands.
 */
public enum class MeRouteMode {

    /** Cancel vanilla `/me` registration and try to register CellulosesZ's own `/me`. */
    REPLACE_VANILLA,

    /**
     * Never touch `/me`: vanilla (or whichever mod registered first) keeps it. CellulosesZ still
     * exposes `/czme` and `/cellulosesz:me`.
     */
    PRESERVE_VANILLA,

}

/**
 * REPLACE only when the action service is enabled *and* the operator opted into the override;
 * disabling the service never strips the player of the vanilla emote command.
 */
public fun meRouteMode(settings: ActionMessageSettings): MeRouteMode =
    when {
        settings.enabled && settings.overrideVanillaMe -> MeRouteMode.REPLACE_VANILLA
        else -> MeRouteMode.PRESERVE_VANILLA
    }

package top.likoslupus.cellulosesz.core.text

import net.minecraft.network.chat.Component

/**
 * Controlled vanilla-component outlet. It exists only for surfaces that genuinely cannot use the
 * Adventure presentation pipeline (vanilla menu titles, the disconnect reason channel, and the
 * command DSL's console-only "requires a player" fallback). Ordinary user feedback must go through
 * the semantic presentation pipeline, never through this object.
 */
public object Messages {

    public fun raw(message: String): Component =
        Component.literal(message)

}

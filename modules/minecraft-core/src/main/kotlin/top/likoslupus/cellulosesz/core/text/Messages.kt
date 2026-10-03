package top.likoslupus.cellulosesz.core.text

import net.minecraft.network.chat.Component

/** Thin vanilla-component factory. No templating language; upgrade only if a real need appears. */
public object Messages {

    public const val PREFIX: String = "[CellulosesZ] "

    public fun prefixed(message: String): Component =
        Component.literal(PREFIX + message)

    public fun raw(message: String): Component =
        Component.literal(message)

}

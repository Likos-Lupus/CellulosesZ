package top.likoslupus.cellulosesz.text

import net.minecraft.network.chat.Component

internal object Messages {

    const val PREFIX: String = "[CellulosesZ] "

    fun prefixed(message: String): Component = Component.literal(PREFIX + message)

    fun raw(message: String): Component = Component.literal(message)

    fun privateMessage(senderName: String, message: String): Component =
        Component.literal("[$senderName -> you] $message")

    fun privateMessageSent(targetName: String, message: String): Component =
        Component.literal("[you -> $targetName] $message")

}

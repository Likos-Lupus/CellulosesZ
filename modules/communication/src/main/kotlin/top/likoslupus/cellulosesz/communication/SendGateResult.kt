package top.likoslupus.cellulosesz.communication

import net.minecraft.network.chat.Component

/**
 * Result of a cross-feature sender restriction check. Typed so the ambiguous "does `true` mean
 * allowed or restricted?" boolean can never come back.
 */
public sealed interface SendGateResult {

    public data object Allowed : SendGateResult

    public data class Denied(
        public val feedback: Component,
    ) : SendGateResult

}

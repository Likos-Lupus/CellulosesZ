package top.likoslupus.cellulosesz.communication

/** Why a sender was refused by the cross-feature gate. Typed so presentation is per-recipient. */
public enum class SenderRestriction {

    MUTED,

}

/**
 * Result of a cross-feature sender restriction check. Typed so the ambiguous "does `true` mean
 * allowed or restricted?" boolean can never come back.
 */
public sealed interface SendGateResult {

    public data object Allowed : SendGateResult

    public data class Denied(
        public val reason: SenderRestriction,
    ) : SendGateResult

}

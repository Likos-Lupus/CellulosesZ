package top.likoslupus.cellulosesz.economy.account

import java.util.*

/** The role of an account; only [PLAYER] accounts are addressable as a transfer subject. */
internal enum class AccountKind {

    PLAYER,
    MINT,
    SINK,

}

/**
 * A typed economy account identity. Player accounts are keyed by UUID, never by name; system
 * accounts are the two internal bookkeeping counterparts and can only be produced by this factory,
 * so no caller can forge `system:mint` as a payment target.
 */
internal sealed interface EconomyAccountId {

    val storageId: String
    val kind: AccountKind

    data class Player(val playerUuid: UUID) : EconomyAccountId {

        override val storageId: String
            get() = "player:$playerUuid"

        override val kind: AccountKind
            get() = AccountKind.PLAYER

    }

    data object Mint : EconomyAccountId {

        override val storageId: String
            get() = "system:mint"

        override val kind: AccountKind
            get() = AccountKind.MINT

    }

    data object Sink : EconomyAccountId {

        override val storageId: String
            get() = "system:sink"

        override val kind: AccountKind
            get() = AccountKind.SINK

    }

    companion object {

        fun fromStorageId(storageId: String): EconomyAccountId? = when (storageId) {
            Mint.storageId -> Mint
            Sink.storageId -> Sink
            else -> {
                val local = storageId.removePrefix("player:")
                if (local == storageId) {
                    null
                } else {
                    try {
                        Player(UUID.fromString(local))
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }
        }

    }

}

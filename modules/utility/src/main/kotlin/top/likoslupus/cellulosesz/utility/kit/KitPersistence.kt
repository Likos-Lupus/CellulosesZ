package top.likoslupus.cellulosesz.utility.kit

/** Machine-data safety caps, independent of the (admin-tunable) config limits. */
internal const val MAX_STORED_KITS: Int = 10_000
internal const val MAX_STORED_ITEMS_PER_KIT: Int = 1_000
internal const val MAX_ITEM_PAYLOAD_CHARS: Int = 1_048_576

/** Persisted form of a reuse policy; the sealed domain type is validated on load. */
internal enum class KitReuseMode {

    ALWAYS,
    ONCE,
    COOLDOWN,

}

internal const val MAX_STORED_CLAIMS_PER_PLAYER: Int = 10_000

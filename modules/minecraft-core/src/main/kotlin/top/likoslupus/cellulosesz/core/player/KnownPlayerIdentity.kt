package top.likoslupus.cellulosesz.core.player

import java.util.*

/**
 * Immutable snapshot of a player identity known to this server. It never holds a
 * [net.minecraft.server.level.ServerPlayer] and can safely cross suspension.
 */
public data class KnownPlayerIdentity(
    public val id: UUID,
    public val name: String,
)

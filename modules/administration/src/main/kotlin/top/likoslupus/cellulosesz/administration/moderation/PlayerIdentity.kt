package top.likoslupus.cellulosesz.administration.moderation

import java.util.*

/**
 * Immutable snapshot of a moderation target. It never holds a [net.minecraft.server.level.ServerPlayer]
 * and can safely cross suspension and be persisted.
 */
internal data class PlayerIdentity(
    val id: UUID,
    val name: String,
)

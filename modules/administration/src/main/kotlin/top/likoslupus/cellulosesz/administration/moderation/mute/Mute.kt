package top.likoslupus.cellulosesz.administration.moderation.mute

import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import java.time.Instant
import java.util.*

/** An active mute. Runtime value; persisted through [MuteFile]. */
internal data class Mute(
    val playerId: UUID,
    val playerName: String,
    val actor: PersistedModerationActor,
    val reason: String,
    val issuedAt: Instant,
    val expiresAt: Instant?,
)

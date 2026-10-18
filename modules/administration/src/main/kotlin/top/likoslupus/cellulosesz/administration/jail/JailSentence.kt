package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.core.player.WorldPosition
import java.time.Instant
import java.util.*

/** An immutable jail sentence. Runtime value; persisted by the sentence repository. */
internal data class JailSentence(
    val playerId: UUID,
    val playerName: String,
    val jailName: JailName,
    val status: JailStatus,
    val actor: PersistedModerationActor,
    val reason: String,
    val issuedAt: Instant,
    /** How the term is measured. */
    val mode: JailSentenceMode,
    /** The instant a [JailSentenceMode.WALL_CLOCK] sentence expires; otherwise `null`. */
    val expiresAt: Instant?,
    /** The seconds left on a [JailSentenceMode.ONLINE] sentence; otherwise `null`. */
    val remainingSeconds: Long?,
    /** Where the player stood when jailed, or `null` when they were offline. */
    val returnPosition: WorldPosition?,
    /** When the sentence entered [JailStatus.RELEASE_PENDING], or `null` while active. */
    val releaseRequestedAt: Instant?,
)

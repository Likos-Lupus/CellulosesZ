package top.likoslupus.cellulosesz.administration.integration

import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * The actor of a moderation integration request. Never a free string.
 *
 * A [Player] is authorized by the port itself: the player must be online and hold the moderation
 * capability of the requested action (the same `cellulosesz.command.*` nodes the commands use).
 * An [Addon] is only constructed by the application adapter after it has verified the addon's
 * capability against its live authorization snapshot; the port treats it as an already-authorized
 * system actor and records the addon id in the audit trail.
 */
public sealed interface ModerationIntegrationActor {

    public data class Player(public val playerId: UUID) : ModerationIntegrationActor

    public data class Addon(public val addonId: String) : ModerationIntegrationActor

}

/** A coarse moderation status summary; detail beyond this requires the moderator capability. */
public class ModerationStatusSummary(
    public val muted: Boolean,
    public val mutedUntil: Instant?,
    public val confined: Boolean,
)

public sealed interface ModerationIntegrationStatus {

    public data class Known(
        public val summary: ModerationStatusSummary
    ) : ModerationIntegrationStatus

    public data object NotFound : ModerationIntegrationStatus

    public data object NotAuthorized : ModerationIntegrationStatus

    public data object StorageUnavailable : ModerationIntegrationStatus

    /** The moderation runtime is shutting down; nothing was executed. */
    public data object RuntimeStopping : ModerationIntegrationStatus

}

public sealed interface ModerationIntegrationOutcome {

    public data class Applied(public val actionId: String) : ModerationIntegrationOutcome

    public data object AlreadyInState : ModerationIntegrationOutcome

    public data object TargetUnknown : ModerationIntegrationOutcome

    /** The target is protected (an operator under `protectOperators`, or the actor itself). */
    public data object ProtectedTarget : ModerationIntegrationOutcome

    public data object InvalidDuration : ModerationIntegrationOutcome

    public data object InvalidReason : ModerationIntegrationOutcome

    public data object NotAuthorized : ModerationIntegrationOutcome

    public data object StorageUnavailable : ModerationIntegrationOutcome

    /** The jail capability is disabled or has no usable destination. */
    public data object Unavailable : ModerationIntegrationOutcome

    public data object RuntimeStopping : ModerationIntegrationOutcome

    /** The penalty applied but its audit record did not; surfaced, never silently swallowed. */
    public data object PartiallyApplied : ModerationIntegrationOutcome

}

/**
 * Committed moderation-domain facts. They describe CellulosesZ's own committed actions only; a
 * vanilla ban or another mod's permission change is not a CellulosesZ event.
 */
public sealed interface ModerationDomainEvent {

    public data class Muted(
        public val playerId: UUID,
        public val until: Instant?,
        public val actionId: String,
    ) : ModerationDomainEvent

    public data class Unmuted(
        public val playerId: UUID,
        public val actionId: String,
    ) : ModerationDomainEvent

    public data class Confined(
        public val playerId: UUID,
        public val actionId: String,
    ) : ModerationDomainEvent

    public data class Released(
        public val playerId: UUID,
        public val actionId: String,
    ) : ModerationDomainEvent

    public data class VanishStateChanged(
        public val playerId: UUID,
        public val vanished: Boolean,
    ) : ModerationDomainEvent

}

/** Receives one immutable committed moderation fact; a throwing observer is isolated and logged. */
public fun interface ModerationDomainObserver {

    public fun onEvent(event: ModerationDomainEvent)

    public companion object {

        public val None: ModerationDomainObserver = ModerationDomainObserver { }

    }

}

/** A cancellable observer registration. [close] is idempotent and safe from any thread. */
public fun interface ModerationEventRegistration : AutoCloseable {

    public override fun close()

}

/**
 * Narrow integration surface for the moderation domain. It exposes only the operations the public
 * API needs and never exposes the internal mute/jail/vanish services. It preserves target
 * protection and the structured audit trail, and emits a committed fact for every applied action,
 * whether it came from a command or from the API.
 */
public interface ModerationIntegrationPort {

    /** A cheap, session-scoped coarse read: whether [subjectId] is muted. It reveals no detail. */
    public fun isMuted(viewerId: UUID, subjectId: UUID): Boolean

    public fun observe(observer: ModerationDomainObserver): ModerationEventRegistration

    public suspend fun status(viewerId: UUID, subjectId: UUID): ModerationIntegrationStatus

    public suspend fun mute(
        actor: ModerationIntegrationActor,
        targetId: UUID,
        reason: String,
        duration: Duration?,
    ): ModerationIntegrationOutcome

    public suspend fun unmute(
        actor: ModerationIntegrationActor,
        targetId: UUID,
    ): ModerationIntegrationOutcome

    public suspend fun confine(
        actor: ModerationIntegrationActor,
        targetId: UUID,
        reason: String,
    ): ModerationIntegrationOutcome

    public suspend fun release(
        actor: ModerationIntegrationActor,
        targetId: UUID,
    ): ModerationIntegrationOutcome

}

package top.likoslupus.cellulosesz.api

import top.likoslupus.cellulosesz.api.communication.CommunicationApi
import top.likoslupus.cellulosesz.api.extension.ExtensionsApi
import top.likoslupus.cellulosesz.api.kit.KitsApi
import top.likoslupus.cellulosesz.api.moderation.ModerationApi
import top.likoslupus.cellulosesz.api.movement.MovementApi
import top.likoslupus.cellulosesz.api.player.PlayersApi
import top.likoslupus.cellulosesz.api.presentation.PresentationApi

/**
 * The independent public API contract version. It is deliberately separate from the mod runtime
 * version: `major = 0` means experimental, where a documented, versioned break is still possible.
 */
public data class ApiVersion(
    public val major: Int,
    public val minor: Int,
)

/**
 * A read that can be temporarily unavailable because the server session is not ready. An empty
 * result (`Available(null)`) is distinct from [NotReady]/[Stopping].
 */
public sealed interface ApiQuery<out T> {

    public data class Available<T>(public val value: T) : ApiQuery<T>

    public data object NotReady : ApiQuery<Nothing>

    public data object Stopping : ApiQuery<Nothing>

}

/** Called once when the API becomes ready for the current server session. */
public fun interface ApiReadyListener {

    public fun onReady(api: CellulosesZApi)

}

/** A cancellable registration. [close] is idempotent and safe to call from any thread. */
public fun interface Subscription : AutoCloseable {

    public override fun close()

}

/**
 * A per-server-session, read-only view of CellulosesZ's integration surface. It is valid only while
 * the session that produced it is ready; a new world/session yields a new view.
 */
public interface CellulosesZApi {

    public val version: ApiVersion

    public val players: PlayerLookup

    public val visibility: VisibilityLookup

    public val teleports: TeleportEvents

    public val player: PlayersApi

    public val movement: MovementApi

    public val communication: CommunicationApi

    public val kits: KitsApi

    public val moderation: ModerationApi

    public val presentation: PresentationApi

    public val extensions: ExtensionsApi

}

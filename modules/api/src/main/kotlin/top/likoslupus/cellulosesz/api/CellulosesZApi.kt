package top.likoslupus.cellulosesz.api

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

}

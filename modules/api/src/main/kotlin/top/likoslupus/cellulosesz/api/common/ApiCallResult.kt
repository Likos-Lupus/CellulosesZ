package top.likoslupus.cellulosesz.api.common

/**
 * The outer wrapper for every asynchronous API call.
 *
 * It describes runtime availability and infrastructure failures only. An *expected* business
 * outcome (denied, not found, cooldown, already used, ...) is carried inside [Completed] as a
 * domain-specific sealed result, so consumers can match exhaustively without parsing strings.
 */
public sealed interface ApiCallResult<out T> {

    /**
     * The request reached a domain decision. This does **not** imply business success: inspect the
     * wrapped domain result (for example a `TeleportActionOutcome`).
     */
    public data class Completed<T>(public val value: T) : ApiCallResult<T>

    /** The API is not ready yet for a server session; nothing was executed. */
    public data object NotReady : ApiCallResult<Nothing>

    /** The current server session is stopping; no new world-affecting work is accepted. */
    public data object Stopping : ApiCallResult<Nothing>

    /** The requested feature is unavailable (disabled, absent, or failed to load). */
    public data class Unavailable(public val feature: String) : ApiCallResult<Nothing>

    /**
     * An unexpected infrastructure failure occurred. [diagnosticId] correlates with the server log;
     * it never contains payload data, SQL, IP addresses or credentials.
     */
    public data class Failed(public val diagnosticId: String) : ApiCallResult<Nothing>

}

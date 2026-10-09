package top.likoslupus.cellulosesz.application.health

/** Explicit bootstrap/health state, so a failed storage bring-up never looks like "ready". */
public class ApplicationHealth {

    public enum class State {

        UNINITIALIZED,
        CONFIG_READY,
        BOOTSTRAPPING_STORAGE,
        STORAGE_READY,
        LOADING_STATE,
        READY,
        FAILED,
        STOPPING,
        STOPPED,

    }

    @Volatile public var state: State = State.UNINITIALIZED
        private set
    @Volatile public var loader: String = "unknown"
    @Volatile public var permissionBackend: String = "vanilla"
    @Volatile public var storageType: String? = null
    @Volatile public var storageEndpoint: String? = null
    @Volatile public var migration: String = "NONE"
    @Volatile public var identityRecords: Int = 0
    @Volatile public var lastError: String? = null

    public fun transition(next: State) {
        state = next
    }

    public fun fail(message: String) {
        lastError = message
        state = State.FAILED
    }

    /** A single redacted, secret-free status line. */
    public fun summary(): String =
        buildString {
            append("state=").append(state)
            append(" loader=").append(loader)
            append(" permissionBackend=").append(permissionBackend)
            storageType?.let { append(" storage=").append(it) }
            storageEndpoint?.let { append(" storageEndpoint=").append(it) }
            append(" migration=").append(migration)
            append(" identityRecords=").append(identityRecords)
            lastError?.let { append(" lastError=").append(it) }
        }

}

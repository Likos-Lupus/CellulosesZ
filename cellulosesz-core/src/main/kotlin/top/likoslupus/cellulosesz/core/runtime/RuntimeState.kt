package top.likoslupus.cellulosesz.core.runtime

/**
 * Explicit lifecycle state for the CellulosesZ core runtime.
 */
enum class RuntimeState {

    /**
     * Active and accepting new module scopes and owned work.
     */
    RUNNING,

    /**
     * Shutdown initiated: rejects new work while in-flight operations drain and child scopes close.
     */
    STOPPING,

    /**
     * Fully stopped: all child scopes, jobs, and owned resources have completed teardown.
     */
    STOPPED,

}

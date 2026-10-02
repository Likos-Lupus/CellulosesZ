package top.likoslupus.cellulosesz.administration.moderation

/** Readiness of the moderation subsystem, independent of the runtime kernel state. */
internal enum class ModerationState {

    LOADING,
    READY,
    DEGRADED,
    STOPPING,

}

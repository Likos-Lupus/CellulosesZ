package top.likoslupus.cellulosesz.api.platform.operation

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome

public enum class PlatformOperationStatus {

    SUCCESS,
    WRONG_THREAD,
    NOT_READY,
    WORLD_NOT_FOUND,
    UNSAFE_DESTINATION,
    INVALID_ARGUMENT,
    INVALID_INPUT,
    INVALID_SOURCE,
    INVALID_STATE,
    NOT_FOUND,
    PERMISSION_DENIED,
    STORAGE_FAILURE,
    TARGET_NOT_FOUND,
    STATE_NOT_ALLOWED,
    EXEMPT,
    UNSUPPORTED,
    CONFLICT,
    PARTIAL_SUCCESS,
    ROLLBACK_FAILED,
    INTERNAL_ERROR;

    public fun toCommandOutcome(): CommandOutcome =
        when (this) {
            SUCCESS -> CommandOutcome.success()
            PARTIAL_SUCCESS -> CommandOutcome.partial()
            WRONG_THREAD,
            NOT_READY,
            STORAGE_FAILURE,
            ROLLBACK_FAILED,
            INTERNAL_ERROR -> CommandOutcome.failed()

            else -> CommandOutcome.rejected()
        }

}

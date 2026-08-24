package top.likoslupus.cellulosesz.modules.admin.domain;

/**
 * Stable machine-readable outcome for administration operations.
 */
public enum AdminStatus {

    SUCCESS,
    PARTIAL_SUCCESS,
    NOT_FOUND,
    ALREADY_EXISTS,
    INVALID_INPUT,
    PERSISTENCE_FAILURE,
    NATIVE_COMMAND_FAILURE,
    PLATFORM_FAILURE,
    ROLLBACK_FAILURE,
    FAILURE;

    public boolean failed() {
        return !successful();
    }

    public boolean successful() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

}

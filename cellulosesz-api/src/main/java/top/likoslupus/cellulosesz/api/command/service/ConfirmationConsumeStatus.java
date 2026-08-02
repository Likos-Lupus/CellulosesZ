package top.likoslupus.cellulosesz.api.command.service;

public enum ConfirmationConsumeStatus {

    CONSUMED,
    NOT_FOUND,
    TOKEN_MISMATCH,
    EXPIRED,
    PAYLOAD_TYPE_MISMATCH,
    CONCURRENTLY_CONSUMED

}

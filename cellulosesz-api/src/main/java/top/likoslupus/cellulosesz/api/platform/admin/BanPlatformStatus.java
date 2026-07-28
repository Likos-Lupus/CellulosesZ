package top.likoslupus.cellulosesz.api.platform.admin;

public enum BanPlatformStatus {

    SUCCESS,
    ALREADY_BANNED,
    NOT_FOUND,
    NOT_READY,
    WRONG_THREAD,
    PERSISTENCE_FAILURE,
    PLATFORM_FAILURE;

    public boolean successful() {
        return this == SUCCESS;
    }

}

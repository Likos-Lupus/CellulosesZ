package top.likoslupus.cellulosesz.api.module

public enum class ModuleLifecycleState {

    DISCOVERED,
    RESOLVED,
    STARTING,
    ACTIVE,
    STOPPING,
    STOPPED,
    FAILED;

    public val isActive: Boolean get() = this == ACTIVE

}

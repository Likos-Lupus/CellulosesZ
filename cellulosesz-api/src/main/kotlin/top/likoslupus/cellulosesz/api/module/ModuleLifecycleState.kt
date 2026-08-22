package top.likoslupus.cellulosesz.api.module

enum class ModuleLifecycleState {

    DISCOVERED,
    RESOLVED,
    STARTING,
    ACTIVE,
    STOPPING,
    STOPPED,
    FAILED;

    val isActive: Boolean get() = this == ACTIVE

}

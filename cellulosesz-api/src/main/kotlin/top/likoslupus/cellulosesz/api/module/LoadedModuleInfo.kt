package top.likoslupus.cellulosesz.api.module

data class LoadedModuleInfo(
    val key: ModuleKey,
    val name: String,
    val description: String,
    val phase: ModulePhase,
    val enabled: Boolean,
    val state: String = if (enabled) "ACTIVE" else "STOPPED",
) {

    fun id(): String = key.value
    fun name(): String = name
    fun description(): String = description
    fun phase(): ModulePhase = phase
    fun enabled(): Boolean = enabled
    fun state(): String = state

}

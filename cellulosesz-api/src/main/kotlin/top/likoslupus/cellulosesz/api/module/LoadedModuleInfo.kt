package top.likoslupus.cellulosesz.api.module

public data class LoadedModuleInfo(
    public val key: ModuleKey,
    public val name: String,
    public val description: String,
    public val phase: ModulePhase,
    public val enabled: Boolean,
    public val state: String = if (enabled) "ACTIVE" else "STOPPED",
) {

    public fun id(): String = key.value
    public fun name(): String = name
    public fun description(): String = description
    public fun phase(): ModulePhase = phase
    public fun enabled(): Boolean = enabled
    public fun state(): String = state

}

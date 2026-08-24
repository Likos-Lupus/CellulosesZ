package top.likoslupus.cellulosesz.api.module

import top.likoslupus.cellulosesz.api.validation.requireNonBlank

public data class ModuleDescriptor(
    public val key: ModuleKey,
    public val name: String,
    public val description: String,
    public val phase: ModulePhase = ModulePhase.FEATURE,
    public val priority: Int = 0,
    public val requires: Set<ModuleKey> = emptySet(),
    public val optional: Set<ModuleKey> = emptySet(),
    public val enabledByDefault: Boolean = true,
) {

    init {
        name.requireNonBlank { "name" }
        require(key !in requires) {
            "Module '$key' cannot require itself"
        }
        require(key !in optional) {
            "Module '$key' cannot optionally depend on itself"
        }
        val overlap = requires.intersect(optional)
        require(overlap.isEmpty()) {
            "Module '$key' has overlapping required and optional dependencies: $overlap"
        }
    }

    public fun id(): String = key.value

}

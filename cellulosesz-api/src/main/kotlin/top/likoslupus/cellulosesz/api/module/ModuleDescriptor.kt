package top.likoslupus.cellulosesz.api.module

import top.likoslupus.cellulosesz.api.validation.TextChecks

data class ModuleDescriptor(
    val key: ModuleKey,
    val name: String,
    val description: String,
    val phase: ModulePhase = ModulePhase.FEATURE,
    val priority: Int = 0,
    val requires: Set<ModuleKey> = emptySet(),
    val optional: Set<ModuleKey> = emptySet(),
    val enabledByDefault: Boolean = true,
) {

    init {
        TextChecks.requireNonBlank(name, "name")
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

    fun id(): String = key.value

}

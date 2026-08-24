package top.likoslupus.cellulosesz.core.module

import top.likoslupus.cellulosesz.api.module.ModuleDescriptor
import top.likoslupus.cellulosesz.api.module.ModuleKey

data class ResolvedModuleGraph(
    val activeKeys: Set<ModuleKey>,
    val startOrder: List<ModuleDescriptor>,
    val stopOrder: List<ModuleDescriptor> = startOrder.reversed(),
    val descriptors: Map<ModuleKey, ModuleDescriptor> = startOrder.associateBy { it.key },
) {

    val startKeys: List<ModuleKey> = startOrder.map { it.key }
    val stopKeys: List<ModuleKey> = stopOrder.map { it.key }

    fun optionalAvailability(key: ModuleKey): Map<ModuleKey, Boolean> {
        val descriptor = descriptors[key]
        return descriptor?.optional?.associateWith {
            it in activeKeys
        } ?: emptyMap()
    }

}

class ModuleGraph(
    val catalog: ModuleCatalog,
) {

    private val allDescriptors: Map<ModuleKey, ModuleDescriptor> =
        catalog.descriptors.associateBy { it.key }

    init {
        catalog.descriptors.forEach { (key, _, _, _, _, requires) ->
            requires.forEach {
                if (it !in allDescriptors) {
                    throw ModuleLoadException("Module '$key' requires missing module '$it'")
                }
            }
        }
    }

    fun sort(descriptors: Collection<ModuleDescriptor>): List<ModuleDescriptor> {
        val byKey = LinkedHashMap<ModuleKey, ModuleDescriptor>()
        descriptors.forEach {
            if (byKey.putIfAbsent(it.key, it) != null) {
                throw ModuleLoadException("Duplicate module id: ${it.key}")
            }
        }

        byKey.values.forEach { desc ->
            desc.requires.forEach { req ->
                if (req !in byKey) {
                    throw ModuleLoadException("Module '${desc.key}' requires missing module '$req'")
                }
            }
        }

        val baseOrder = byKey.values.sortedWith(
            compareBy<ModuleDescriptor> { it.phase }
                    .thenBy { it.priority }
                    .thenBy { it.key.value }
        )

        val sorted = mutableListOf<ModuleDescriptor>()
        val visiting = LinkedHashSet<ModuleKey>()
        val visited = mutableSetOf<ModuleKey>()

        fun visit(descriptor: ModuleDescriptor) {
            val key = descriptor.key
            if (key in visited) return
            if (key in visiting) {
                val cycle = visiting.dropWhile { it != key } + key
                throw ModuleLoadException("Module dependency cycle: ${cycle.joinToString(" -> ") { it.value }}")
            }

            visiting.add(key)

            descriptor.requires.sortedBy { it.value }.forEach {
                val dep = byKey[it]
                    ?: throw ModuleLoadException("Module '$key' requires missing module '$it'")
                visit(dep)
            }

            descriptor.optional.sortedBy { it.value }.forEach {
                val dep = byKey[it]
                if (dep != null) {
                    visit(dep)
                }
            }

            visiting.remove(key)
            visited.add(key)
            sorted.add(descriptor)
        }

        baseOrder.forEach { visit(it) }

        return sorted
    }

    fun resolve(enabled: Set<ModuleKey>): ResolvedModuleGraph {
        enabled.forEach { key ->
            val desc = allDescriptors[key]
                ?: throw ModuleLoadException("Unknown module key: '$key'")
            desc.requires.forEach {
                if (it !in enabled) {
                    throw ModuleLoadException("Module '$key' requires enabled module '$it'")
                }
            }
        }

        val activeDescriptors = enabled.map { allDescriptors.getValue(it) }
        val startOrder = sort(activeDescriptors)
        return ResolvedModuleGraph(
            activeKeys = enabled,
            startOrder = startOrder,
        )
    }

}

package top.likoslupus.cellulosesz.core.module

import top.likoslupus.cellulosesz.api.module.ModuleDescriptor
import top.likoslupus.cellulosesz.api.module.ModuleKey
import top.likoslupus.cellulosesz.api.module.ModulePhase

interface ModuleCatalog {

    val definitions: List<ModuleDefinition>
    val descriptors: List<ModuleDescriptor> get() = definitions.map { it.descriptor }

    operator fun get(key: ModuleKey): ModuleDefinition?

    fun require(key: ModuleKey): ModuleDefinition =
        get(key) ?: throw NoSuchElementException("No module definition found for key '$key'")

    companion object {

        @JvmStatic
        fun of(definitions: Collection<ModuleDefinition>): ModuleCatalog =
            DefaultModuleCatalog(definitions)

        @JvmStatic
        fun builder(): ModuleCatalogBuilder = ModuleCatalogBuilder()

    }

}

class DefaultModuleCatalog(definitions: Collection<ModuleDefinition>) : ModuleCatalog {

    override val definitions: List<ModuleDefinition> = definitions.toList()
    private val byKey: Map<ModuleKey, ModuleDefinition>

    init {
        val map = LinkedHashMap<ModuleKey, ModuleDefinition>()
        this.definitions.forEach { def ->
            val key = def.descriptor.key
            require(map.putIfAbsent(key, def) == null) {
                "Duplicate module key in catalog: '$key'"
            }
        }
        byKey = map
    }

    override fun get(key: ModuleKey): ModuleDefinition? = byKey[key]

}

class ModuleCatalogBuilder {

    private val definitions = mutableListOf<ModuleDefinition>()

    fun add(definition: ModuleDefinition): ModuleCatalogBuilder {
        definitions.add(definition)
        return this
    }

    fun module(
        key: ModuleKey,
        name: String,
        description: String,
        phase: ModulePhase = ModulePhase.FEATURE,
        priority: Int = 0,
        enabledByDefault: Boolean = true,
        factory: ModuleFactory,
        configure: (ModuleDescriptorBuilder.() -> Unit)? = null,
    ): ModuleCatalogBuilder {
        val descBuilder = ModuleDescriptorBuilder(
            key,
            name,
            description,
            phase,
            priority,
            enabledByDefault
        )
        configure?.let { descBuilder.it() }
        definitions.add(
            ModuleDefinition(
                descBuilder.build(),
                factory
            )
        )
        return this
    }

    fun core(
        key: ModuleKey,
        name: String,
        description: String,
        priority: Int = 0,
        enabledByDefault: Boolean = true,
        factory: ModuleFactory,
        configure: (ModuleDescriptorBuilder.() -> Unit)? = null,
    ): ModuleCatalogBuilder = module(
        key = key,
        name = name,
        description = description,
        phase = ModulePhase.CORE,
        priority = priority,
        enabledByDefault = enabledByDefault,
        factory = factory,
        configure = configure,
    )

    fun feature(
        key: ModuleKey,
        name: String,
        description: String,
        priority: Int = 0,
        enabledByDefault: Boolean = true,
        factory: ModuleFactory,
        configure: (ModuleDescriptorBuilder.() -> Unit)? = null,
    ): ModuleCatalogBuilder = module(
        key = key,
        name = name,
        description = description,
        phase = ModulePhase.FEATURE,
        priority = priority,
        enabledByDefault = enabledByDefault,
        factory = factory,
        configure = configure,
    )

    fun build(): ModuleCatalog = DefaultModuleCatalog(definitions)

}

class ModuleDescriptorBuilder(
    var key: ModuleKey,
    var name: String,
    var description: String,
    var phase: ModulePhase = ModulePhase.FEATURE,
    var priority: Int = 0,
    var enabledByDefault: Boolean = true,
) {

    private val requires = mutableSetOf<ModuleKey>()
    private val optional = mutableSetOf<ModuleKey>()

    fun requires(key: ModuleKey) {
        requires.add(key)
    }

    fun requires(k1: ModuleKey, k2: ModuleKey) {
        requires.add(k1)
        requires.add(k2)
    }

    fun requires(
        k1: ModuleKey,
        k2: ModuleKey,
        k3: ModuleKey
    ) {
        requires.add(k1)
        requires.add(k2)
        requires.add(k3)
    }

    fun requires(
        k1: ModuleKey,
        k2: ModuleKey,
        k3: ModuleKey,
        k4: ModuleKey
    ) {
        requires.add(k1)
        requires.add(k2)
        requires.add(k3)
        requires.add(k4)
    }

    fun requires(keys: Iterable<ModuleKey>) {
        requires.addAll(keys)
    }

    fun requires(vararg keys: String) {
        requires.addAll(keys.map(::ModuleKey))
    }

    fun optional(key: ModuleKey) {
        optional.add(key)
    }

    fun optional(k1: ModuleKey, k2: ModuleKey) {
        optional.add(k1)
        optional.add(k2)
    }

    fun optional(keys: Iterable<ModuleKey>) {
        optional.addAll(keys)
    }

    fun optional(vararg keys: String) {
        optional.addAll(keys.map(::ModuleKey))
    }

    fun build(): ModuleDescriptor = ModuleDescriptor(
        key = key,
        name = name,
        description = description,
        phase = phase,
        priority = priority,
        requires = requires.toSet(),
        optional = optional.toSet(),
        enabledByDefault = enabledByDefault,
    )

}

inline fun moduleCatalog(builderAction: ModuleCatalogBuilder.() -> Unit): ModuleCatalog =
    ModuleCatalogBuilder().apply(builderAction).build()

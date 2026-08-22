package top.likoslupus.cellulosesz.api.module

data class ModuleDefinition(
    val descriptor: ModuleDescriptor,
    val factory: ModuleFactory,
) {

    val key: ModuleKey get() = descriptor.key

}

package top.likoslupus.cellulosesz.core.module

import top.likoslupus.cellulosesz.api.module.ModuleDescriptor
import top.likoslupus.cellulosesz.api.module.ModuleKey

data class ModuleDefinition(
    val descriptor: ModuleDescriptor,
    val factory: ModuleFactory,
) {

    val key: ModuleKey get() = descriptor.key

}

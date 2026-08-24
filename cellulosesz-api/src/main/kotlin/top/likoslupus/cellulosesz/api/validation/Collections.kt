@file:JvmName("Checks")
@file:JvmMultifileClass

package top.likoslupus.cellulosesz.api.validation

public inline fun <T : Collection<*>> T.requireNonEmpty(lazyName: () -> Any): T {
    require(!isEmpty()) {
        "${lazyName()} must not be empty"
    }
    return this
}

public fun <T : Collection<*>> requireNonEmpty(collection: T, name: String): T {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(!collection.isEmpty()) {
        "$name must not be empty"
    }
    return collection
}

public inline fun <M : Map<*, *>> M.requireNonEmpty(lazyName: () -> Any): M {
    require(isNotEmpty()) {
        "${lazyName()} must not be empty"
    }
    return this
}

public fun <M : Map<*, *>> requireNonEmpty(map: M, name: String): M {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(map.isNotEmpty()) {
        "$name must not be empty"
    }
    return map
}

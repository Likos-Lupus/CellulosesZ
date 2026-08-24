package top.likoslupus.cellulosesz.api.util

internal fun <T> Collection<T>.toImmutableList(): List<T> =
    java.util.List.copyOf(this)

internal fun <T> Collection<T>.toImmutableSet(): Set<T> =
    java.util.Set.copyOf(this)

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
    java.util.Map.copyOf(this)

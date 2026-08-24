package top.likoslupus.cellulosesz.api.service

public interface ServiceRegistry {

    public fun <T : Any> register(type: Class<T>, instance: T): Registration =
        register(type, instance, "global")

    public fun <T : Any> register(
        type: Class<T>,
        instance: T,
        owner: String
    ): Registration

    public fun <T : Any> require(type: Class<T>): T

    public fun <T : Any> find(type: Class<T>): T?

    public fun contains(type: Class<*>): Boolean

}

public inline fun <reified T : Any> ServiceRegistry.require(): T = require(T::class.java)

public inline fun <reified T : Any> ServiceRegistry.find(): T? = find(T::class.java)

public inline fun <reified T : Any> ServiceRegistry.contains(): Boolean = contains(T::class.java)

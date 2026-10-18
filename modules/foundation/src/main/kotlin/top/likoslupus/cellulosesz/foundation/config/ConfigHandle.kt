package top.likoslupus.cellulosesz.foundation.config

/**
 * The read/reload surface shared by the single-file [ConfigStore] and the multi-file
 * [BundleConfigStore]. The composition root depends on this interface, so the storage layout can
 * change without touching command or feature code.
 */
public interface ConfigHandle<T> {

    public val initialized: Boolean

    public val current: T

    public val generation: Int

    /** Blocking one-shot boot load. Call from the startup lifecycle, never the server thread. */
    public fun initialLoadBlocking(): ConfigReloadResult

    /** Transactional reload on the store's IO dispatcher. A failure keeps the previous snapshot. */
    public suspend fun reload(): ConfigReloadResult

}

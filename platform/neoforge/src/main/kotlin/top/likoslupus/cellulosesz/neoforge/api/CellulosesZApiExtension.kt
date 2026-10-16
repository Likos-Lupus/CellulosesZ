package top.likoslupus.cellulosesz.neoforge.api

import net.neoforged.fml.IExtensionPoint
import top.likoslupus.cellulosesz.api.CellulosesZApiProvider

/**
 * The NeoForge-only discovery bridge for the CellulosesZ public API.
 *
 * It is registered on the `cellulosesz` `ModContainer` as an FML extension point, so a consumer
 * discovers the provider without reflection or a private global:
 *
 * ```java
 * ModList.get().getModContainerById("cellulosesz")
 *     .flatMap(container -> container.getCustomExtension(CellulosesZApiExtension.class))
 *     .ifPresent(extension -> extension.provider().whenReady(api -> { ... }));
 * ```
 *
 * The type is loader-specific; the `CellulosesZApiProvider` it returns is the same pure-JVM contract
 * on every loader. Consumers that support NeoForge compile against this interface; because it is
 * bundled in the CellulosesZ NeoForge jar it must not be shaded into the consumer (see
 * `docs/api/public-api-v0.md`).
 */
fun interface CellulosesZApiExtension : IExtensionPoint {

    fun provider(): CellulosesZApiProvider

}

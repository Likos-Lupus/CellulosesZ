package top.likoslupus.cellulosesz.core.text.adventure

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.minecraft.server.MinecraftServer
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import java.util.concurrent.atomic.AtomicReference

/**
 * Lifecycle-scoped owner of the Adventure platform audience provider. Centralizes
 * `MinecraftServerAudiences` so no feature code casts Minecraft objects to [Audience] and common
 * code behaves identically on Fabric and NeoForge.
 */
public class AdventureRuntime {

    private val audiences = AtomicReference<MinecraftServerAudiences?>(null)

    public fun start(server: MinecraftServer) {
        val created = MinecraftServerAudiences.of(server)
        check(audiences.compareAndSet(null, created)) {
            "Adventure runtime is already started"
        }
    }

    public fun stop() {
        audiences.getAndSet(null)?.close()
    }

    public fun requireAudiences(): MinecraftServerAudiences =
        audiences.get() ?: error("Adventure runtime is not available")

    public fun audience(target: CommandFeedbackTarget): Audience =
        when (target) {
            is CommandFeedbackTarget.Player -> requireAudiences().player(target.id)
            CommandFeedbackTarget.Server -> requireAudiences().console()
        }

}

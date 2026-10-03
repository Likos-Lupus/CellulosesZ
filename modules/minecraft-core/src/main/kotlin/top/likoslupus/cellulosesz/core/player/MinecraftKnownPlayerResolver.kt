package top.likoslupus.cellulosesz.core.player

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/**
 * Server-thread confined identity resolver backed by the online player list plus an in-memory index
 * seeded by the composition root from joins, bans, mutes and mailbox owners.
 *
 * It deliberately never consults Minecraft's name-based profile cache: on a cache miss that path
 * performs a Mojang network lookup, which command handling must not trigger.
 */
public class MinecraftKnownPlayerResolver(
    private val kernel: RuntimeKernel,
) : KnownPlayerResolver {

    private val byId = HashMap<UUID, String>()
    private val byName = HashMap<String, UUID>()

    public fun record(identity: KnownPlayerIdentity) {
        byId[identity.id] = identity.name
        byName[identity.name.lowercase()] = identity.id
    }

    public fun clear() {
        byId.clear()
        byName.clear()
    }

    override fun onlineByName(name: String): KnownPlayerIdentity? =
        recordAndReturn(kernel.requireServer().playerList.getPlayerByName(name))

    override fun onlineById(id: UUID): KnownPlayerIdentity? =
        recordAndReturn(kernel.requireServer().playerList.getPlayer(id))

    private fun recordAndReturn(player: ServerPlayer?): KnownPlayerIdentity? =
        player?.let {
            KnownPlayerIdentity(
                it.uuid,
                it.gameProfile.name
            ).also(::record)
        }

    override fun knownByName(name: String): KnownPlayerIdentity? {
        val id = byName[name.lowercase()]
            ?: return null
        val resolved = byId[id]
            ?: return null
        return KnownPlayerIdentity(id, resolved)
    }

    override fun knownById(id: UUID): KnownPlayerIdentity? =
        byId[id]?.let { KnownPlayerIdentity(id, it) }

}

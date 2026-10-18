package top.likoslupus.cellulosesz.player.whois

import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.model.DetailField
import top.likoslupus.cellulosesz.player.afk.AfkState
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/** A live snapshot of an online target's runtime state, gathered on the server thread. */
internal data class WhoisLive(
    val health: Float,
    val maxHealth: Float,
    val food: Int,
    val experienceLevel: Int,
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val gameMode: String,
    val operator: Boolean,
    val whitelisted: Boolean,
    val afk: Boolean,
    /** The remote address, or null when the viewer lacks `WHOIS_IP`. */
    val address: String?,
)

/** The extra whois data beyond the durable profile: live state plus contributor fields. */
internal data class WhoisEnrichment(
    val live: WhoisLive?,
    val extra: List<DetailField>,
)

/** Ordered contributor list; the composition root fills it through the feature facade. */
internal class WhoisContributorRegistry {

    private val contributors = CopyOnWriteArrayList<WhoisContributor>()

    fun register(contributor: WhoisContributor) {
        contributors += contributor
    }

    suspend fun fields(playerId: UUID): List<DetailField> =
        contributors.flatMap { it.fields(playerId) }

}

/**
 * Gathers the live and contributed `/whois` fields. Offline targets yield no live fields;
 * contributor fields are always collected, so a feature can report durable facts it already holds.
 */
internal class WhoisService(
    private val kernel: RuntimeKernel,
    private val contributors: WhoisContributorRegistry,
) {

    suspend fun enrichment(playerId: UUID, showIp: Boolean): WhoisEnrichment {
        val live = kernel.onServerThread {
            val player = kernel.requireServer().playerList.getPlayer(playerId)
            player?.let { snapshot(it.uuid, it.gameProfile.name, it, showIp) }
        }
        return WhoisEnrichment(
            live = live,
            extra = contributors.fields(playerId),
        )
    }

    private fun snapshot(
        playerId: UUID,
        name: String,
        player: net.minecraft.server.level.ServerPlayer,
        showIp: Boolean,
    ): WhoisLive {
        val server = kernel.requireServer()
        val target = net.minecraft.server.players.NameAndId(playerId, name)
        val position = player.blockPosition()
        return WhoisLive(
            health = player.health,
            maxHealth = player.maxHealth,
            food = player.getFoodData().foodLevel,
            experienceLevel = player.experienceLevel,
            dimension = player.level().dimension().identifier().toString(),
            x = position.x,
            y = position.y,
            z = position.z,
            gameMode = player.gameMode().getName(),
            operator = server.playerList.isOp(target),
            whitelisted = server.playerList.isWhiteListed(target),
            afk = AfkState.isAfk(playerId),
            address = if (showIp) player.ipAddress else null,
        )
    }

}

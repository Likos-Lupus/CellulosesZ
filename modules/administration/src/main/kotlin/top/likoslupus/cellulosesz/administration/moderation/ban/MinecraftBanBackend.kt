package top.likoslupus.cellulosesz.administration.moderation.ban

import net.minecraft.network.chat.Component
import net.minecraft.server.players.IpBanListEntry
import net.minecraft.server.players.NameAndId
import net.minecraft.server.players.UserBanListEntry
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.time.Instant
import java.util.*

/**
 * Native Minecraft ban-list backend. This is the single source of truth for enforcement: login
 * rejection, restart persistence, and `/pardon` interoperability all come from vanilla. Must run on
 * the server thread.
 */
internal class MinecraftBanBackend(
    private val kernel: RuntimeKernel,
) : BanBackend {

    override fun banAccount(
        target: PlayerIdentity,
        actor: ModerationActor,
        reason: ModerationReason,
        expiresAt: Instant?,
    ): BanBackendResult {
        val server = kernel.requireServer()
        val list = server.playerList.bans
        val nameAndId = NameAndId(target.id, target.name)
        if (list.isBanned(nameAndId)) {
            return BanBackendResult.AlreadyBanned
        }

        list.add(
            UserBanListEntry(
                nameAndId,
                Date.from(Instant.now()),
                actorName(actor),
                expiresAt?.let(Date::from),
                reason.value,
            )
        )
        server.playerList.getPlayer(target.id)?.connection?.disconnect(
            banMessage(
                reason,
                expiresAt
            )
        )
        return BanBackendResult.Applied
    }

    override fun unbanAccount(target: PlayerIdentity): Boolean =
        kernel.requireServer().playerList.bans.remove(
            NameAndId(target.id, target.name)
        )

    override fun listedAccountIdentities(): List<PlayerIdentity> =
        kernel.requireServer().playerList.bans.entries.mapNotNull { entry ->
            entry.user?.let {
                PlayerIdentity(
                    it.id(),
                    it.name()
                )
            }
        }

    override fun banIp(
        address: IpAddress,
        actor: ModerationActor,
        reason: ModerationReason,
        expiresAt: Instant?,
    ): BanBackendResult {
        val server = kernel.requireServer()
        val list = server.playerList.ipBans
        if (list.isBanned(address.value)) {
            return BanBackendResult.AlreadyBanned
        }

        list.add(
            IpBanListEntry(
                address.value,
                Date.from(Instant.now()),
                actorName(actor),
                expiresAt?.let(Date::from),
                reason.value,
            )
        )
        server.playerList.players
                .filter { it.ipAddress == address.value }
                .forEach { it.connection.disconnect(banMessage(reason, expiresAt)) }
        return BanBackendResult.Applied
    }

    override fun unbanIp(address: IpAddress): Boolean =
        kernel.requireServer().playerList.ipBans.remove(address.value)

    private fun actorName(actor: ModerationActor): String =
        when (actor) {
            ModerationActor.Console -> "Server"
            is ModerationActor.Player -> actor.name
        }

    private fun banMessage(
        reason: ModerationReason,
        expiresAt: Instant?
    ): Component =
        Component.literal(
            when (expiresAt) {
                null -> "Banned: ${reason.value}"
                else -> "Banned until $expiresAt: ${reason.value}"
            }
        )

}

package top.likoslupus.cellulosesz.administration.moderation.ban

import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import java.time.Instant

internal sealed interface BanBackendResult {

    data object Applied : BanBackendResult
    data object AlreadyBanned : BanBackendResult

}

/**
 * The only seam that may touch Minecraft's native account/IP ban lists. Version and mapping
 * differences stay here; the domain service only sees this interface.
 */
internal interface BanBackend {

    fun banAccount(
        target: PlayerIdentity,
        actor: ModerationActor,
        reason: ModerationReason,
        expiresAt: Instant?,
    ): BanBackendResult

    fun unbanAccount(target: PlayerIdentity): Boolean

    /** Identities currently present in the native account ban list. Must run on the server thread. */
    fun listedAccountIdentities(): List<PlayerIdentity>

    fun banIp(
        address: IpAddress,
        actor: ModerationActor,
        reason: ModerationReason,
        expiresAt: Instant?,
    ): BanBackendResult

    fun unbanIp(address: IpAddress): Boolean

}

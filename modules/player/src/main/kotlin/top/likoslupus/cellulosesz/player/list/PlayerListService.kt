package top.likoslupus.cellulosesz.player.list

import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.permission.hasPermission
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.player.afk.AfkService
import top.likoslupus.cellulosesz.player.config.PlayerListGroup
import top.likoslupus.cellulosesz.player.config.PlayerSettings
import java.util.*

/** One online player, reduced to the values `/list` groups and renders. */
internal data class PlayerListCandidate(
    val id: UUID,
    val name: String,
    val afk: Boolean,
)

/** A resolved group: the label to render plus the permission a member must hold, if any. */
internal data class PlayerListGroupSpec(
    val id: String,
    val label: String,
    val permission: String?,
)

/** One rendered `/list` row: the player plus the group they landed in. */
internal data class PlayerListEntry(
    val id: UUID,
    val name: String,
    val afk: Boolean,
    val groupId: String,
    val groupLabel: String,
)

/** The outcome of a `/list` query. */
internal sealed interface PlayerListResult {

    data class Rows(
        val entries: List<PlayerListEntry>,
        val online: Int,
        val maximum: Int,
        val filterLabel: String?,
    ) : PlayerListResult

    data object UnknownGroup : PlayerListResult

}

/**
 * Pure grouping rule: a player joins the first configured group whose permission they hold, or the
 * default group. Rows keep group order, then name order; a filter narrows to one group.
 */
internal object PlayerListGrouper {

    fun group(
        candidates: List<PlayerListCandidate>,
        groups: List<PlayerListGroupSpec>,
        defaultGroup: PlayerListGroupSpec,
        filterGroupId: String?,
        holds: (UUID, String) -> Boolean,
    ): List<PlayerListEntry> {
        val ordered = orderedGroups(groups, defaultGroup)
        val assigned = candidates.map { candidate ->
            val group = ordered.firstOrNull { spec ->
                spec.permission != null
                        && holds(candidate.id, spec.permission)
            } ?: defaultGroup
            candidate to group
        }
        val effectiveFilter = filterGroupId?.lowercase(Locale.ROOT)
        return ordered
                .flatMap { group ->
                    assigned
                            .filter { (_, assignedGroup) -> assignedGroup.id == group.id }
                            .sortedBy { (candidate, _) -> candidate.name.lowercase(Locale.ROOT) }
                            .map { (candidate, _) ->
                                PlayerListEntry(
                                    id = candidate.id,
                                    name = candidate.name,
                                    afk = candidate.afk,
                                    groupId = group.id,
                                    groupLabel = group.label,
                                )
                            }
                }
                .filter { entry ->
                    effectiveFilter == null
                            || entry.groupId.lowercase(Locale.ROOT) == effectiveFilter
                }
    }

    /** The configured groups plus the default, without duplicating a shared id. */
    fun orderedGroups(
        groups: List<PlayerListGroupSpec>,
        defaultGroup: PlayerListGroupSpec,
    ): List<PlayerListGroupSpec> =
        when {
            groups.any { it.id == defaultGroup.id } -> groups
            else -> groups + defaultGroup
        }

    fun toSpec(group: PlayerListGroup): PlayerListGroupSpec =
        PlayerListGroupSpec(
            id = group.id,
            label = group.label,
            permission = group.permission.ifBlank { null },
        )

}

/**
 * Server-thread confined `/list` snapshot. Vanish visibility is supplied by the composition root as
 * a `(viewerId, targetId) -> Boolean` query so the player module never depends on the vanish owner.
 * A console source (null viewer) sees everything.
 */
internal class PlayerListService(
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val afk: AfkService,
    private val settings: () -> PlayerSettings,
) {

    fun list(
        viewerId: UUID?,
        includeVanished: Boolean,
        requestedGroup: String?,
        visibility: (UUID, UUID) -> Boolean,
    ): PlayerListResult {
        val server = kernel.requireServer()
        val config = settings().list
        val groups = config.groups.map(PlayerListGrouper::toSpec)
        val defaultGroup = PlayerListGrouper.toSpec(config.defaultGroup)
        val ordered = PlayerListGrouper.orderedGroups(groups, defaultGroup)

        val filter = requestedGroup
                ?.takeIf { it.isNotBlank() }
                ?.let { raw ->
                    ordered.firstOrNull {
                        it.id.equals(raw, ignoreCase = true)
                                || it.label.equals(raw, ignoreCase = true)
                    }
                }
        if (!requestedGroup.isNullOrBlank() && filter == null) {
            return PlayerListResult.UnknownGroup
        }

        val candidates = server.playerList.players
                .filter { player ->
                    includeVanished
                            || viewerId == null
                            || visibility(viewerId, player.uuid)
                }
                .map { player ->
                    PlayerListCandidate(
                        id = player.uuid,
                        name = player.gameProfile.name,
                        afk = afk.isAfk(player.uuid),
                    )
                }

        val specByPermission = ordered
                .mapNotNull { spec -> spec.permission?.let { it to spec } }
                .associate { (permission, _) ->
                    permission to PermissionSpec(
                        node = permission,
                        fallback = VanillaPermissionFallback.DENY_ALL,
                        description = "List group membership",
                    )
                }

        val entries = PlayerListGrouper.group(
            candidates = candidates,
            groups = groups,
            defaultGroup = defaultGroup,
            filterGroupId = filter?.id,
            holds = { playerId, permission ->
                val player = server.playerList.getPlayer(playerId)
                    ?: return@group false
                player.createCommandSourceStack()
                        .hasPermission(
                            permissions,
                            specByPermission.getValue(permission)
                        )
            },
        )

        return PlayerListResult.Rows(
            entries = entries,
            online = candidates.size,
            maximum = server.playerList.maxPlayers,
            filterLabel = filter?.label,
        )
    }

}

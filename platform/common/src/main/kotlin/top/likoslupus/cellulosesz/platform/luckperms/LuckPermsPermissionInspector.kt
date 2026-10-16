package top.likoslupus.cellulosesz.platform.luckperms

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.luckperms.api.util.Tristate
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.permission.*

/**
 * Optional native LuckPerms diagnostics, shared by both loader cells.
 *
 * LuckPerms' API is `compileOnly`: this class is only ever instantiated after the loader confirms the
 * LuckPerms mod is present, so a server without LuckPerms never links it. The platform permission
 * bridge stays the sole authority for the effective decision ([PermissionInspection.effectiveAllowed]);
 * the LuckPerms value is reported alongside it and is never used to override it.
 *
 * The class deliberately holds no live player or world reference; each [inspect] call reads the
 * current online context and returns an immutable snapshot.
 */
class LuckPermsPermissionInspector(
    private val bridge: PermissionBridge,
) : PermissionInspector, AutoCloseable {

    private val luckPerms: LuckPerms? = runCatching { LuckPermsProvider.get() }.getOrNull()

    /**
     * Subscribed only while a cache-invalidating consumer exists. Today there is none, so the
     * subscription is a no-op placeholder that is still closed on shutdown; wire derived caches here
     * (invalidating by UUID on the server thread) when a consumer is added.
     */
    private val subscription =
        luckPerms?.eventBus?.subscribe(UserDataRecalculateEvent::class.java) { }

    override fun inspect(
        source: CommandSourceStack,
        spec: PermissionSpec,
    ): PermissionInspection {
        val effective = bridge.test(source, spec)
        val vanilla = vanillaFallback(source, spec)
        val api = luckPerms
        val player = source.player

        if (api == null) {
            return snapshot(
                spec,
                effective,
                vanilla,
                PermissionOpinion.UNKNOWN,
                emptyMap(),
                when (player) {
                    null -> PermissionBackendStatus.VANILLA
                    else -> PermissionBackendStatus.LUCKPERMS_UNAVAILABLE
                },
            )
        }
        if (player == null) {
            return snapshot(
                spec,
                effective,
                vanilla,
                PermissionOpinion.UNKNOWN,
                emptyMap(),
                PermissionBackendStatus.LUCKPERMS_ACTIVE,
            )
        }

        val user = api.userManager.getUser(player.uuid)
            ?: return snapshot(
                spec,
                effective,
                vanilla,
                PermissionOpinion.UNKNOWN,
                emptyMap(),
                PermissionBackendStatus.LUCKPERMS_NOT_READY,
            )

        val options = api.contextManager.getQueryOptions(player)
        val opinion = when (user.cachedData.getPermissionData(options).checkPermission(spec.node)) {
            Tristate.TRUE -> PermissionOpinion.ALLOW
            Tristate.FALSE -> PermissionOpinion.DENY
            Tristate.UNDEFINED -> PermissionOpinion.UNDEFINED
        }
        return snapshot(
            spec,
            effective,
            vanilla,
            opinion,
            options.context().toMap(),
            PermissionBackendStatus.LUCKPERMS_ACTIVE,
        )
    }

    private fun snapshot(
        spec: PermissionSpec,
        effective: Boolean,
        vanilla: Boolean,
        opinion: PermissionOpinion,
        context: Map<String, Set<String>>,
        status: PermissionBackendStatus,
    ): PermissionInspection =
        PermissionInspection(
            node = spec.node,
            effectiveAllowed = effective,
            vanillaDefaultAllowed = vanilla,
            platformBackend = bridge.backendName,
            luckPermsOpinion = opinion,
            contextSummary = context,
            status = status,
        )

    override fun close() {
        subscription?.close()
    }

}

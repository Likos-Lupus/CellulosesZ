package top.likoslupus.cellulosesz.movement

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.VanillaFallbackPermissionBridge
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.home.HomeCommands
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.home.JdbcHomeRepository
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.random.*
import top.likoslupus.cellulosesz.movement.request.JdbcTpAutoRepository
import top.likoslupus.cellulosesz.movement.request.TeleportRequestCommands
import top.likoslupus.cellulosesz.movement.request.TeleportRequestService
import top.likoslupus.cellulosesz.movement.request.TpAutoService
import top.likoslupus.cellulosesz.movement.spawn.JdbcSpawnRepository
import top.likoslupus.cellulosesz.movement.spawn.SpawnCommands
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportCommands
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.JdbcTeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import top.likoslupus.cellulosesz.movement.teleport.safety.SafeDestinationResolver
import top.likoslupus.cellulosesz.movement.terrain.MinecraftTerrainBackend
import top.likoslupus.cellulosesz.movement.terrain.TerrainBackend
import top.likoslupus.cellulosesz.movement.terrain.TerrainDestinationFinder
import top.likoslupus.cellulosesz.movement.terrain.command.TerrainCommands
import top.likoslupus.cellulosesz.movement.warp.JdbcWarpRepository
import top.likoslupus.cellulosesz.movement.warp.WarpCommands
import top.likoslupus.cellulosesz.movement.warp.WarpService
import java.util.*

/**
 * Public surface of the movement bounded context. Everything else in this module is `internal`, so
 * the application only sees command registration, movement events, disconnect handling, and config.
 */
public class MovementFeature internal constructor(
    private val teleports: TeleportCoordinator,
    private val homes: HomeService,
    private val warps: WarpService,
    private val spawn: SpawnService,
    private val requests: TeleportRequestService,
    private val tpAuto: TpAutoService,
    private val requestReachability: RequestReachabilityPort,
    private val pending: PendingTeleportService,
    private val backend: TeleportBackend,
    private val history: TeleportHistoryService,
    private val randomTeleports: RandomTeleportService,
    private val terrain: TerrainBackend,
    private val settings: () -> MovementSettings,
    private val kernel: RuntimeKernel,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
    private val permissions: PermissionService,
) {

    public fun commands(): List<CommandDefinition> {
        val teleportSettings = { settings().teleport }
        return HomeCommands.commands(
            homes,
            backend,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation
        ) + WarpCommands.commands(
            warps,
            backend,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
        ) + SpawnCommands.commands(
            spawn,
            backend,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation
        ) + TeleportRequestCommands.commands(
            requests,
            tpAuto,
            teleports,
            teleportSettings,
            requestReachability,
            kernel,
            messages,
            adventure,
            presentation
        ) + TeleportCommands.commands(
            teleports,
            history,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation
        ) + RandomTeleportCommands.commands(
            randomTeleports,
            teleports,
            teleportSettings,
            { settings().randomTeleport },
            kernel,
            messages,
            adventure,
            presentation
        ) + TerrainCommands.commands(
            terrain,
            teleports,
            teleportSettings,
            { settings().terrain },
            kernel,
            messages,
            adventure,
            presentation
        )
    }

    /** Localized notice for a requester whose pending teleport request was cancelled because the target left. */
    public fun requestTargetOfflineNotice(): Notice =
        MovementMessages.requestTargetOffline()

    /** Cancels a delayed teleport when the player moves. Must run on the server thread. */
    public fun onPlayerTick(player: ServerPlayer) {
        pending.onTick(player.uuid, player.toStoredPosition())
    }

    /** Cancels a delayed teleport when the player takes damage. Must run on the server thread. */
    public fun onPlayerHurt(playerId: UUID) {
        pending.onDamage(playerId)
    }

    /** Cancels a delayed teleport and clears request state for a leaving player. */
    public fun onPlayerQuit(playerId: UUID): List<UUID> {
        pending.cancel(playerId, TeleportCancellation.DISCONNECTED)
        return requests.clearPlayer(playerId)
    }

    /** Loads the durable auto-accept index before any command can read it. Runs on the server thread. */
    public fun onServerStarting() {
        tpAuto.hydrate()
    }

    /** Cancels every delayed teleport when the server stops. */
    public fun onServerStopping() {
        pending.cancelAll(TeleportCancellation.SERVER_STOPPING)
    }

    /**
     * Moves a player to an explicit destination through the single teleport path. Used by the
     * composition root to adapt administration's jail transfer port. Runs on the server thread.
     */
    public suspend fun teleportPlayerTo(
        playerId: UUID,
        dimension: String,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
        cause: MovementTeleportCause,
    ): MovementTransferResult {
        val internal = cause.toInternal()
        return teleports.execute(
            TeleportIntent(
                subjectId = playerId,
                destination = TeleportDestination.Fixed(
                    StoredPosition(
                        dimension = dimension,
                        x = x,
                        y = y,
                        z = z,
                        yaw = yaw,
                        pitch = pitch,
                    )
                ),
                cause = internal,
                policy = teleportPolicyFor(internal, settings().teleport),
            )
        ).toTransferResult()
    }

    /**
     * Moves a player to the server spawn through the single teleport path. Used to return a released
     * player. Runs on the server thread.
     */
    public suspend fun teleportPlayerToSpawn(
        playerId: UUID,
        cause: MovementTeleportCause,
    ): MovementTransferResult {
        val internal = cause.toInternal()
        val destination = backend.vanillaSpawnPosition()
        return teleports.execute(
            TeleportIntent(
                subjectId = playerId,
                destination = TeleportDestination.Fixed(destination),
                cause = internal,
                policy = teleportPolicyFor(internal, settings().teleport),
            )
        ).toTransferResult()
    }

}

public fun createMovementFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    settings: () -> MovementSettings,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    confinement: TeleportConfinementCheck = TeleportConfinementCheck { false },
    requestReachability: RequestReachabilityPort = RequestReachabilityPort { _, _ -> true },
    permissions: PermissionService = PermissionService(VanillaFallbackPermissionBridge),
): MovementFeature {
    val backend = MinecraftTeleportBackend(kernel, SafeDestinationResolver())
    val pending = PendingTeleportService()
    val cooldowns = TeleportCooldowns()
    val history = TeleportHistoryService(JdbcTeleportHistoryRepository(database, namespace))
    val teleports = TeleportCoordinator(
        backend = backend,
        pending = pending,
        cooldowns = cooldowns,
        history = history,
        historyEnabled = { settings().history.enabled },
        confinement = confinement::isConfined,
    )

    val terrainFinder = TerrainDestinationFinder()
    val terrain = MinecraftTerrainBackend(kernel, terrainFinder)
    val randomTeleports = RandomTeleportService(
        regions = JdbcRandomTeleportRegionRepository(database, namespace),
        backend = MinecraftRandomTeleportBackend(kernel, terrainFinder, SafeDestinationResolver()),
        planner = RandomTeleportPlanner(),
        settings = { settings().randomTeleport },
    )

    return MovementFeature(
        teleports = teleports,
        homes = HomeService(JdbcHomeRepository(database, namespace)) { settings().homes },
        warps = WarpService(JdbcWarpRepository(database, namespace)),
        spawn = SpawnService(JdbcSpawnRepository(database, namespace)),
        requests = TeleportRequestService(settings = { settings().requests }),
        tpAuto = TpAutoService(
            repository = JdbcTpAutoRepository(database, namespace),
            settings = { settings().requests },
        ),
        requestReachability = requestReachability,
        pending = pending,
        backend = backend,
        history = history,
        randomTeleports = randomTeleports,
        terrain = terrain,
        settings = settings,
        kernel = kernel,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
        permissions = permissions,
    )
}

private fun MovementTeleportCause.toInternal(): TeleportCause =
    when (this) {
        MovementTeleportCause.ADMIN -> TeleportCause.ADMIN
        MovementTeleportCause.CONFINEMENT -> TeleportCause.JAIL_CONFINEMENT
        MovementTeleportCause.CORRECTION -> TeleportCause.JAIL_CORRECTION
        MovementTeleportCause.RELEASE -> TeleportCause.JAIL_RELEASE
    }

private fun TeleportOutcome.toTransferResult(): MovementTransferResult =
    when (this) {
        is TeleportOutcome.Success ->
            MovementTransferResult.Success

        TeleportOutcome.PlayerOffline ->
            MovementTransferResult.PlayerOffline

        TeleportOutcome.TargetOffline ->
            MovementTransferResult.Failed("movement.teleport.error.target_offline")

        is TeleportOutcome.UnknownDimension ->
            MovementTransferResult.Failed("movement.teleport.error.unknown_dimension")

        TeleportOutcome.OutsideWorldBorder ->
            MovementTransferResult.Failed("movement.teleport.error.outside_border")

        TeleportOutcome.UnsafeDestination ->
            MovementTransferResult.Failed("movement.teleport.error.unsafe")

        is TeleportOutcome.Cooldown ->
            MovementTransferResult.Failed("movement.teleport.error.cooldown")

        TeleportOutcome.AlreadyPending ->
            MovementTransferResult.Failed("movement.teleport.error.already_pending")

        TeleportOutcome.Confined ->
            MovementTransferResult.Failed("movement.teleport.error.confined")

        is TeleportOutcome.Cancelled ->
            MovementTransferResult.Failed("movement.teleport.error.cancelled")

        TeleportOutcome.PassengerConflict ->
            MovementTransferResult.Failed("movement.teleport.error.passenger")

        TeleportOutcome.RuntimeStopping ->
            MovementTransferResult.Failed("core.error.runtime_stopping")
    }

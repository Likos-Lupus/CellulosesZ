package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.*
import dev.architectury.platform.Platform
import kotlinx.coroutines.Dispatchers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.administration.createAdministrationFeature
import top.likoslupus.cellulosesz.application.command.RootCommand
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.config.ConfigDefaults
import top.likoslupus.cellulosesz.application.config.ConfigValidation
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.communication.CommunicationSqlSchema
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.createCommunicationFeature
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.IdentitySqlSchema
import top.likoslupus.cellulosesz.core.player.identity.JdbcPlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.TranslationCatalog
import top.likoslupus.cellulosesz.core.text.i18n.preference.JdbcPlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.LocalizationSqlSchema
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.toMessageTheme
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore
import top.likoslupus.cellulosesz.foundation.config.TomlConfigCodec
import top.likoslupus.cellulosesz.foundation.database.DeferredDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.schema.SqlSchemaContributor
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.createMovementFeature
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import top.likoslupus.cellulosesz.utility.createUtilityFeature
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The only composition root. It explicitly wires config, runtime, storage, shared identity lookup and
 * the bounded-context facades; it contains no domain logic.
 */
public object CellulosesZ {

    private val initialized = AtomicBoolean(false)

    private val businessContributors: List<SqlSchemaContributor> = listOf(
        IdentitySqlSchema,
        LocalizationSqlSchema,
        MovementSqlSchema,
        CommunicationSqlSchema,
        AdministrationSqlSchema,
        UtilitySqlSchema,
    )

    public fun initialize(platform: PlatformServices) {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        val health = ApplicationHealth().apply {
            loader = platform.loaderName
            permissionBackend = platform.permissionBridge.backendName
        }
        val kernel = RuntimeKernel()
        val permissions = PermissionService(platform.permissionBridge)

        val configPath = configPath()
        ConfigDefaults.writeIfMissing(configPath)
        val config = ConfigStore(
            path = configPath,
            codec = TomlConfigCodec(CellulosesConfig.serializer()),
            validate = ConfigValidation::validate,
            validateTransition = ConfigValidation.databaseTransition { dataRoot(kernel) },
            ioDispatcher = Dispatchers.IO,
        )
        when (val initial = config.initialLoadBlocking()) {
            is ConfigReloadResult.Success -> Unit
            is ConfigReloadResult.Failure -> {
                val message = initial.errors.joinToString("; ") {
                    "${it.path}: ${it.message}"
                }
                health.fail("initial config load failed: $message")
                error("initial config load failed: $message")
            }
        }
        health.transition(ApplicationHealth.State.CONFIG_READY)

        val database = DeferredDatabaseRuntime()
        val namespace = config.current.database.namespace
        val known = MinecraftKnownPlayerResolver(kernel)
        val identityRepository = JdbcPlayerIdentityRepository(database, namespace)

        val catalog = TranslationCatalog.loadBundled()
        val languageRepository = JdbcPlayerLanguagePreferenceRepository(database, namespace)
        val languagePreferences = PlayerLanguagePreferences(languageRepository)
        val languageResolver = LanguageResolver(catalog, languagePreferences) {
            config.current.localization
        }
        val messages = LocalizedMessages(catalog, languageResolver) {
            config.current.text.colors.toMessageTheme()
        }
        val adventure = AdventureRuntime()

        val movement = createMovementFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.movement },
        )
        val administration = createAdministrationFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.administration },
            known = known,
        )
        val communication = createCommunicationFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.messaging },
            known = known,
        )
        val utility = createUtilityFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.utility },
            known = known,
        )

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            RootCommand.register(
                dispatcher = dispatcher,
                config = config,
                kernel = kernel,
                health = health,
                permissions = permissions,
                messages = messages,
                adventure = adventure,
                languages = languagePreferences,
                resolver = languageResolver,
                catalog = catalog,
            )
            movement.registerCommands(dispatcher)
            communication.registerCommands(
                dispatcher = dispatcher,
                integration = CommunicationIntegration(
                    senderGate = {
                        when {
                            administration.isMuted(it) -> SendGateResult.Denied(
                                Messages.prefixed("you are muted")
                            )

                            else -> SendGateResult.Allowed
                        }
                    },
                    targetReachability = { senderId, targetId ->
                        administration.canBeSeenBy(senderId, targetId)
                    },
                    observer = {
                        administration.observePrivateMessage(
                            senderId = it.senderId,
                            senderName = it.senderName,
                            targetId = it.targetId,
                            targetName = it.targetName,
                            text = it.text,
                        )
                    },
                ),
            )
            administration.registerCommands(dispatcher)
            utility.registerCommands(dispatcher)
        }

        LifecycleEvent.SERVER_STARTING.register { server ->
            kernel.onServerStarting(server)
            adventure.start(server)
            health.transition(ApplicationHealth.State.BOOTSTRAPPING_STORAGE)

            val bootstrap = StorageBootstrap(dataRoot(kernel), businessContributors)
            val result = bootstrap.startBlocking(config.current.database)
            database.initialize(result.database)
            health.storageType = result.descriptor.type.name.lowercase()
            health.storageEndpoint = result.descriptor.redactedEndpoint
            health.migration = result.migration
            health.transition(ApplicationHealth.State.STORAGE_READY)

            val identities = identityRepository.loadAllBlocking()
            known.hydrate(identities.map { KnownPlayerIdentity(it.id, it.name) })
            health.identityRecords = identities.size

            languagePreferences.hydrate(languageRepository.loadAllBlocking())

            administration.onServerStarting()
            communication.onServerStarting()
            utility.onServerStarting()
            health.transition(ApplicationHealth.State.LOADING_STATE)
        }
        LifecycleEvent.SERVER_STARTED.register {
            kernel.onServerStarted()
            health.transition(ApplicationHealth.State.READY)
        }
        LifecycleEvent.SERVER_STOPPING.register {
            health.transition(ApplicationHealth.State.STOPPING)
            movement.onServerStopping()
            communication.onServerStopping()
            administration.onServerStopping()
            utility.onServerStopping()
            database.close()
            kernel.onServerStopping()
            health.transition(ApplicationHealth.State.STOPPED)
        }
        LifecycleEvent.SERVER_STOPPED.register {
            adventure.stop()
            kernel.onServerStopped()
        }

        TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer) {
                movement.onPlayerTick(player)
            }
        }
        EntityEvent.LIVING_HURT.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                movement.onPlayerHurt(entity.uuid)
            }
            EventResult.pass()
        }

        PlayerEvent.PLAYER_JOIN.register { player ->
            administration.onPlayerJoined(player)
            communication.onPlayerJoined(player.uuid)
            val identity = KnownPlayerIdentity(player.uuid, player.gameProfile.name)
            known.record(identity)
            kernel.launch {
                runCatching {
                    identityRepository.observe(
                        identity,
                        Instant.now()
                    )
                }
            }
        }

        ChatEvent.RECEIVED.register { player, _ ->
            if (player != null
                && administration.isMuted(player.uuid)
            ) {
                player.sendSystemMessage(Messages.prefixed("you are muted"))
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }

        PlayerEvent.PLAYER_QUIT.register {
            administration.onPlayerQuit(it.uuid)
            communication.onPlayerQuit(it.uuid)
            val affected = movement.onPlayerQuit(it.uuid)
            if (kernel.state == KernelState.RUNNING) {
                affected.forEach { senderId ->
                    PlayerResolver.onlineById(
                        kernel.requireServer(),
                        senderId
                    )?.sendSystemMessage(
                        Messages.prefixed("teleport request cancelled: target went offline")
                    )
                }
            }
        }
    }

    private fun configPath(): Path =
        Platform.getConfigFolder()
                .resolve("cellulosesz")
                .resolve("cellulosesz.toml")

    private fun dataRoot(kernel: RuntimeKernel): Path =
        kernel.requireServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("cellulosesz")

}

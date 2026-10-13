package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.*
import dev.architectury.platform.Platform
import kotlinx.coroutines.Dispatchers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import top.likoslupus.cellulosesz.administration.AdministrationFeature
import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.administration.createAdministrationFeature
import top.likoslupus.cellulosesz.application.command.RootCommand
import top.likoslupus.cellulosesz.application.command.help.SelectiveVanillaHelpOverlay
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.config.ConfigDefaults
import top.likoslupus.cellulosesz.application.config.ConfigValidation
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.application.port.MovementJailTransferPort
import top.likoslupus.cellulosesz.communication.*
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.help.CommandDocumentationService
import top.likoslupus.cellulosesz.core.command.help.CommandHelpResolver
import top.likoslupus.cellulosesz.core.command.registry.CommandRegistry
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.IdentitySqlSchema
import top.likoslupus.cellulosesz.core.player.identity.JdbcPlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.document.ClasspathCommandDocumentSource
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentLoader
import top.likoslupus.cellulosesz.core.text.i18n.*
import top.likoslupus.cellulosesz.core.text.i18n.preference.JdbcPlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.LocalizationSqlSchema
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.presentation.createTextPresentation
import top.likoslupus.cellulosesz.core.text.theme.TextThemeResolver
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore
import top.likoslupus.cellulosesz.foundation.config.TomlConfigCodec
import top.likoslupus.cellulosesz.foundation.database.DeferredDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.schema.SqlSchemaContributor
import top.likoslupus.cellulosesz.movement.MovementFeature
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.createMovementFeature
import top.likoslupus.cellulosesz.player.PlayerFeature
import top.likoslupus.cellulosesz.player.PlayerSqlSchema
import top.likoslupus.cellulosesz.player.createPlayerFeature
import top.likoslupus.cellulosesz.utility.UtilityFeature
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import top.likoslupus.cellulosesz.utility.createUtilityFeature
import top.likoslupus.cellulosesz.utility.item.AdministrativeActionAuditPort
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
        PlayerSqlSchema,
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

        val configDirectory = configDirectory()
        val configPath = configDirectory.resolve("cellulosesz.toml")
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

        val catalog = TranslationCatalog.load(
            listOf(
                TranslationSource("core", BundledTranslations::class.java),
                TranslationSource("movement", MovementFeature::class.java),
                TranslationSource("communication", CommunicationFeature::class.java),
                TranslationSource("administration", AdministrationFeature::class.java),
                TranslationSource("utility", UtilityFeature::class.java),
                TranslationSource("player", PlayerFeature::class.java),
            )
        )
        val languageRepository = JdbcPlayerLanguagePreferenceRepository(database, namespace)
        val languagePreferences = PlayerLanguagePreferences(languageRepository)
        val languageResolver = LanguageResolver(catalog, languagePreferences) {
            config.current.localization
        }
        val presentationTheme = { TextThemeResolver.resolve(config.current.text) }
        val messages = LocalizedMessages(
            catalog,
            languageResolver,
            presentationTheme
        )
        val presentation = createTextPresentation(
            messages,
            languageResolver,
            presentationTheme
        )
        val adventure = AdventureRuntime()

        lateinit var movement: MovementFeature
        val jailTransferPort = MovementJailTransferPort { movement }
        val administration = createAdministrationFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.administration },
            known = known,
            jailTransfer = { jailTransferPort },
            messages = messages,
            adventure = adventure,
            presentation = presentation,
        )
        movement = createMovementFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            settings = { config.current.movement },
            messages = messages,
            adventure = adventure,
            presentation = presentation,
            confinement = { administration.isConfined(it) },
            requestReachability = { senderId, targetId ->
                administration.canBeSeenBy(senderId, targetId)
            },
            permissions = permissions,
        )
        val communication = createCommunicationFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            permissions = permissions,
            settings = { config.current.messaging },
            known = known,
            messages = messages,
            adventure = adventure,
            presentation = presentation,
            configDirectory = configDirectory,
            languages = languageResolver,
            documentTheme = { DocumentTheme.from(presentationTheme()) },
        )
        val utility = createUtilityFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            settings = { config.current.utility },
            known = known,
            messages = messages,
            adventure = adventure,
            presentation = presentation,
            audit = AdministrativeActionAuditPort { actorId, targetId, targetName, cleared ->
                administration.recordInventoryClear(actorId, targetId, targetName, cleared)
            },
        )
        val playerFeature = createPlayerFeature(
            kernel = kernel,
            database = database,
            namespace = namespace,
            settings = { config.current.players },
            known = known,
            identities = identityRepository,
            messages = messages,
            adventure = adventure,
            presentation = presentation,
            permissions = permissions,
        )

        val integration = CommunicationIntegration(
            senderGate = {
                when {
                    administration.isMuted(it) -> SendGateResult.Denied(SenderRestriction.MUTED)
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
        )

        val commandRegistry = CommandRegistry(
            buildList {
                add(
                    RootCommand.definition(
                        config = config,
                        kernel = kernel,
                        health = health,
                        messages = messages,
                        adventure = adventure,
                        presentation = presentation,
                        languages = languagePreferences,
                        resolver = languageResolver,
                        catalog = catalog,
                        modVersion = modVersion(),
                        loaderName = platform.loaderName,
                        onReloadApplied = { playerFeature.applyNicknameDisplay() },
                    )
                )
                addAll(movement.commands())
                addAll(communication.commands(integration))
                addAll(administration.commands())
                addAll(utility.commands())
                addAll(playerFeature.commands())
            }
        )
        val helpResolver = CommandHelpResolver(commandRegistry.definitions, permissions)
        val documentation = CommandDocumentationService(
            loader = MarkdownDocumentLoader(
                listOf(
                    ClasspathCommandDocumentSource(movement::class.java),
                    ClasspathCommandDocumentSource(communication::class.java),
                    ClasspathCommandDocumentSource(administration::class.java),
                    ClasspathCommandDocumentSource(utility::class.java),
                    ClasspathCommandDocumentSource(PlayerFeature::class.java),
                    ClasspathCommandDocumentSource(CellulosesZ::class.java),
                )
            ),
            messages = messages,
            languages = languageResolver,
            permissions = permissions,
        )
        val helpOverlay = SelectiveVanillaHelpOverlay(
            helpResolver,
            documentation,
            adventure
        ) {
            DocumentTheme.from(presentationTheme())
        }
        // The `/me` override is a mixin decision taken while vanilla builds its dispatcher, so the
        // snapshot is published here at mod init, well before the server constructs its commands.
        communication.applyVanillaMeOverride(
            config.current.messaging.actionMessage.overrideVanillaMe
        )

        CommandRegistrationEvent.EVENT.register { dispatcher, buildContext, _ ->
            commandRegistry.register(dispatcher, buildContext, permissions)
            helpOverlay.install(dispatcher)
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

            movement.onServerStarting()
            administration.onServerStarting()
            communication.onServerStarting()
            utility.onServerStarting()
            playerFeature.onServerStarting()
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
            playerFeature.onServerStopping()
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
                administration.onPlayerTick(player)
                playerFeature.onPlayerTick(player)
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
            playerFeature.onPlayerJoined(player)
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
                presentation.renderFor(
                    player.uuid,
                    administration.mutedChatNotice()
                ).forEach {
                    adventure.audience(
                        CommandFeedbackTarget.Player(player.uuid)
                    ).sendMessage(it)
                }
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }

        PlayerEvent.PLAYER_QUIT.register { player ->
            administration.onPlayerQuit(player.uuid)
            communication.onPlayerQuit(player.uuid)
            utility.onPlayerQuit(player.uuid)
            playerFeature.onPlayerQuit(player)
            val affected = movement.onPlayerQuit(player.uuid)
            if (kernel.state == KernelState.RUNNING) {
                affected.forEach { senderId ->
                    presentation.renderFor(senderId, movement.requestTargetOfflineNotice())
                            .forEach {
                                adventure.audience(CommandFeedbackTarget.Player(senderId))
                                        .sendMessage(it)
                            }
                }
            }
        }
    }

    private fun configDirectory(): Path =
        Platform.getConfigFolder()
                .resolve("cellulosesz")

    private fun modVersion(): String =
        runCatching { Platform.getMod("cellulosesz").version }
                .getOrDefault("unknown")

    private fun dataRoot(kernel: RuntimeKernel): Path =
        kernel.requireServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("cellulosesz")

}

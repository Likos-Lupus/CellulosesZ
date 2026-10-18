package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.api.ApiVersion
import top.likoslupus.cellulosesz.api.PlayerIdentity
import top.likoslupus.cellulosesz.api.extension.ExtensionsApi
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.application.api.*
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.movement.integration.MovementDomainObserver
import top.likoslupus.cellulosesz.movement.integration.MovementHomeChangeKind
import top.likoslupus.cellulosesz.movement.integration.MovementRequestState
import top.likoslupus.cellulosesz.movement.integration.MovementWarpChangeKind
import top.likoslupus.cellulosesz.player.integration.PlayerAfkCause
import top.likoslupus.cellulosesz.player.integration.PlayerDomainObserver
import java.time.Instant
import java.util.*

/**
 * Bootstrap phase for the public API: provider publication, domain-event forwarding, and the
 * per-session adapter graph. The provider is published before any feature is constructed, and a
 * fresh session is opened in `SERVER_STARTING` so an old view can never be revived.
 */
internal object BootstrapApi {

    fun publishProvider(state: BootstrapState) {
        state.apiDispatcher = ApiRequestDispatcher(state.kernel)
        state.extensions = ApplicationExtensionHub(
            ApiAuthorization { state.config.current.api },
            state.addonText
        )
        state.apiProvider = ApplicationApiProvider(state.extensions)
        state.apiProvider.openRegistrationWindow()
        state.platform.publishApi(state.apiProvider)
        state.extensionsApi = object : ExtensionsApi {
            override fun registeredAddons(): List<String> =
                state.extensions.registeredAddonIds()
        }
    }

    /**
     * Domain facts are forwarded to the current session's public listeners (if any). The holder is
     * swapped per session, so an old session's listeners never receive a new session's events.
     */
    fun forwardEvents(graph: BootstrapState) {
        graph.playerFeature.domainObserver = object : PlayerDomainObserver {
            override fun onAfkChanged(
                playerId: UUID,
                afk: Boolean,
                cause: PlayerAfkCause
            ) {
                graph.sessionEvents.get()?.player?.dispatchAfkChanged(
                    playerId,
                    afk,
                    cause
                )
            }

            override fun onNicknameChanged(
                playerId: UUID,
                previous: String?,
                current: String?
            ) {
                graph.sessionEvents.get()?.player?.dispatchNicknameChanged(
                    playerId,
                    previous,
                    current
                )
            }

            override fun onPresenceChanged(
                playerId: UUID,
                online: Boolean
            ) {
                graph.sessionEvents.get()?.player?.dispatchPresenceChanged(playerId, online)
            }
        }
        graph.movement.domainObserver = object : MovementDomainObserver {
            override fun onHomeChanged(
                ownerId: UUID,
                name: String,
                kind: MovementHomeChangeKind
            ) {
                graph.sessionEvents.get()?.movement?.dispatchHomeChanged(
                    HomeChanged(
                        ownerId,
                        name,
                        kind.toApiKind(),
                        Instant.now()
                    )
                )
            }

            override fun onWarpChanged(
                name: String,
                kind: MovementWarpChangeKind
            ) {
                graph.sessionEvents.get()?.movement?.dispatchWarpChanged(
                    WarpChanged(
                        name,
                        kind.toApiKind(),
                        Instant.now()
                    )
                )
            }

            override fun onSpawnChanged(configured: Boolean) {
                graph.sessionEvents.get()?.movement?.dispatchSpawnChanged(
                    SpawnChanged(
                        configured,
                        Instant.now()
                    )
                )
            }

            override fun onTeleportRequestChanged(
                senderId: UUID,
                targetId: UUID,
                state: MovementRequestState,
            ) {
                graph.sessionEvents.get()?.movement?.dispatchTeleportRequestChanged(
                    TeleportRequestChanged(
                        senderId,
                        targetId,
                        state.toApiState()
                    )
                )
            }
        }
        graph.administration.integration.observe { event ->
            graph.sessionEvents.get()?.moderation?.dispatch(event)
        }
    }

    fun openSession(state: BootstrapState) {
        val session = state.apiProvider.beginSession()
        lateinit var playerApi: PlayerApiAdapter
        val preferencesApi = PlayerPreferencesApiAdapter(
            preferences = state.preferences,
            known = state.known,
            hub = state.extensions,
            dispatcher = state.apiDispatcher,
            dispatch = { playerApi.dispatchPreferenceChanged(it) },
        )
        playerApi = PlayerApiAdapter(
            state.playerFeature.integration,
            state.apiDispatcher,
            preferencesApi
        )
        val worldsApi = WorldsApiAdapter(
            kernel = state.kernel,
            integration = state.movement.integration,
            permissions = state.permissions,
            settings = { state.config.current.movement },
            hub = state.extensions,
            dispatcher = state.apiDispatcher,
        )
        val movementApi = MovementApiAdapter(
            state.movement.integration,
            state.apiDispatcher,
            worldsApi
        )
        val communicationApi = CommunicationApiAdapter(
            state.communication.integration,
            state.apiDispatcher
        )
        val kitApi = KitApiAdapter(state.utility.integration, state.apiDispatcher)
        val moderationApi = ModerationApiAdapter(
            state.administration.integration,
            state.apiDispatcher
        )
        val presentationApi = PresentationApiAdapter(
            lease = session.lease,
            languages = state.languageResolver,
            presentation = state.presentation,
            adventure = state.adventure,
            kernel = state.kernel,
            dispatcher = state.apiDispatcher,
            isOnline = state.playerFeature.integration::online,
            registerHelp = state.extensions::registerHelpDocument,
            resolveAddonText = { key, language ->
                state.addonText.resolve(key, language)
            },
        )
        val kitCatalog = state.utility.integration.observeCatalogChanges { event ->
            kitApi.dispatchCatalogChanged(event)
        }
        lateinit var economyApi: EconomyApiAdapter
        val worthApi = EconomyWorthApiAdapter(
            integration = state.economy.integration,
            hub = state.extensions,
            dispatcher = state.apiDispatcher,
            dispatch = { economyApi.dispatchWorthChanged(it) },
        )
        economyApi = EconomyApiAdapter(
            lease = session.lease,
            integration = state.economy.integration,
            dispatcher = state.apiDispatcher,
            hub = state.extensions,
            settings = { state.config.current.economy },
            worth = worthApi,
        )
        state.sessionEvents.set(
            ApiSessionEvents(
                player = playerApi,
                movement = movementApi,
                communication = communicationApi,
                kit = kitApi,
                moderation = moderationApi,
                economy = economyApi,
                kitCatalog = { kitCatalog.close() },
            )
        )
        state.apiProvider.attach(
            ApplicationApi(
                version = ApiVersion(0, 1),
                players = ApiPlayerLookup(session.lease) { id ->
                    state.known.knownById(id)?.let {
                        PlayerIdentity(it.id, it.name)
                    }
                },
                visibility = ApiVisibilityLookup(session.lease) { viewerId, targetId ->
                    state.administration.canBeSeenBy(viewerId, targetId)
                },
                teleports = session.teleports,
                player = playerApi,
                movement = movementApi,
                communication = communicationApi,
                kits = kitApi,
                moderation = moderationApi,
                economy = economyApi,
                presentation = presentationApi,
                extensions = state.extensionsApi,
            )
        )
    }

}

/**
 * The per-session public API adapters. A fresh instance is built in [BootstrapApi.openSession] and
 * dropped in `SERVER_STOPPING`, so no listener or subscription from one session survives into the
 * next.
 */
internal class ApiSessionEvents(
    val player: PlayerApiAdapter,
    val movement: MovementApiAdapter,
    val communication: CommunicationApiAdapter,
    val kit: KitApiAdapter,
    val moderation: ModerationApiAdapter,
    val economy: EconomyApiAdapter,
    private val kitCatalog: () -> Unit,
) {

    fun clear() {
        player.clear()
        movement.clear()
        communication.eventSink().clear()
        kit.close()
        moderation.clear()
        economy.events.clear()
        runCatching { kitCatalog() }
    }

}

private fun MovementHomeChangeKind.toApiKind(): HomeChangeKind =
    when (this) {
        MovementHomeChangeKind.CREATED -> HomeChangeKind.CREATED
        MovementHomeChangeKind.UPDATED -> HomeChangeKind.UPDATED
        MovementHomeChangeKind.RENAMED -> HomeChangeKind.RENAMED
        MovementHomeChangeKind.DELETED -> HomeChangeKind.DELETED
    }

private fun MovementWarpChangeKind.toApiKind(): WarpChangeKind =
    when (this) {
        MovementWarpChangeKind.CREATED -> WarpChangeKind.CREATED
        MovementWarpChangeKind.UPDATED -> WarpChangeKind.UPDATED
        MovementWarpChangeKind.DELETED -> WarpChangeKind.DELETED
    }

private fun MovementRequestState.toApiState(): TeleportRequestState =
    when (this) {
        MovementRequestState.PENDING -> TeleportRequestState.PENDING
        MovementRequestState.ACCEPTED -> TeleportRequestState.ACCEPTED
        MovementRequestState.DENIED -> TeleportRequestState.DENIED
        MovementRequestState.CANCELLED -> TeleportRequestState.CANCELLED
        MovementRequestState.EXPIRED -> TeleportRequestState.EXPIRED
    }

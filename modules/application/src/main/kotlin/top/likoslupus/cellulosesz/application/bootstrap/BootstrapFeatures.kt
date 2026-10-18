package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.administration.createAdministrationFeature
import top.likoslupus.cellulosesz.api.communication.PrivateMessageDelivered
import top.likoslupus.cellulosesz.api.economy.EconomyAccountCreated
import top.likoslupus.cellulosesz.api.economy.EconomyBalanceChanged
import top.likoslupus.cellulosesz.api.economy.EconomyTransactionCommitted
import top.likoslupus.cellulosesz.api.economy.EconomyTransactionKind
import top.likoslupus.cellulosesz.application.api.extension.ComposedKitEligibility
import top.likoslupus.cellulosesz.application.api.extension.ComposedTeleportPreflight
import top.likoslupus.cellulosesz.application.api.toApiEvent
import top.likoslupus.cellulosesz.application.port.MovementJailTransferPort
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.SenderRestriction
import top.likoslupus.cellulosesz.communication.createCommunicationFeature
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.model.DetailField
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.economy.createEconomyFeature
import top.likoslupus.cellulosesz.economy.integration.EconomyBalanceResult
import top.likoslupus.cellulosesz.economy.integration.EconomyCommittedEvent
import top.likoslupus.cellulosesz.economy.integration.EconomyDomainObserver
import top.likoslupus.cellulosesz.economy.payment.PaymentEligibility
import top.likoslupus.cellulosesz.movement.createMovementFeature
import top.likoslupus.cellulosesz.player.createPlayerFeature
import top.likoslupus.cellulosesz.utility.createUtilityFeature
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.*

/**
 * Bootstrap phase three: construct every bounded-context facade and the cross-feature ports that
 * only the composition root may know about. Administration exists before movement (jail confinement)
 * and the public-API provider is published before any feature is built, exactly as before.
 */
internal object BootstrapFeatures {

    fun install(state: BootstrapState) {
        installAdministration(state)
        BootstrapApi.publishProvider(state)
        installMovement(state)
        installCommunication(state)
        installUtility(state)
        installPlayer(state)
        installEconomy(state)
        exposeCrossFeaturePorts(state)
    }

    private fun installAdministration(state: BootstrapState) {
        val jailTransferPort = MovementJailTransferPort { state.movement }
        state.administration = createAdministrationFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            permissions = state.permissions,
            settings = { state.config.current.administration },
            known = state.known,
            jailTransfer = { jailTransferPort },
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
        )
    }

    private fun installMovement(state: BootstrapState) {
        state.movement = createMovementFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            settings = { state.config.current.movement },
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
            preferences = state.preferences,
            known = state.known,
            confinement = { state.administration.isConfined(it) },
            requestReachability = { senderId, targetId ->
                state.administration.canBeSeenBy(senderId, targetId)
            },
            teleportPreflight = ComposedTeleportPreflight(state.extensions),
            teleportObserver = { committed ->
                val event = committed.toApiEvent()
                state.apiProvider.dispatchTeleport(event)
                state.sessionEvents.get()?.movement?.dispatchTeleportCommitted(event)
            },
            permissions = state.permissions,
        )
    }

    private fun installCommunication(state: BootstrapState) {
        state.communication = createCommunicationFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            permissions = state.permissions,
            settings = { state.config.current.messaging },
            known = state.known,
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
            configDirectory = state.configDirectory,
            languages = state.languageResolver,
            documentTheme = { DocumentTheme.from(state.presentationTheme()) },
            playerPreferences = state.preferences,
            groupResolver = { playerId ->
                val player = state.kernel.requireServer().playerList.getPlayer(playerId)
                if (player == null) {
                    null
                } else {
                    val source = player.createCommandSourceStack()
                    state.config.current.players.list.groups.firstOrNull { group ->
                        group.permission.isBlank() ||
                                state.permissions.allows(
                                    source,
                                    PermissionSpec(
                                        group.permission,
                                        VanillaPermissionFallback.DENY_ALL,
                                        "list group ${group.id}",
                                    ),
                                )
                    }?.id
                }
            },
            onlineCount = { state.kernel.requireServer().playerList.playerCount },
            uniqueCount = { state.known.knownCount },
            uptime = {
                val totalSeconds = ManagementFactory.getRuntimeMXBean().uptime / 1000
                val days = totalSeconds / 86_400
                val hours = totalSeconds % 86_400 / 3_600
                val minutes = totalSeconds % 3_600 / 60
                when {
                    days > 0 -> "${days}d ${hours}h ${minutes}m"
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            },
            silentJoinVanish = { state.administration.forceVanish(it) },
            hiddenJoinQuit = { state.administration.shouldHideJoinQuit(it) },
        )
    }

    private fun installUtility(state: BootstrapState) {
        state.utility = createUtilityFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            settings = { state.config.current.utility },
            known = state.known,
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
            preferences = state.preferences,
            permissions = state.permissions,
            audit = { actorId, targetId, targetName, cleared ->
                state.administration.recordInventoryClear(
                    actorId,
                    targetId,
                    targetName,
                    cleared
                )
            },
            kitEligibility = ComposedKitEligibility(state.extensions),
        )
    }

    private fun installPlayer(state: BootstrapState) {
        state.playerFeature = createPlayerFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            settings = { state.config.current.players },
            known = state.known,
            identities = state.identityRepository,
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
            permissions = state.permissions,
        )
    }

    private fun installEconomy(state: BootstrapState) {
        state.economy = createEconomyFeature(
            kernel = state.kernel,
            database = state.database,
            namespace = state.namespace,
            settings = { state.config.current.economy },
            resolver = state.known,
            messages = state.messages,
            adventure = state.adventure,
            presentation = state.presentation,
            preferences = state.preferences,
            permissions = state.permissions,
            // The ignore list is owned by communication; this adapter only projects the query, so
            // economy never depends on the communication module.
            eligibility = PaymentEligibility { payerId, receiverId ->
                !state.communication.integration.ignoresPlayer(
                    receiverId,
                    payerId
                )
            },
            observer = object : EconomyDomainObserver {
                override fun onTransactionCommitted(event: EconomyCommittedEvent) {
                    val sessionEconomy = state.sessionEvents.get()?.economy ?: return
                    sessionEconomy.events.dispatchCommitted(
                        EconomyTransactionCommitted(
                            transactionId = event.transactionId,
                            kind = EconomyTransactionKind.valueOf(event.kind.name),
                            currencyId = event.currencyId,
                            occurredAt = event.occurredAt,
                            affectedPlayerIds = event.changes.map { it.playerId },
                        )
                    )
                    event.changes.forEach { change ->
                        sessionEconomy.events.dispatchBalanceChanged(
                            EconomyBalanceChanged(
                                change.playerId,
                                event.transactionId,
                                event.occurredAt
                            )
                        )
                    }
                }

                override fun onAccountOpened(playerId: UUID, at: Instant) {
                    state.sessionEvents.get()?.economy?.events?.dispatchAccountCreated(
                        EconomyAccountCreated(playerId, at)
                    )
                }
            },
        )
    }

    /**
     * Visibility is answered by administration, so `/list` and the public API never assume the
     * player module knows about vanish.
     */
    private fun exposeCrossFeaturePorts(state: BootstrapState) {
        state.playerFeature.vanishVisibility = { viewerId, targetId ->
            state.administration.canBeSeenBy(viewerId, targetId)
        }
        state.playerFeature.registerWhoisContributor { playerId ->
            val (muted, jailed) = state.kernel.onServerThread {
                state.administration.isMuted(playerId) to state.administration.isConfined(playerId)
            }
            buildList {
                if (muted) {
                    add(
                        DetailField(
                            LocalizedText(MessageKey("player.whois.field.muted")),
                            LocalizedText(MessageKey("player.whois.value.yes")),
                        )
                    )
                }
                if (jailed) {
                    add(
                        DetailField(
                            LocalizedText(MessageKey("player.whois.field.jailed")),
                            LocalizedText(MessageKey("player.whois.value.yes")),
                        )
                    )
                }
                when (val balance = state.economy.integration.balance(playerId)) {
                    is EconomyBalanceResult.Available -> add(
                        DetailField(
                            LocalizedText(MessageKey("player.whois.field.balance")),
                            LocalizedText(
                                MessageKey("player.whois.value.money"),
                                listOf(TextArgument.Literal(balance.snapshot.amount.toPlainString())),
                            ),
                        )
                    )

                    else -> Unit
                }
            }
        }
    }

    fun installIntegration(state: BootstrapState) {
        state.integration = CommunicationIntegration(
            senderGate = {
                when {
                    state.administration.isMuted(it) ->
                        SendGateResult.Denied(SenderRestriction.MUTED)

                    else -> SendGateResult.Allowed
                }
            },
            targetReachability = { senderId, targetId ->
                state.administration.canBeSeenBy(senderId, targetId)
            },
            observer = { message ->
                state.administration.observePrivateMessage(
                    senderId = message.senderId,
                    senderName = message.senderName,
                    targetId = message.targetId,
                    targetName = message.targetName,
                    text = message.text,
                )
                state.sessionEvents.get()?.communication?.eventSink()
                        ?.dispatchPrivateMessageDelivered(
                            PrivateMessageDelivered(
                                message.senderId,
                                message.targetId
                            )
                        )
            },
        )
    }

}

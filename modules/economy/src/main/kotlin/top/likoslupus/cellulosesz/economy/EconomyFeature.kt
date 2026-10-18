package top.likoslupus.cellulosesz.economy

import com.mojang.serialization.JsonOps
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.VanillaFallbackPermissionBridge
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.economy.command.EconomyCommands
import top.likoslupus.cellulosesz.economy.command.WorthSellCommands
import top.likoslupus.cellulosesz.economy.command.WorthSellContext
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.integration.DefaultEconomyIntegration
import top.likoslupus.cellulosesz.economy.integration.EconomyDomainObserver
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.payment.PayConfirmationState
import top.likoslupus.cellulosesz.economy.payment.PaymentEligibility
import top.likoslupus.cellulosesz.economy.sale.JdbcSaleIntentRepository
import top.likoslupus.cellulosesz.economy.sale.SaleInventory
import top.likoslupus.cellulosesz.economy.sale.SaleItemCodec
import top.likoslupus.cellulosesz.economy.sale.SaleRecoveryService
import top.likoslupus.cellulosesz.economy.service.EconomyService
import top.likoslupus.cellulosesz.economy.worth.JdbcWorthRepository
import top.likoslupus.cellulosesz.economy.worth.WorthCache
import top.likoslupus.cellulosesz.economy.worth.WorthService
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.time.Instant
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The economy feature facade: commands plus the single narrow integration surface. */
public class EconomyFeature internal constructor(
    public val integration: EconomyIntegrationPort,
    private val kernel: RuntimeKernel,
    private val settings: () -> EconomySettings,
    private val resolver: KnownPlayerResolver,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
    private val permissions: PermissionService,
    private val preferences: PlayerPreferenceService,
    private val eligibility: PaymentEligibility,
    private val confirmations: PayConfirmationState,
    private val worth: WorthService,
    private val recovery: SaleRecoveryService,
    private val worthSell: WorthSellContext,
) {

    public fun commands(): List<CommandDefinition> =
        EconomyCommands.commands(
            integration,
            settings,
            resolver,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
            preferences,
            eligibility,
            confirmations,
        ) + WorthSellCommands.commands(worthSell)

    /**
     * Hydrates the worth cache and recovers durable sale intents. Both run off the server thread;
     * the cache is read synchronously by `/worth` and `/sell` once available.
     */
    public fun onServerStarting() {
        kernel.launch {
            runCatching { worth.hydrate() }
            runCatching { recovery.recover() }
        }
    }

    public fun onServerStopping(): Unit =
        confirmations.clearAll()

    /** Drops a leaving player's session-only payment confirmation. Server thread. */
    public fun onPlayerQuit(playerId: UUID) {
        confirmations.clear(playerId)
    }

}

/**
 * Builds the economy feature from its collaborators. The composition root owns configuration, the
 * database runtime, the namespace, the domain observer, the permission bridge and the payment
 * eligibility policy; this factory only wires the ledger, the worth catalog and the sale pipeline.
 */
public fun createEconomyFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    settings: () -> EconomySettings,
    resolver: KnownPlayerResolver,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    preferences: PlayerPreferenceService,
    observer: EconomyDomainObserver,
    permissions: PermissionService = PermissionService(VanillaFallbackPermissionBridge),
    eligibility: PaymentEligibility = PaymentEligibility.AllowAll,
    now: () -> Instant = { Instant.now() },
): EconomyFeature {
    val repository = JdbcEconomyRepository(database, namespace)
    val service = EconomyService(repository, settings, now)

    val worthRepository = JdbcWorthRepository(database, namespace)
    val worth = WorthService(
        repository = worthRepository,
        cache = WorthCache(),
        seedEnabled = { settings().worth.seedBundledDefaults },
        now = now,
        seedText = { readBundledWorthSeed() },
    )
    val integration = DefaultEconomyIntegration(service, observer, worth)

    val saleIntents = JdbcSaleIntentRepository(database, namespace)
    val codec = SaleItemCodec {
        kernel.requireServer()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE)
    }
    val inventory = SaleInventory(kernel, codec)
    val recovery = SaleRecoveryService(
        kernel = kernel,
        repository = saleIntents,
        ledger = repository,
        inventory = inventory,
        now = now,
    )
    val worthSell = WorthSellContext(
        integration = integration,
        settings = settings,
        kernel = kernel,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
        permissions = permissions,
        worth = worth,
        intents = saleIntents,
        inventory = inventory,
        codec = codec,
        recovery = recovery,
        now = now,
    )

    return EconomyFeature(
        integration,
        kernel,
        settings,
        resolver,
        messages,
        adventure,
        presentation,
        permissions,
        preferences,
        eligibility,
        PayConfirmationState(),
        worth,
        recovery,
        worthSell,
    )
}

/** The bundled worth seed. Absent or unreadable resources simply skip the import. */
private const val WORTH_SEED_RESOURCE: String = "/cellulosesz/worth/default.tsv"

private suspend fun readBundledWorthSeed(): String? =
    withContext(Dispatchers.IO) {
        EconomyFeature::class.java
                .getResourceAsStream(WORTH_SEED_RESOURCE)
                ?.use { it.readBytes().decodeToString() }
    }

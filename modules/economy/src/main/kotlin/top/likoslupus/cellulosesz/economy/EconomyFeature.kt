package top.likoslupus.cellulosesz.economy

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.economy.command.EconomyCommands
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.integration.DefaultEconomyIntegration
import top.likoslupus.cellulosesz.economy.integration.EconomyDomainObserver
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.service.EconomyService
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.time.Instant

/** The economy feature facade: commands plus the single narrow integration surface. */
public class EconomyFeature internal constructor(
    public val integration: EconomyIntegrationPort,
    private val kernel: RuntimeKernel,
    private val settings: () -> EconomySettings,
    private val resolver: KnownPlayerResolver,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) {

    public fun commands(): List<CommandDefinition> =
        EconomyCommands.commands(
            integration,
            settings,
            resolver,
            kernel,
            messages,
            adventure,
            presentation
        )

    public fun onServerStarting(): Unit =
        Unit

    public fun onServerStopping(): Unit =
        Unit

}

/**
 * Builds the economy feature from its collaborators. The composition root owns configuration, the
 * database runtime, the namespace and the domain observer; this factory only wires the ledger.
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
    observer: EconomyDomainObserver,
    now: () -> Instant = { Instant.now() },
): EconomyFeature {
    val repository = JdbcEconomyRepository(database, namespace)
    val service = EconomyService(repository, settings, now)
    val integration = DefaultEconomyIntegration(service, observer)
    return EconomyFeature(
        integration,
        kernel,
        settings,
        resolver,
        messages,
        adventure,
        presentation
    )
}

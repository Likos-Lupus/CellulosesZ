package top.likoslupus.cellulosesz.utility

import com.mojang.serialization.JsonOps
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.serverThreadRunner
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.UtilitySettings
import top.likoslupus.cellulosesz.utility.inspection.InspectionCommands
import top.likoslupus.cellulosesz.utility.inspection.InspectionService
import top.likoslupus.cellulosesz.utility.inspection.MinecraftInspectionBackend
import top.likoslupus.cellulosesz.utility.integration.KitIntegrationPort
import top.likoslupus.cellulosesz.utility.integration.UtilityKitIntegration
import top.likoslupus.cellulosesz.utility.item.AdministrativeActionAuditPort
import top.likoslupus.cellulosesz.utility.item.ItemUtilityCommands
import top.likoslupus.cellulosesz.utility.item.ItemUtilityService
import top.likoslupus.cellulosesz.utility.item.MinecraftItemUtilityBackend
import top.likoslupus.cellulosesz.utility.kit.*
import top.likoslupus.cellulosesz.utility.position.PositionCommands
import top.likoslupus.cellulosesz.utility.workstation.MinecraftWorkstationBackend
import top.likoslupus.cellulosesz.utility.workstation.WorkstationCommands
import top.likoslupus.cellulosesz.utility.workstation.WorkstationService
import java.time.Clock
import java.util.*

/**
 * Public surface of the utility bounded context. Internals stay `internal`.
 */
public class UtilityFeature internal constructor(
    private val kits: KitService,
    private val kitGrants: KitGrantService,
    private val items: ItemUtilityService,
    private val workstations: WorkstationService,
    private val inspection: InspectionService,
    private val known: KnownPlayerResolver,
    private val kernel: RuntimeKernel,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
    private val audit: AdministrativeActionAuditPort,
) {

    /** Narrow, Minecraft-free integration surface for the public kit facade. */
    public val integration: KitIntegrationPort = UtilityKitIntegration(kits, kitGrants)

    public fun commands(): List<CommandDefinition> =
        KitCommands.commands(
            kits,
            known,
            kernel,
            messages,
            adventure,
            presentation
        ) + ItemUtilityCommands.commands(
            items,
            known,
            messages,
            adventure,
            presentation,
            audit,
            kernel,
        ) + WorkstationCommands.commands(
            workstations,
            messages,
            adventure,
            presentation
        ) + InspectionCommands.commands(
            inspection,
            known,
            messages,
            adventure,
            presentation
        ) + PositionCommands.commands(
            adventure,
            presentation
        )

    public fun onServerStarting() {
        kits.beginLoad()
        kernel.launch { kits.load() }
    }

    public fun onServerStopping() {
        kits.shutdown()
        items.clearConfirmations()
    }

    /** Drops any pending `/clearinventory` confirmation for a leaving player. */
    public fun onPlayerQuit(playerId: UUID) {
        items.forgetInventoryClear(playerId)
    }

}

public fun createUtilityFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    settings: () -> UtilitySettings,
    known: KnownPlayerResolver,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    audit: AdministrativeActionAuditPort = AdministrativeActionAuditPort { _, _, _, _ -> },
    kitEligibility: KitClaimEligibility = KitClaimEligibility.AllowAll,
): UtilityFeature {
    val runner = kernel.serverThreadRunner()
    val clock = Clock.systemUTC()
    val codec = KitItemCodec {
        kernel.requireServer()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE)
    }

    val kitInventory = MinecraftKitInventoryBackend(kernel)
    val kits = KitService(
        runner = runner,
        definitions = JdbcKitRepository(database, namespace, codec),
        claims = JdbcKitClaimRepository(database, namespace),
        inventory = kitInventory,
        known = known,
        claimLocks = KeyedMutex(),
        settings = { settings().kits },
        clock = clock,
        eligibility = kitEligibility,
    )
    val kitGrants = KitGrantService(
        runner = runner,
        kits = kits,
        grants = JdbcKitGrantRepository(database, namespace),
        inventory = kitInventory,
        known = known,
        settings = { settings().kits },
        clock = clock,
    )
    val items = ItemUtilityService(
        backend = MinecraftItemUtilityBackend(kernel),
        known = known,
        settings = { settings().items },
        editing = { settings().itemEditing },
    )
    val workstations = WorkstationService(
        backend = MinecraftWorkstationBackend(kernel),
        settings = { settings().workstations },
    )
    val inspection = InspectionService(
        backend = MinecraftInspectionBackend(kernel),
    )

    return UtilityFeature(
        kits = kits,
        kitGrants = kitGrants,
        items = items,
        workstations = workstations,
        inspection = inspection,
        known = known,
        kernel = kernel,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
        audit = audit,
    )
}

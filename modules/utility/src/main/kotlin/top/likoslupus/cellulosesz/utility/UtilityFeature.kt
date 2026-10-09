package top.likoslupus.cellulosesz.utility

import com.mojang.brigadier.CommandDispatcher
import com.mojang.serialization.JsonOps
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.serverThreadRunner
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.UtilitySettings
import top.likoslupus.cellulosesz.utility.inspection.InspectionCommands
import top.likoslupus.cellulosesz.utility.inspection.InspectionService
import top.likoslupus.cellulosesz.utility.inspection.MinecraftInspectionBackend
import top.likoslupus.cellulosesz.utility.item.ItemUtilityCommands
import top.likoslupus.cellulosesz.utility.item.ItemUtilityService
import top.likoslupus.cellulosesz.utility.item.MinecraftItemUtilityBackend
import top.likoslupus.cellulosesz.utility.kit.*
import top.likoslupus.cellulosesz.utility.workstation.MinecraftWorkstationBackend
import top.likoslupus.cellulosesz.utility.workstation.WorkstationCommands
import top.likoslupus.cellulosesz.utility.workstation.WorkstationService
import java.time.Clock

/**
 * Public surface of the utility bounded context. Internals stay `internal`.
 */
public class UtilityFeature internal constructor(
    private val kits: KitService,
    private val items: ItemUtilityService,
    private val workstations: WorkstationService,
    private val inspection: InspectionService,
    private val known: KnownPlayerResolver,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
) {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        KitCommands.register(dispatcher, kits, known, kernel, permissions)
        ItemUtilityCommands.register(dispatcher, items, known, permissions)
        WorkstationCommands.register(dispatcher, workstations)
        InspectionCommands.register(dispatcher, inspection, known, permissions)
    }

    public fun onServerStarting() {
        kits.beginLoad()
        kernel.launch { kits.load() }
    }

    public fun onServerStopping() {
        kits.shutdown()
    }

}

public fun createUtilityFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    permissions: PermissionService,
    settings: () -> UtilitySettings,
    known: KnownPlayerResolver,
): UtilityFeature {
    val runner = kernel.serverThreadRunner()
    val codec = KitItemCodec {
        kernel.requireServer()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE)
    }

    val kits = KitService(
        runner = runner,
        definitions = JdbcKitRepository(database, namespace, codec),
        claims = JdbcKitClaimRepository(database, namespace),
        inventory = MinecraftKitInventoryBackend(kernel),
        known = known,
        claimLocks = KeyedMutex(),
        settings = { settings().kits },
        clock = Clock.systemUTC(),
    )
    val items = ItemUtilityService(
        backend = MinecraftItemUtilityBackend(kernel),
        known = known,
        settings = { settings().items },
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
        items = items,
        workstations = workstations,
        inspection = inspection,
        known = known,
        kernel = kernel,
        permissions = permissions,
    )
}

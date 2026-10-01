package top.likoslupus.cellulosesz.utility

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.utility.item.ItemCommands
import top.likoslupus.cellulosesz.utility.kit.FileKitRepository
import top.likoslupus.cellulosesz.utility.kit.KitCommands
import top.likoslupus.cellulosesz.utility.kit.KitService
import java.nio.file.Path

/**
 * Public surface of the utility bounded context. Internals stay `internal`.
 */
public class UtilityFeature internal constructor(
    private val kitService: KitService,
    private val kernel: RuntimeKernel,
) {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        KitCommands.register(dispatcher, kitService, kernel)
        ItemCommands.register(dispatcher)
    }

}

public fun createUtilityFeature(kernel: RuntimeKernel, dataRoot: () -> Path): UtilityFeature =
    UtilityFeature(KitService(FileKitRepository(dataRoot), kernel), kernel)

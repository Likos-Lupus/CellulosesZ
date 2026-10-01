package top.likoslupus.cellulosesz.utility.kit

import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.UUID

internal sealed interface GiveKitResult {

    data object Success : GiveKitResult

    data object NotFound : GiveKitResult

    data object PlayerOffline : GiveKitResult

}

internal sealed interface CreateKitResult {

    data object Success : CreateKitResult

    data object InvalidName : CreateKitResult

    data object Empty : CreateKitResult

    data object PlayerOffline : CreateKitResult

}

internal sealed interface DeleteKitResult {

    data object Deleted : DeleteKitResult

    data object InvalidName : DeleteKitResult

    data object NotFound : DeleteKitResult

}

internal class KitService(
    private val repository: KitRepository,
    private val kernel: RuntimeKernel,
) {

    suspend fun give(playerId: UUID, rawName: String): GiveKitResult {
        val name = KitName.parse(rawName) ?: return GiveKitResult.NotFound
        val items = repository.load(name) ?: return GiveKitResult.NotFound

        return kernel.onServerThread {
            val player = PlayerResolver.onlineById(kernel.requireServer(), playerId)
                ?: return@onServerThread GiveKitResult.PlayerOffline
            items.forEach { player.addItem(it.copy()) }
            GiveKitResult.Success
        }
    }

    suspend fun create(playerId: UUID, rawName: String): CreateKitResult {
        val name = KitName.parse(rawName) ?: return CreateKitResult.InvalidName
        val items = kernel.onServerThread {
            val player = PlayerResolver.onlineById(kernel.requireServer(), playerId)
                ?: return@onServerThread null
            val inventory = player.inventory
            (0 until inventory.containerSize).mapNotNull { slot ->
                inventory.getItem(slot).takeUnless { it.isEmpty }?.copy()
            }
        } ?: return CreateKitResult.PlayerOffline

        if (items.isEmpty()) {
            return CreateKitResult.Empty
        }

        repository.save(name, items)
        return CreateKitResult.Success
    }

    suspend fun delete(rawName: String): DeleteKitResult {
        val name = KitName.parse(rawName)
            ?: return DeleteKitResult.InvalidName
        return if (repository.remove(name)) {
            DeleteKitResult.Deleted
        } else {
            DeleteKitResult.NotFound
        }
    }

    suspend fun list(): List<KitName> = repository.names()

}

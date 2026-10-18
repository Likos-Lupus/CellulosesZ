package top.likoslupus.cellulosesz.utility.kit

import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.time.Duration

internal sealed interface ClaimKitResult {

    data class Delivered(
        val kit: KitName,
        val droppedStacks: Int,
        val claimRecordedFully: Boolean,
        val equippedStacks: Int = 0,
    ) : ClaimKitResult

    data object Disabled : ClaimKitResult
    data object CatalogLoading : ClaimKitResult
    data object StorageUnavailable : ClaimKitResult
    data object InvalidName : ClaimKitResult
    data object NotFound : ClaimKitResult
    data object PlayerOffline : ClaimKitResult
    data object InventoryFull : ClaimKitResult
    data class Cooldown(val remaining: Duration) : ClaimKitResult
    data object AlreadyUsed : ClaimKitResult
    data object DeliveryFailedAfterReservation : ClaimKitResult

    data class Denied(val reasonKey: String) : ClaimKitResult

}

internal sealed interface CreateKitResult {

    data class Created(val definition: KitDefinition) : CreateKitResult
    data object Disabled : CreateKitResult
    data object InvalidName : CreateKitResult
    data object AlreadyExists : CreateKitResult
    data object InvalidCooldown : CreateKitResult
    data object CooldownTooLong : CreateKitResult
    data object EmptyInventory : CreateKitResult
    data object TooManyItems : CreateKitResult
    data object KitLimitReached : CreateKitResult
    data object PlayerOffline : CreateKitResult
    data object StorageUnavailable : CreateKitResult

}

internal sealed interface UpdateKitResult {

    data class Updated(val definition: KitDefinition) : UpdateKitResult
    data object Disabled : UpdateKitResult
    data object InvalidName : UpdateKitResult
    data object NotFound : UpdateKitResult
    data object InvalidCooldown : UpdateKitResult
    data object CooldownTooLong : UpdateKitResult
    data object EmptyInventory : UpdateKitResult
    data object TooManyItems : UpdateKitResult
    data object PlayerOffline : UpdateKitResult
    data object StorageUnavailable : UpdateKitResult

}

internal sealed interface DeleteKitResult {

    data class Deleted(val name: KitName) : DeleteKitResult
    data object Disabled : DeleteKitResult
    data object InvalidName : DeleteKitResult
    data object NotFound : DeleteKitResult
    data object StorageUnavailable : DeleteKitResult

}

internal sealed interface KitAvailability {

    data object Available : KitAvailability

    data class Cooldown(val remaining: Duration) : KitAvailability

    data object Used : KitAvailability

}

internal data class KitListEntry(
    val name: KitName,
    val reuse: KitReusePolicy,
    val availability: KitAvailability,
)

internal sealed interface ListKitsResult {

    data class Listed(val entries: List<KitListEntry>) : ListKitsResult
    data object Loading : ListKitsResult
    data object StorageUnavailable : ListKitsResult

}

internal sealed interface ShowKitResult {

    data class Shown(val definition: KitDefinition) : ShowKitResult
    data object Disabled : ShowKitResult
    data object Loading : ShowKitResult
    data object StorageUnavailable : ShowKitResult
    data object InvalidName : ShowKitResult
    data object NotFound : ShowKitResult

}

internal sealed interface ResetKitResult {

    data class Reset(
        val name: KitName,
        val target: KnownPlayerIdentity,
    ) : ResetKitResult

    data object NothingToReset : ResetKitResult
    data object Disabled : ResetKitResult
    data object InvalidName : ResetKitResult
    data object NotFound : ResetKitResult
    data object StorageUnavailable : ResetKitResult

}

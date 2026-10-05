package top.likoslupus.cellulosesz.utility.kit

import net.minecraft.world.item.ItemStack
import java.util.*

internal sealed interface KitCaptureResult {

    data class Captured(val items: List<ItemStack>) : KitCaptureResult

    data object PlayerOffline : KitCaptureResult

}

internal sealed interface KitPreflightResult {

    data object Fits : KitPreflightResult

    data object Full : KitPreflightResult

    data object PlayerOffline : KitPreflightResult

}

internal sealed interface KitDeliveryResult {

    data class Delivered(val droppedStacks: Int) : KitDeliveryResult

    data object PlayerOffline : KitDeliveryResult

    data object Failed : KitDeliveryResult

}

/**
 * All kit `ItemStack` interaction with a live player. Implementations are server-thread confined;
 * the service hops onto the server thread through its runner before calling these methods.
 */
internal interface KitInventoryBackend {

    /** Captures the player's main inventory and hotbar (never armor or offhand). */
    fun capture(playerId: UUID): KitCaptureResult

    fun preflight(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
    ): KitPreflightResult

    fun deliver(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
    ): KitDeliveryResult

}

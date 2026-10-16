package top.likoslupus.cellulosesz.utility.integration

import top.likoslupus.cellulosesz.utility.kit.*
import java.util.*

/**
 * Adapts the utility kit catalog/claim services and the durable grant service to the narrow
 * [KitIntegrationPort]. It only maps internal results into stable Minecraft-free outcomes and
 * forwards committed catalog mutations; it holds no policy of its own.
 */
internal class UtilityKitIntegration(
    private val kits: KitService,
    private val grants: KitGrantService,
) : KitIntegrationPort {

    override suspend fun list(playerId: UUID): KitListResult =
        when {
            !kits.enabled() -> KitListResult.Disabled

            else -> when (val result = kits.list(playerId)) {
                is ListKitsResult.Listed ->
                    KitListResult.Listed(result.entries.map { it.toSummary() })

                ListKitsResult.Loading ->
                    KitListResult.Loading

                ListKitsResult.StorageUnavailable ->
                    KitListResult.StorageUnavailable
            }
        }

    override suspend fun availability(
        playerId: UUID,
        kitName: String,
    ): KitAvailabilityResult {
        if (!kits.enabled()) {
            return KitAvailabilityResult.Disabled
        }
        val name = KitName.parse(kitName)
            ?: return KitAvailabilityResult.NotFound
        return when (val result = kits.list(playerId)) {
            is ListKitsResult.Listed -> result.entries
                    .firstOrNull { it.name == name }
                    ?.let { KitAvailabilityResult.Found(it.availability.toView()) }
                ?: KitAvailabilityResult.NotFound

            ListKitsResult.Loading ->
                KitAvailabilityResult.Loading

            ListKitsResult.StorageUnavailable ->
                KitAvailabilityResult.StorageUnavailable
        }
    }

    override suspend fun claim(playerId: UUID, kitName: String): KitClaimResult =
        kits.claim(playerId, kitName).toResult()

    override suspend fun grant(request: KitGrantRequest): KitGrantResult =
        grants.grant(
            addonId = request.addonId,
            playerId = request.playerId,
            rawKitName = request.kitName,
            grantId = request.grantId,
        ).toResult()

    override fun observeCatalogChanges(observer: KitCatalogObserver): KitCatalogSubscription {
        val registration = kits.observeCatalogChanges { name, change, at ->
            observer.onCatalogChanged(
                KitCatalogChangeEvent(
                    name.value,
                    change.toKind(),
                    at
                )
            )
        }
        return KitCatalogSubscription { registration.close() }
    }

}

private fun KitListEntry.toSummary(): KitSummary =
    KitSummary(
        name = name.value,
        reuse = reuse.toView(),
        availability = availability.toView(),
    )

private fun KitReusePolicy.toView(): KitReuseView = when (this) {
    KitReusePolicy.Always -> KitReuseView.Always
    KitReusePolicy.Once -> KitReuseView.Once
    is KitReusePolicy.Cooldown -> KitReuseView.Cooldown(duration)
}

private fun KitAvailability.toView(): KitAvailabilityView = when (this) {
    KitAvailability.Available -> KitAvailabilityView.Available
    KitAvailability.Used -> KitAvailabilityView.AlreadyUsed
    is KitAvailability.Cooldown -> KitAvailabilityView.Cooldown(remaining)
}

private fun KitCatalogChange.toKind(): KitCatalogChangeKind = when (this) {
    KitCatalogChange.CREATED -> KitCatalogChangeKind.CREATED
    KitCatalogChange.UPDATED -> KitCatalogChangeKind.UPDATED
    KitCatalogChange.DELETED -> KitCatalogChangeKind.DELETED
}

private fun ClaimKitResult.toResult(): KitClaimResult = when (this) {
    is ClaimKitResult.Delivered ->
        KitClaimResult.Delivered(
            kit.value,
            droppedStacks,
            claimRecordedFully
        )

    ClaimKitResult.Disabled ->
        KitClaimResult.Disabled

    ClaimKitResult.CatalogLoading ->
        KitClaimResult.Loading

    ClaimKitResult.StorageUnavailable ->
        KitClaimResult.StorageUnavailable

    ClaimKitResult.InvalidName ->
        KitClaimResult.InvalidName

    ClaimKitResult.NotFound ->
        KitClaimResult.NotFound

    ClaimKitResult.PlayerOffline ->
        KitClaimResult.PlayerOffline

    ClaimKitResult.InventoryFull ->
        KitClaimResult.InventoryFull

    is ClaimKitResult.Cooldown ->
        KitClaimResult.Cooldown(remaining)

    ClaimKitResult.AlreadyUsed ->
        KitClaimResult.AlreadyUsed

    ClaimKitResult.DeliveryFailedAfterReservation ->
        KitClaimResult.DeliveryFailedAfterReservation

    is ClaimKitResult.Denied ->
        KitClaimResult.Denied(reasonKey)
}

private fun GrantKitResult.toResult(): KitGrantResult = when (this) {
    is GrantKitResult.Delivered ->
        KitGrantResult.Delivered(
            grantId,
            kitName.value,
            droppedStacks
        )

    GrantKitResult.AlreadyProcessed ->
        KitGrantResult.AlreadyProcessed

    GrantKitResult.PreviouslyUncertain ->
        KitGrantResult.PreviouslyUncertain

    GrantKitResult.PlayerOffline ->
        KitGrantResult.PlayerOffline

    GrantKitResult.KitNotFound ->
        KitGrantResult.KitNotFound

    GrantKitResult.InventoryFull ->
        KitGrantResult.InventoryFull

    GrantKitResult.DeliveryFailed ->
        KitGrantResult.DeliveryFailed

    GrantKitResult.StorageUnavailable ->
        KitGrantResult.StorageUnavailable

    GrantKitResult.Disabled ->
        KitGrantResult.Disabled

    GrantKitResult.Loading ->
        KitGrantResult.Loading
}

package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiPage
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.economy.*
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.economy.integration.EconomyActorDescriptor
import top.likoslupus.cellulosesz.economy.integration.EconomyActorKind
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import java.time.Instant

/**
 * Adapts the worth catalogue onto the public API. Reads require `economy.worth.read`; writing a
 * unit price is an administrative capability (`economy.worth.write`) and is attributed to the
 * addon's registration handle. A write publishes [EconomyWorthChanged] only when the price actually
 * changed.
 */
internal class EconomyWorthApiAdapter(
    private val integration: EconomyIntegrationPort,
    private val hub: ApplicationExtensionHub,
    private val dispatcher: ApiRequestDispatcher,
    private val dispatch: (EconomyWorthChanged) -> Unit,
) : EconomyWorthApi {

    override fun unitPrice(
        access: AddonAccess,
        itemId: String,
        callback: ApiCallback<WorthLookupOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(access, CAP_READ) ->
                    ApiCallResult.Completed(WorthLookupOutcome.NotAuthorized)

                else -> when (val price = integration.worthUnitPrice(itemId)) {
                    null -> ApiCallResult.Completed(WorthLookupOutcome.NotFound)
                    else -> ApiCallResult.Completed(
                        WorthLookupOutcome.Found(
                            WorthView(
                                itemId,
                                EconomyAmount(price),
                                null
                            )
                        )
                    )
                }
            }
        },
        deliver = callback::onComplete,
    )

    override fun page(
        access: AddonAccess,
        cursor: String?,
        pageSize: Int,
        callback: ApiCallback<WorthPageOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(access, CAP_READ) ->
                    ApiCallResult.Completed(WorthPageOutcome.NotAuthorized)

                else -> {
                    val size = pageSize.coerceIn(1, MAX_PAGE_SIZE)
                    val all = integration.worthCatalog()
                    val start = cursor?.toIntOrNull()?.coerceIn(0, all.size) ?: 0
                    val end = (start + size).coerceAtMost(all.size)
                    val items = all.subList(start, end).map { entry ->
                        WorthView(
                            entry.itemId,
                            EconomyAmount(entry.unitPrice),
                            null
                        )
                    }
                    ApiCallResult.Completed(
                        WorthPageOutcome.Listed(
                            ApiPage(
                                items = items,
                                nextCursor = if (end < all.size) end.toString() else null,
                            )
                        )
                    )
                }
            }
        },
        deliver = callback::onComplete,
    )

    override fun set(
        access: AddonAccess,
        itemId: String,
        unitPrice: EconomyAmount,
        callback: ApiCallback<WorthMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(access, CAP_WRITE) ->
                    ApiCallResult.Completed(WorthMutationOutcome.NotAuthorized)

                itemId.isBlank() ->
                    ApiCallResult.Completed(WorthMutationOutcome.InvalidItem)

                else -> {
                    val changed = integration.setWorth(
                        itemId,
                        unitPrice.decimal,
                        EconomyActorDescriptor(
                            EconomyActorKind.ADDON,
                            access.addonId
                        ),
                    )
                    val worth = WorthView(itemId, unitPrice, Instant.now())
                    if (changed) {
                        dispatch(
                            EconomyWorthChanged(
                                itemId,
                                unitPrice,
                                Instant.now()
                            )
                        )
                        ApiCallResult.Completed(WorthMutationOutcome.Changed(worth))
                    } else {
                        ApiCallResult.Completed(WorthMutationOutcome.Unchanged(worth))
                    }
                }
            }
        },
        deliver = callback::onComplete,
    )

    private companion object {

        private const val CAP_READ = "economy.worth.read"
        private const val CAP_WRITE = "economy.worth.write"
        private const val MAX_PAGE_SIZE = 200

    }

}

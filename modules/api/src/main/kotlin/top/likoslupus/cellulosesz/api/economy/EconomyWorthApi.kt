package top.likoslupus.cellulosesz.api.economy

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiPage
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Instant

/**
 * The configured unit worth of one item. [unitPrice] is the exact per-item sale/grant price, never a
 * floating-point value; [updatedAt] is when it was last set, or `null` for an imported/default row.
 */
public class WorthView(
    public val itemId: String,
    public val unitPrice: EconomyAmount,
    public val updatedAt: Instant?,
)

public sealed interface WorthLookupOutcome {

    public data class Found(public val worth: WorthView) : WorthLookupOutcome

    public data object NotFound : WorthLookupOutcome

    public data object NotAuthorized : WorthLookupOutcome

    public data object Disabled : WorthLookupOutcome

}

public sealed interface WorthPageOutcome {

    public data class Listed(public val page: ApiPage<WorthView>) : WorthPageOutcome

    public data object NotAuthorized : WorthPageOutcome

    public data object Disabled : WorthPageOutcome

}

/** The outcome of a worth mutation. [Unchanged] means the stored price already had this value. */
public sealed interface WorthMutationOutcome {

    public data class Changed(public val worth: WorthView) : WorthMutationOutcome

    public data class Unchanged(public val worth: WorthView) : WorthMutationOutcome

    public data object InvalidItem : WorthMutationOutcome

    public data object InvalidAmount : WorthMutationOutcome

    public data object NotAuthorized : WorthMutationOutcome

    public data object Disabled : WorthMutationOutcome

}

/**
 * Item worth reads (single lookup and a bounded page) and an authorized mutation. Every operation
 * requires a capability on its [AddonAccess]; reads and writes are authorized server-side.
 */
public interface EconomyWorthApi {

    public fun unitPrice(
        access: AddonAccess,
        itemId: String,
        callback: ApiCallback<WorthLookupOutcome>,
    ): ApiRequestHandle

    public fun page(
        access: AddonAccess,
        cursor: String?,
        pageSize: Int,
        callback: ApiCallback<WorthPageOutcome>,
    ): ApiRequestHandle

    public fun set(
        access: AddonAccess,
        itemId: String,
        unitPrice: EconomyAmount,
        callback: ApiCallback<WorthMutationOutcome>,
    ): ApiRequestHandle

}

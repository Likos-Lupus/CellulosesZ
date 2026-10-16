package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import java.time.Instant
import java.util.*

public enum class KitDeliveryStatus {

    DELIVERED,
    DELIVERED_WITH_OVERFLOW,

}

public class KitClaimed(
    public val playerId: UUID,
    public val kitName: String,
    public val deliveryStatus: KitDeliveryStatus,
)

public class KitGranted(
    public val playerId: UUID,
    public val addonId: String,
    public val grantId: String,
    public val kitName: String,
    public val deliveryStatus: KitDeliveryStatus,
)

public enum class KitCatalogChange {

    CREATED,
    UPDATED,
    DELETED,

}

public class KitCatalogChanged(
    public val kitName: String,
    public val change: KitCatalogChange,
    public val occurredAt: Instant,
)

/** Kit fact events; item data is never included. Dispatched on the server thread. */
public interface KitEvents {

    public fun onClaimed(
        listener: ApiEventListener<KitClaimed>,
    ): Subscription

    public fun onGranted(
        listener: ApiEventListener<KitGranted>,
    ): Subscription

    public fun onCatalogChanged(
        listener: ApiEventListener<KitCatalogChanged>,
    ): Subscription

}

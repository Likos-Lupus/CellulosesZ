package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import top.likoslupus.cellulosesz.api.validation.requireNonEmpty
import top.likoslupus.cellulosesz.api.validation.requireNonNegative
import java.math.BigDecimal
import java.time.Duration

/** Immutable kit definition exposed by the public API. */
@JvmRecord
public data class KitDefinition(
    public val id: String,
    public val displayName: String,
    public val permission: String?,
    public val cooldown: Duration,
    public val cost: BigDecimal,
    public val items: List<KitItem>
) {

    init {
        id.requireNonBlank { "id" }
        displayName.requireNonBlank { "displayName" }
        require(
            !(
                    cooldown.nano != 0
                            || (cooldown.isNegative && cooldown != Duration.ofSeconds(-1))
                    )
        ) {
            "cooldown must be whole seconds and at least zero or exactly -1 second"
        }
        cost.requireNonNegative { "cost" }
        items.requireNonEmpty { "items" }
    }

}

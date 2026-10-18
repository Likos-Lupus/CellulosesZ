package top.likoslupus.cellulosesz.core.preference

/**
 * A boolean per-player preference owned by a feature but stored in one shared table. The stable
 * [storageKey] is the only value persisted, so a feature may rename the enum constant without
 * touching stored rows as long as the key is preserved.
 */
public enum class PlayerPreference(public val storageKey: String) {

    TELEPORT_ENABLED("teleport_enabled"),

    ACCEPTING_PAY("accepting_pay"),

    PAY_CONFIRMATION("pay_confirmation"),

    REPLY_TO_LAST_RECIPIENT("reply_to_last_recipient"),

    CLEAR_INVENTORY_CONFIRMATION("clear_inventory_confirmation");

    public companion object {

        public fun fromStorageKey(raw: String): PlayerPreference? =
            entries.firstOrNull { it.storageKey == raw }

    }

}

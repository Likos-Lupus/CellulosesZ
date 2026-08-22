package top.likoslupus.cellulosesz.api.module

import top.likoslupus.cellulosesz.api.validation.TextChecks

@JvmInline
value class ModuleKey(
    val value: String
) : Comparable<ModuleKey> {

    init {
        TextChecks.requireNonBlank(value, "value")
    }

    override fun toString(): String = value

    override fun compareTo(other: ModuleKey): Int = value.compareTo(other.value)

    companion object {

        @JvmStatic
        fun of(value: String): ModuleKey = ModuleKey(value)

    }

}

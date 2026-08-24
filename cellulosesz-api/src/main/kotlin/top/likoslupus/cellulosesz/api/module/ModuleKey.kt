package top.likoslupus.cellulosesz.api.module

import top.likoslupus.cellulosesz.api.validation.requireNonBlank

@JvmInline
public value class ModuleKey(
    public val value: String
) : Comparable<ModuleKey> {

    init {
        value.requireNonBlank { "value" }
    }

    override fun toString(): String = value

    override fun compareTo(other: ModuleKey): Int = value.compareTo(other.value)

    public companion object {

        @JvmStatic
        public fun of(value: String): ModuleKey = ModuleKey(value)

    }

}

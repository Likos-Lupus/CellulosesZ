package top.likoslupus.cellulosesz.api.text

import java.math.BigDecimal
import java.util.*

/** Closed set of values that may be rendered into a localized message. */
public sealed interface MessageArgument {

    @JvmRecord
    public data class Text(val value: String) : MessageArgument

    @JvmRecord
    public data class Number(val value: BigDecimal) : MessageArgument

    @JvmRecord
    public data class BooleanValue(val value: Boolean) : MessageArgument

    @JvmRecord
    public data class UuidValue(val value: UUID) : MessageArgument

    @JvmRecord
    public data class RichTextValue(val value: RichText) : MessageArgument

    @JvmRecord
    public data class NestedMessage(val value: LocalizedMessage) : MessageArgument

    public companion object {

        @JvmStatic
        public fun text(value: String): MessageArgument = Text(value)

        @JvmStatic
        public fun number(value: Int): MessageArgument = Number(BigDecimal.valueOf(value.toLong()))

        @JvmStatic
        public fun number(value: Long): MessageArgument = Number(BigDecimal.valueOf(value))

        @JvmStatic
        public fun number(value: Double): MessageArgument = Number(BigDecimal.valueOf(value))

        @JvmStatic
        public fun number(value: BigDecimal): MessageArgument = Number(value)

        @JvmStatic
        public fun bool(value: Boolean): MessageArgument = BooleanValue(value)

        @JvmStatic
        public fun uuid(value: UUID): MessageArgument = UuidValue(value)

        @JvmStatic
        public fun richText(value: RichText): MessageArgument = RichTextValue(value)

        @JvmStatic
        public fun nested(value: LocalizedMessage): MessageArgument = NestedMessage(value)

    }

}

package top.likoslupus.cellulosesz.api.text

import top.likoslupus.cellulosesz.api.util.toImmutableList
import java.math.BigDecimal
import java.util.*

/** Immutable ordered collection of typed message arguments. */
public class MessageArguments private constructor(
    private val values: List<MessageArgument>
) {

    public fun values(): List<MessageArgument> = values

    public fun isEmpty(): Boolean = values.isEmpty()

    public class Builder internal constructor() {

        private val values: MutableList<MessageArgument> = ArrayList()

        public fun add(value: String): Builder =
            add(MessageArgument.text(value))

        public fun add(value: MessageArgument): Builder {
            values.add(value)
            return this
        }

        public fun add(value: Int): Builder =
            add(MessageArgument.number(value))

        public fun add(value: Long): Builder =
            add(MessageArgument.number(value))

        public fun add(value: Double): Builder =
            add(MessageArgument.number(value))

        public fun add(value: Boolean): Builder =
            add(MessageArgument.bool(value))

        public fun add(value: BigDecimal): Builder =
            add(MessageArgument.number(value))

        public fun add(value: UUID): Builder =
            add(MessageArgument.uuid(value))

        public fun add(value: RichText): Builder =
            add(MessageArgument.richText(value))

        public fun add(value: LocalizedMessage): Builder =
            add(MessageArgument.nested(value))

        public fun addAll(arguments: MessageArguments): Builder {
            values.addAll(arguments.values)
            return this
        }

        public fun build(): MessageArguments =
            if (values.isEmpty()) {
                EMPTY
            } else {
                MessageArguments(values.toImmutableList())
            }

    }

    public companion object {

        private val EMPTY = MessageArguments(emptyList())

        @JvmStatic
        public fun empty(): MessageArguments = EMPTY

        @JvmStatic
        public fun of(vararg arguments: MessageArgument): MessageArguments =
            if (arguments.isEmpty()) {
                EMPTY
            } else {
                MessageArguments(arguments.toList())
            }

        @JvmStatic
        public fun builder(): Builder = Builder()

    }

}

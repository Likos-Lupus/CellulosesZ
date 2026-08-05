package top.likoslupus.cellulosesz.api.text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/** Immutable ordered collection of typed message arguments. */
public final class MessageArguments {

    private static final MessageArguments EMPTY = new MessageArguments(List.of());

    private final List<MessageArgument> values;

    private MessageArguments(List<MessageArgument> values) {
        this.values = List.copyOf(requireNonNull(values, "values"));
    }

    public static MessageArguments empty() {
        return EMPTY;
    }

    public static MessageArguments of(MessageArgument... arguments) {
        requireNonNull(arguments, "arguments");
        if (arguments.length == 0) {
            return EMPTY;
        }
        return new MessageArguments(List.of(arguments));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<MessageArgument> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public static final class Builder {

        private final List<MessageArgument> values = new ArrayList<>();

        private Builder() {
        }

        public Builder add(String value) {
            return add(MessageArgument.text(value));
        }

        public Builder add(MessageArgument value) {
            values.add(requireNonNull(value, "value"));
            return this;
        }

        public Builder add(int value) {
            return add(MessageArgument.number(value));
        }

        public Builder add(long value) {
            return add(MessageArgument.number(value));
        }

        public Builder add(double value) {
            return add(MessageArgument.number(value));
        }

        public Builder add(boolean value) {
            return add(MessageArgument.bool(value));
        }

        public Builder add(BigDecimal value) {
            return add(MessageArgument.number(value));
        }

        public Builder add(UUID value) {
            return add(MessageArgument.uuid(value));
        }

        public Builder add(RichText value) {
            return add(MessageArgument.richText(value));
        }

        public Builder add(LocalizedMessage value) {
            return add(MessageArgument.nested(value));
        }

        public Builder addAll(MessageArguments arguments) {
            values.addAll(requireNonNull(arguments, "arguments").values);
            return this;
        }

        public MessageArguments build() {
            return values.isEmpty()
                    ? EMPTY
                    : new MessageArguments(values);
        }

    }

}

package top.likoslupus.cellulosesz.api.text;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/** Closed set of values that may be rendered into a localized message. */
public sealed interface MessageArgument permits
        MessageArgument.Text,
        MessageArgument.Number,
        MessageArgument.BooleanValue,
        MessageArgument.UuidValue,
        MessageArgument.RichTextValue,
        MessageArgument.NestedMessage {

    static MessageArgument text(String value) {
        return new Text(value);
    }

    static MessageArgument number(int value) {
        return new Number(BigDecimal.valueOf(value));
    }

    static MessageArgument number(long value) {
        return new Number(BigDecimal.valueOf(value));
    }

    static MessageArgument number(double value) {
        return new Number(BigDecimal.valueOf(value));
    }

    static MessageArgument number(BigDecimal value) {
        return new Number(value);
    }

    static MessageArgument bool(boolean value) {
        return new BooleanValue(value);
    }

    static MessageArgument uuid(UUID value) {
        return new UuidValue(value);
    }

    static MessageArgument richText(RichText value) {
        return new RichTextValue(value);
    }

    static MessageArgument nested(LocalizedMessage value) {
        return new NestedMessage(value);
    }

    record Text(String value) implements MessageArgument {

        public Text {
            value = requireNonNull(value, "value");
        }

    }

    record Number(BigDecimal value) implements MessageArgument {

        public Number {
            requireNonNull(value, "value");
        }

    }

    record BooleanValue(boolean value) implements MessageArgument {

    }

    record UuidValue(UUID value) implements MessageArgument {

        public UuidValue {
            requireNonNull(value, "value");
        }

    }

    record RichTextValue(RichText value) implements MessageArgument {

        public RichTextValue {
            requireNonNull(value, "value");
        }

    }

    record NestedMessage(LocalizedMessage value) implements MessageArgument {

        public NestedMessage {
            requireNonNull(value, "value");
        }

    }

}

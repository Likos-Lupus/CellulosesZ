package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;

final class MessageTemplateArguments {

    private static final String INTERNAL_TAG_PREFIX = "cellulosesz_arg_";

    private MessageTemplateArguments() {
    }

    static CompiledTemplate compile(String template) {
        if (template.contains("<" + INTERNAL_TAG_PREFIX)) {
            throw new IllegalArgumentException(
                    "Message template contains the reserved internal argument tag prefix"
            );
        }

        var output = new StringBuilder(template.length());
        var indexes = new LinkedHashSet<Integer>();
        for (var index = 0; index < template.length(); index++) {
            var character = template.charAt(index);
            if (character == '\\' && index + 1 < template.length()
                    && template.charAt(index + 1) == '{'
            ) {
                var closing = template.indexOf('}', index + 2);
                if (closing >= 0
                        && isDigits(template, index + 2, closing)
                ) {
                    output.append(template, index + 1, closing + 1);
                    index = closing;
                    continue;
                }
            }

            if (character == '{') {
                var closing = template.indexOf('}', index + 1);
                if (closing < 0) {
                    throw new IllegalArgumentException("Unclosed positional argument token");
                }

                if (!isDigits(template, index + 1, closing)) {
                    throw new IllegalArgumentException(
                            "Message arguments must use non-negative numeric indexes"
                    );
                }

                if (closing - index > 2 && template.charAt(index + 1) == '0') {
                    throw new IllegalArgumentException(
                            "Message argument indexes must not contain leading zeroes"
                    );
                }

                final int argumentIndex;
                try {
                    argumentIndex = Integer.parseInt(
                            template,
                            index + 1,
                            closing,
                            10
                    );
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Message argument index is too large",
                            exception
                    );
                }

                indexes.add(argumentIndex);
                output.append('<')
                        .append(INTERNAL_TAG_PREFIX)
                        .append(argumentIndex)
                        .append('>');
                index = closing;
                continue;
            }

            if (character == '}') {
                throw new IllegalArgumentException("Unmatched closing brace in message template");
            }

            output.append(character);
        }

        var maximum = indexes.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);
        IntStream.rangeClosed(0, maximum)
                .filter(index -> !indexes.contains(index))
                .forEach(index -> {
                    throw new IllegalArgumentException(
                            "Message argument indexes must be continuous from 0; missing " + index
                    );
                });

        return new CompiledTemplate(
                output.toString(),
                Set.copyOf(indexes),
                maximum + 1
        );
    }

    private static boolean isDigits(
            String value,
            int start,
            int end
    ) {
        if (start == end) {
            return false;
        }

        return IntStream.range(start, end)
                .noneMatch(index -> {
                    var character = value.charAt(index);
                    return character >= '0' && character <= '9';
                });
    }

    static String resolverName(int index) {
        return INTERNAL_TAG_PREFIX + index;
    }

    record CompiledTemplate(
            String miniMessage,
            Set<Integer> indexes,
            int argumentCount
    ) {

    }

}

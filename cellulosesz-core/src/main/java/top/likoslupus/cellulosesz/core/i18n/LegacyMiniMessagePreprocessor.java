package top.likoslupus.cellulosesz.core.i18n;

import java.util.Map;

final class LegacyMiniMessagePreprocessor {

    private static final Map<Character, String> TAGS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    private LegacyMiniMessagePreprocessor() {
    }

    static String convert(String input) {
        var output = new StringBuilder(input.length());
        for (var index = 0; index < input.length(); index++) {
            var current = input.charAt(index);
            if ((current != '&' && current != '§') || index + 1 >= input.length()) {
                output.append(current);
                continue;
            }

            if (input.charAt(index + 1) == '#' && index + 7 < input.length()) {
                var hex = input.substring(index + 2, index + 8);
                if (hex.chars().allMatch(LegacyMiniMessagePreprocessor::hexDigit)) {
                    output.append("<#").append(hex).append('>');
                    index += 7;
                    continue;
                }
            }

            var tag = TAGS.get(Character.toLowerCase(input.charAt(index + 1)));
            if (tag == null) {
                output.append(current);
                continue;
            }
            output.append('<').append(tag).append('>');
            index++;
        }
        return output.toString();
    }

    private static boolean hexDigit(int value) {
        return value >= '0' && value <= '9'
                || value >= 'a' && value <= 'f'
                || value >= 'A' && value <= 'F';
    }

}

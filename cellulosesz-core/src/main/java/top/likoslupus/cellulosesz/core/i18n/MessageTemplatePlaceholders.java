package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class MessageTemplatePlaceholders {

    private static final Pattern TAG = Pattern.compile("(?<!\\\\)<([A-Za-z_][A-Za-z0-9_-]*)>");
    private static final Set<String> FORMATTING_TAGS = Set.of(
            "primary", "secondary",
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white",
            "bold", "b", "italic", "i", "underlined", "underline", "u",
            "strikethrough", "st", "obfuscated", "magic", "reset"
    );

    private MessageTemplatePlaceholders() {
    }

    static Set<String> names(String template) {
        var names = new LinkedHashSet<String>();
        var matcher = TAG.matcher(template);
        while (matcher.find()) {
            var name = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!FORMATTING_TAGS.contains(name)) names.add(name);
        }
        return Set.copyOf(names);
    }

}

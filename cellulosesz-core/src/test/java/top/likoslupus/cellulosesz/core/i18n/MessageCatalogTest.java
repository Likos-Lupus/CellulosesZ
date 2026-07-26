package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

final class MessageCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    @Test
    void englishAndChineseKeysAndPlaceholderOrderMatch() {
        var english = BuiltInCommandMessages.english();
        var chinese = BuiltInCommandMessages.chinese();
        assertEquals(english.keySet(), chinese.keySet());
        english.forEach((key, value) -> assertEquals(
                placeholders(value), placeholders(chinese.get(key)), key));
    }

    private static java.util.List<String> placeholders(String text) {
        var result = new ArrayList<String>();
        var matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }

    @Test
    void catalogsContainNoDevelopmentStageNames() {
        BuiltInCommandMessages.english().forEach((key, value) -> {
            var combined = (key + " " + value).toLowerCase();
            assertFalse(combined.contains("stagee"));
            assertFalse(combined.contains("stage e"));
            assertFalse(combined.contains("phase-e"));
        });
    }

}

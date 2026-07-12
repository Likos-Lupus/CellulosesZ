package top.likoslupus.cellulosesz.api.text;

import java.util.Map;

public interface MessageRenderer {

    default RichText render(String locale, String key) {
        return render(locale, key, Map.of());
    }

    RichText render(
            String locale,
            String key,
            Map<String, ?> placeholders
    );

    default RichText render(String locale, LocalizedMessage message) {
        return render(locale, message.key(), message.placeholders());
    }

    default RichText renderInline(String locale, String template) {
        return renderInline(locale, template, Map.of());
    }

    RichText renderInline(
            String locale,
            String template,
            Map<String, ?> placeholders
    );

}

package top.likoslupus.cellulosesz.api.text;

public interface MessageRenderer {

    default RichText render(String locale, String key) {
        return render(locale, key, MessageArguments.empty());
    }

    RichText render(
            String locale,
            String key,
            MessageArguments placeholders
    );

    default RichText render(String locale, LocalizedMessage message) {
        return render(locale, message.key(), message.placeholders());
    }

    default RichText renderInline(String locale, String template) {
        return renderInline(locale, template, MessageArguments.empty());
    }

    RichText renderInline(
            String locale,
            String template,
            MessageArguments placeholders
    );

}

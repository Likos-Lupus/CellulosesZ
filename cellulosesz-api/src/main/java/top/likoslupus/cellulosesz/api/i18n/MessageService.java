package top.likoslupus.cellulosesz.api.i18n;

import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.Map;

public interface MessageService {

    String message(String key);

    String message(
            String key,
            Map<String, ?> placeholders
    );

    RichText rich(
            String locale,
            String key,
            Map<String, ?> placeholders
    );

    boolean contains(String locale, String key);

    void reload();

}

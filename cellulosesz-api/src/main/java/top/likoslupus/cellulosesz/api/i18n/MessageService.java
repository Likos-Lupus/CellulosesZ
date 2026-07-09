package top.likoslupus.cellulosesz.api.i18n;

import java.util.Map;

public interface MessageService {

    String message(String key);

    String message(String key, Map<String, ?> placeholders);

    void reload();

}

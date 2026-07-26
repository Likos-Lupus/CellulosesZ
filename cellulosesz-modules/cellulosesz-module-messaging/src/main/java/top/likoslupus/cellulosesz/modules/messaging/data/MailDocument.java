package top.likoslupus.cellulosesz.modules.messaging.data;

import top.likoslupus.cellulosesz.api.messaging.MailMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MailDocument {

    public Map<String, List<MailMessage>> inboxes = new LinkedHashMap<>();

    public MailDocument copy() {
        var copy = new MailDocument();
        inboxes.forEach((recipient, messages) ->
                copy.inboxes.put(recipient, new ArrayList<>(messages))
        );
        return copy;
    }

}

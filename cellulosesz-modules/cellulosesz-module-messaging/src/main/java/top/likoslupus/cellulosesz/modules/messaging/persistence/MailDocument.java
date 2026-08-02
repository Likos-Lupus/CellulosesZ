package top.likoslupus.cellulosesz.modules.messaging.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MailDocument {

    public Map<String, List<MailMessageDocument>> inboxes = new LinkedHashMap<>();

    public MailDocument copy() {
        var copy = new MailDocument();
        inboxes.forEach((recipient, messages) -> {
            var entries = new ArrayList<MailMessageDocument>();
            messages.forEach(message -> entries.add(MailMapper.copy(message)));
            copy.inboxes.put(recipient, entries);
        });
        return copy;
    }

}

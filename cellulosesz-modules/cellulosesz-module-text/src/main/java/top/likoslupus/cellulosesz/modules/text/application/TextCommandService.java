package top.likoslupus.cellulosesz.modules.text.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Set;

public interface TextCommandService {

    PageResult info(int page);

    PageResult motd(int page);

    PageResult rules(int page);

    PageResult custom(String name, int page);

    Set<String> customNames();

    record PageResult(
            boolean success,
            List<LocalizedMessage> messages
    ) {

        public PageResult {
            messages = List.copyOf(messages);
        }

    }

}

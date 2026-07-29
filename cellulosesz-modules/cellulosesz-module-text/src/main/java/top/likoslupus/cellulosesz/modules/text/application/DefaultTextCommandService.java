package top.likoslupus.cellulosesz.modules.text.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class DefaultTextCommandService implements TextCommandService {

    private final TextService texts;

    public DefaultTextCommandService(TextService texts) {
        this.texts = requireNonNull(texts, "texts");
    }

    @Override
    public PageResult info(int page) {
        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_INFO_TITLE,
                Map.of(),
                texts.info(),
                page
        );
    }

    @Override
    public PageResult motd(int page) {
        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_MOTD_TITLE,
                Map.of(),
                texts.motd(),
                page
        );
    }

    @Override
    public PageResult rules(int page) {
        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_RULES_TITLE,
                Map.of(),
                texts.rules(),
                page
        );
    }

    @Override
    public PageResult custom(String name, int page) {
        var lines = texts.custom(name);
        if (lines.isEmpty()) {
            return failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_TEXT_CUSTOM_MISSING,
                    Map.of("name", name)
            ));
        }

        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_CUSTOM_TITLE,
                Map.of("name", name),
                lines,
                page
        );
    }

    @Override
    public Set<String> customNames() {
        return texts.customNames();
    }

    private PageResult page(
            String titleKey,
            Map<String, ?> titlePlaceholders,
            List<String> lines,
            int requestedPage
    ) {
        if (requestedPage < 1) {
            return failure(LocalizedMessage.of(GeneratedMessageKeys.COMMANDS_COMMON_INVALID_PAGE));
        }
        if (lines.isEmpty()) {
            return failure(LocalizedMessage.of(GeneratedMessageKeys.COMMANDS_TEXT_EMPTY));
        }

        var pageSize = Math.max(1, texts.pageSize());
        var pages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);

        if (requestedPage > pages) {
            return failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                    Map.of("pages", pages)
            ));
        }

        final int start;
        try {
            start = Math.multiplyExact(requestedPage - 1, pageSize);
        } catch (ArithmeticException _) {
            return failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                    Map.of("pages", pages)
            ));
        }

        var messages = new ArrayList<LocalizedMessage>();
        var titleValues = new LinkedHashMap<String, Object>(titlePlaceholders);

        titleValues.put("page", requestedPage);
        titleValues.put("pages", pages);
        messages.add(LocalizedMessage.of(titleKey, titleValues));

        var end = (int) Math.min((long) start + pageSize, lines.size());
        IntStream.range(start, end)
                .mapToObj(index -> LocalizedMessage.of(
                        GeneratedMessageKeys.COMMANDS_TEXT_LINE,
                        Map.of("line", lines.get(index))
                ))
                .forEach(messages::add);

        return new PageResult(true, messages);
    }

    private PageResult failure(LocalizedMessage message) {
        return new PageResult(
                false,
                List.of(requireNonNull(message, "message"))
        );
    }

}

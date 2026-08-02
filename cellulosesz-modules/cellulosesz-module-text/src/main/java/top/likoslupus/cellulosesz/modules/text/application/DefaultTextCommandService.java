package top.likoslupus.cellulosesz.modules.text.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                MessageArguments.empty(),
                texts.info(),
                page
        );
    }

    @Override
    public PageResult motd(int page) {
        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_MOTD_TITLE,
                MessageArguments.empty(),
                texts.motd(),
                page
        );
    }

    @Override
    public PageResult rules(int page) {
        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_RULES_TITLE,
                MessageArguments.empty(),
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
                    MessageArguments.builder().put("name", name).build()
            ));
        }

        return page(
                GeneratedMessageKeys.COMMANDS_TEXT_CUSTOM_TITLE,
                MessageArguments.of("name", name),
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
            MessageArguments titlePlaceholders,
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
                    MessageArguments.builder().put("pages", pages).build()
            ));
        }

        final int start;
        try {
            start = Math.multiplyExact(requestedPage - 1, pageSize);
        } catch (ArithmeticException _) {
            return failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                    MessageArguments.builder().put("pages", pages).build()
            ));
        }

        var messages = new ArrayList<LocalizedMessage>();
        var titleValues = MessageArguments.builder()
                .putAll(titlePlaceholders)
                .put("page", requestedPage)
                .put("pages", pages)
                .build();
        messages.add(LocalizedMessage.of(titleKey, titleValues));

        var end = (int) Math.min((long) start + pageSize, lines.size());
        for (var index = start; index < end; index++) {
            messages.add(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_TEXT_LINE,
                    MessageArguments.of("line", lines.get(index))
            ));
        }

        return new PageResult(true, messages);
    }

    private PageResult failure(LocalizedMessage message) {
        return new PageResult(
                false,
                List.of(requireNonNull(message, "message"))
        );
    }

}

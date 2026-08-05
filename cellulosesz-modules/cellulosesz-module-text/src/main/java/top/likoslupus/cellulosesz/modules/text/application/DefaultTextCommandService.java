package top.likoslupus.cellulosesz.modules.text.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.TextService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
                "commands.text.info-title",
                MessageArguments.empty(),
                texts.info(),
                page
        );
    }

    @Override
    public PageResult motd(int page) {
        return page(
                "commands.text.motd-title",
                MessageArguments.empty(),
                texts.motd(),
                page
        );
    }

    @Override
    public PageResult rules(int page) {
        return page(
                "commands.text.rules-title",
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
                    "commands.text.custom-missing",
                    MessageArguments.empty()
            ));
        }

        return page(
                "commands.text.custom-title",
                MessageArguments.builder().add(name).build(),
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
            MessageArguments titleArguments,
            List<String> lines,
            int requestedPage
    ) {
        if (requestedPage < 1) {
            return failure(LocalizedMessage.of("commands.common.invalid-page"));
        }

        if (lines.isEmpty()) {
            return failure(LocalizedMessage.of("commands.text.empty"));
        }

        var pageSize = Math.max(1, texts.pageSize());
        var pages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);

        if (requestedPage > pages) {
            return failure(LocalizedMessage.of(
                    "commands.common.page-out-of-range",
                    MessageArguments.builder().add(pages).build()
            ));
        }

        final int start;
        try {
            start = Math.multiplyExact(requestedPage - 1, pageSize);
        } catch (ArithmeticException _) {
            return failure(LocalizedMessage.of(
                    "commands.common.page-out-of-range",
                    MessageArguments.builder().add(pages).build()
            ));
        }

        var messages = new ArrayList<LocalizedMessage>();
        var titleValues = MessageArguments.builder()
                .addAll(titleArguments)
                .add(requestedPage)
                .add(pages)
                .build();
        messages.add(LocalizedMessage.of(titleKey, titleValues));

        var end = (int) Math.min((long) start + pageSize, lines.size());
        IntStream.range(start, end)
                .mapToObj(index -> LocalizedMessage.of(
                        "commands.text.line",
                        MessageArguments.builder().add(lines.get(index)).build()
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

package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import static java.util.Objects.requireNonNull;

public final class InfoSignHandler implements SynchronousSignHandler {

    private final TextService texts;

    public InfoSignHandler(TextService texts) {
        this.texts = requireNonNull(texts, "texts");
    }

    @Override
    public String id() {
        return "Info";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return SignHandlerSupport.textPage(texts, context).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.info-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return SignHandlerSupport.textPage(texts, context)
                .map(value -> SignUseResult.success(
                        "service.sign.info",
                        MessageArguments.builder().add(value).build()
                ))
                .orElseGet(() -> SignUseResult.failure("service.sign.info-format"));
    }

}

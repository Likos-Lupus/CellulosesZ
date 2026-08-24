package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class MailSignHandler implements CellSignHandler {

    private final MailService mail;

    public MailSignHandler(MailService mail) {
        this.mail = requireNonNull(mail, "mail");
    }

    @Override
    public String id() {
        return "Mail";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return SignHandlerSupport.noArguments(context, "service.sign.mail-format");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        return mail
                .unreadCount(context.player().uuid())
                .thenApply(unread -> SignUseResult.success(
                        "service.sign.mail",
                        MessageArguments.builder().add(unread).build()
                ));
    }

}

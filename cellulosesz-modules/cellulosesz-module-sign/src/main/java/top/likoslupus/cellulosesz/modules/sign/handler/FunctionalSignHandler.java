package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class FunctionalSignHandler implements CellSignHandler {

    private final String id;
    private final Function<SignUseContext, SignUseResult> validator;
    private final Function<SignUseContext, SignUseResult> action;

    FunctionalSignHandler(
            String id,
            Function<SignUseContext, SignUseResult> validator,
            Function<SignUseContext, SignUseResult> action
    ) {
        this.id = requireText(id);
        this.validator = requireNonNull(validator, "validator");
        this.action = requireNonNull(action, "action");
    }

    private static String requireText(String value) {
        var checked = requireNonNull(value, "id").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("Sign id must not be blank");
        }
        return checked;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return validator.apply(context);
    }

    @Override
    public SignUseResult use(SignUseContext context) {
        return action.apply(context);
    }

}

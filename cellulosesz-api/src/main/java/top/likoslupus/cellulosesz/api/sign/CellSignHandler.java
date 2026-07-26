package top.likoslupus.cellulosesz.api.sign;

import java.util.concurrent.CompletableFuture;

public interface CellSignHandler {

    String id();

    default SignUseResult validate(SignUseContext context) {
        return SignUseResult.success("service.sign.valid");
    }

    CompletableFuture<SignUseResult> use(SignUseContext context);

}

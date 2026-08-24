package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import java.util.concurrent.CompletableFuture;

public interface CellSignHandler {

    String id();

    default SignUseResult validate(SignUseContext context) {
        return SignUseResult.success("service.sign.valid");
    }

    CompletableFuture<SignUseResult> use(SignUseContext context);

}

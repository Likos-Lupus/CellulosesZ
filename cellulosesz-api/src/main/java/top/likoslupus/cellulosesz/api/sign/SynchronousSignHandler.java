package top.likoslupus.cellulosesz.api.sign;

import java.util.concurrent.CompletableFuture;

public interface SynchronousSignHandler extends CellSignHandler {

    @Override
    default CompletableFuture<SignUseResult> use(SignUseContext context) {
        try {
            return CompletableFuture.completedFuture(useSynchronously(context));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    SignUseResult useSynchronously(SignUseContext context);

}

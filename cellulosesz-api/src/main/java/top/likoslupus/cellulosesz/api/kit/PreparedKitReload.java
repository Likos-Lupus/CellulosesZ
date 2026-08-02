package top.likoslupus.cellulosesz.api.kit;

import java.util.concurrent.CompletionStage;

public interface PreparedKitReload {

    CompletionStage<Void> commit();

    CompletionStage<Void> rollback();

}

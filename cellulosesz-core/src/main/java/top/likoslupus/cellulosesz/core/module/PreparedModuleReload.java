package top.likoslupus.cellulosesz.core.module;

import java.util.concurrent.CompletionStage;

public interface PreparedModuleReload {

    CompletionStage<Void> commit();

    CompletionStage<Void> rollback();

}

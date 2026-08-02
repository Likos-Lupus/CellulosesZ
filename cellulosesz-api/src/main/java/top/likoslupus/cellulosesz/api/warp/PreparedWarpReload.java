package top.likoslupus.cellulosesz.api.warp;

import java.util.concurrent.CompletionStage;

public interface PreparedWarpReload {

    CompletionStage<Void> commit();

    CompletionStage<Void> rollback();

}

package top.likoslupus.cellulosesz.core.runtime;

import top.likoslupus.cellulosesz.api.module.LoadedModuleInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RuntimeService {

    String version();

    CompletableFuture<Void> reload();

    List<LoadedModuleInfo> modules();

}

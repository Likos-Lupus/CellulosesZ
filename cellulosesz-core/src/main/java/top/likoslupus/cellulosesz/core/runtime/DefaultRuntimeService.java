package top.likoslupus.cellulosesz.core.runtime;

import top.likoslupus.cellulosesz.api.module.LoadedModuleInfo;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DefaultRuntimeService implements RuntimeService {

    private final CellulosesZBootstrap bootstrap;

    public DefaultRuntimeService(CellulosesZBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public String version() {
        return bootstrap.version();
    }

    @Override
    public CompletableFuture<Void> reload() {
        return bootstrap.reload();
    }

    @Override
    public List<LoadedModuleInfo> modules() {
        return bootstrap.modules();
    }

}

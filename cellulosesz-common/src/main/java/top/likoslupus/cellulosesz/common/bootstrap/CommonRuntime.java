package top.likoslupus.cellulosesz.common.bootstrap;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.common.command.CommandRootMutator;
import top.likoslupus.cellulosesz.common.lifecycle.CommonRuntimeHooks;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;

import static java.util.Objects.requireNonNull;

public record CommonRuntime(
        CellulosesZBootstrap bootstrap,
        PlatformService platform,
        CommonRuntimeHooks hooks,
        CommandRootMutator commandRoots
) {

    public CommonRuntime {
        bootstrap = requireNonNull(bootstrap, "bootstrap");
        platform = requireNonNull(platform, "platform");
        hooks = requireNonNull(hooks, "hooks");
        commandRoots = requireNonNull(commandRoots, "commandRoots");
    }

}

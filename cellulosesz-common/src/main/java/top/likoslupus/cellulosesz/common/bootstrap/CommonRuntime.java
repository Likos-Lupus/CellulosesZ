package top.likoslupus.cellulosesz.common.bootstrap;

import top.likoslupus.cellulosesz.common.command.CommandRootMutator;
import top.likoslupus.cellulosesz.common.lifecycle.CommonRuntimeHooks;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;

import static java.util.Objects.requireNonNull;

public record CommonRuntime(
        CellulosesZBootstrap bootstrap,
        CommonRuntimeHooks hooks,
        CommandRootMutator commandRoots
) {

    public CommonRuntime {
        requireNonNull(bootstrap, "bootstrap");
        requireNonNull(hooks, "hooks");
        requireNonNull(commandRoots, "commandRoots");
    }

}

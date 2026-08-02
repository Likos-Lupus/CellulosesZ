package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.config.ConfigSnapshot;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModuleReloadContext;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

final class DefaultModuleReloadContext implements ModuleReloadContext {

    private final ModuleContext module;
    private final ConfigSnapshot configs;
    private final Predicate<String> enabled;

    DefaultModuleReloadContext(
            ModuleContext module,
            ConfigSnapshot configs,
            Predicate<String> enabled
    ) {
        this.module = requireNonNull(module, "module");
        this.configs = requireNonNull(configs, "configs");
        this.enabled = requireNonNull(enabled, "enabled");
    }

    @Override
    public ModuleContext module() {
        return module;
    }

    @Override
    public ConfigSnapshot configs() {
        return configs;
    }

    @Override
    public boolean moduleEnabled(String moduleId) {
        return enabled.test(moduleId);
    }

}

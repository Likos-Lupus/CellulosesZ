package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.core.config.ConfigSnapshot;

public interface ModuleReloadContext {

    ModuleContext module();

    ConfigSnapshot configs();

    boolean moduleEnabled(String moduleId);

}

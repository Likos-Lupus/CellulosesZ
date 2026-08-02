package top.likoslupus.cellulosesz.api.module;

import top.likoslupus.cellulosesz.api.config.ConfigSnapshot;

public interface ModuleReloadContext {

    ModuleContext module();

    ConfigSnapshot configs();

    boolean moduleEnabled(String moduleId);

}

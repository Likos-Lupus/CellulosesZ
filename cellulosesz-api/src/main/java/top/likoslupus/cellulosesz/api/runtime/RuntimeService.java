package top.likoslupus.cellulosesz.api.runtime;

import top.likoslupus.cellulosesz.api.module.LoadedModuleInfo;

import java.util.List;

public interface RuntimeService {

    String version();

    void reload();

    List<LoadedModuleInfo> modules();

}

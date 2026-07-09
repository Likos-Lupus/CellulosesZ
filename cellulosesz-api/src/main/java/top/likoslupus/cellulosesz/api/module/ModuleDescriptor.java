package top.likoslupus.cellulosesz.api.module;

import java.util.List;

public record ModuleDescriptor(
        String id,
        String name,
        String description,
        ModulePhase phase,
        int priority,
        List<String> requires,
        List<String> optional,
        boolean enabledByDefault,
        Class<? extends CellulosesZModule> moduleClass
) {

}

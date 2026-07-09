package top.likoslupus.cellulosesz.api.module;

public record LoadedModuleInfo(
        String id,
        String name,
        String description,
        ModulePhase phase,
        boolean enabled,
        String className
) {

}

package top.likoslupus.cellulosesz.core.module;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleDescriptor;
import top.likoslupus.cellulosesz.api.module.ModuleLoadException;
import top.likoslupus.cellulosesz.api.module.ModuleScanner;

import java.util.List;

public final class ClassGraphModuleScanner implements ModuleScanner {

    private static final String BASE_PACKAGE = "top.likoslupus.cellulosesz";

    @Override
    public List<ModuleDescriptor> scan() {
        try (
                var result = new ClassGraph()
                        .enableClassInfo()
                        .enableAnnotationInfo()
                        .acceptPackages(BASE_PACKAGE)
                        .scan()
        ) {
            return result.getClassesWithAnnotation(CellulosesModule.class.getName())
                    .stream()
                    .map(this::descriptor)
                    .toList();
        }
    }

    private ModuleDescriptor descriptor(ClassInfo info) {
        var type = loadModuleClass(info);
        var annotation = type.getAnnotation(CellulosesModule.class);

        if (annotation == null) {
            throw new ModuleLoadException("Class " + type.getName() + " was discovered as a CellulosesZ module, but its annotation could not be loaded by the CellulosesZ class loader");
        }
        if (!CellulosesZModule.class.isAssignableFrom(type)) {
            throw new ModuleLoadException("Class " + type.getName() + " is annotated with @CellulosesModule but does not implement CellulosesZModule");
        }

        return new ModuleDescriptor(
                annotation.id(),
                annotation.name().isBlank() ? annotation.id() : annotation.name(),
                annotation.description(),
                annotation.phase(),
                annotation.priority(),
                List.of(annotation.requires()),
                List.of(annotation.optional()),
                annotation.enabledByDefault(),
                type.asSubclass(CellulosesZModule.class)
        );
    }

    private Class<?> loadModuleClass(ClassInfo info) {
        try {
            return Class.forName(
                    info.getName(),
                    false,
                    CellulosesZModule.class.getClassLoader()
            );
        } catch (ClassNotFoundException exception) {
            throw new ModuleLoadException("Failed to load CellulosesZ module class " + info.getName(), exception);
        }
    }

}

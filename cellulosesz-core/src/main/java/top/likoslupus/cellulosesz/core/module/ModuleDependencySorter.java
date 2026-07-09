package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.module.ModuleDescriptor;
import top.likoslupus.cellulosesz.api.module.ModuleLoadException;

import java.util.*;

public final class ModuleDependencySorter {

    public List<ModuleDescriptor> sort(List<ModuleDescriptor> descriptors) {
        Map<String, ModuleDescriptor> byId = new LinkedHashMap<>();
        descriptors.stream()
                .filter(descriptor -> byId.put(descriptor.id(), descriptor) != null)
                .forEach(descriptor -> {
                    throw new ModuleLoadException("Duplicate module id: " + descriptor.id());
                });

        var baseOrder = descriptors.stream()
                .sorted(
                        Comparator.comparing(ModuleDescriptor::phase)
                                .thenComparingInt(ModuleDescriptor::priority)
                                .thenComparing(ModuleDescriptor::id)
                )
                .toList();

        List<ModuleDescriptor> sorted = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parent = new HashMap<>();

        baseOrder.forEach(descriptor -> visit(
                descriptor,
                byId,
                visiting,
                visited,
                parent,
                sorted
        ));
        return sorted;
    }

    private void visit(
            ModuleDescriptor descriptor,
            Map<String, ModuleDescriptor> byId,
            Set<String> visiting,
            Set<String> visited,
            Map<String, String> parent,
            List<ModuleDescriptor> sorted
    ) {
        if (visited.contains(descriptor.id())) return;
        if (!visiting.add(descriptor.id())) {
            throw new ModuleLoadException("Module dependency cycle detected at " + descriptor.id());
        }

        for (var required : descriptor.requires()) {
            var dependency = byId.get(required);
            if (dependency == null) {
                throw new ModuleLoadException("Module " + descriptor.id() + " requires missing module " + required);
            }
            parent.put(required, descriptor.id());
            visit(dependency, byId, visiting, visited, parent, sorted);
        }

        visiting.remove(descriptor.id());
        visited.add(descriptor.id());
        sorted.add(descriptor);
    }

}

package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResolution;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class MinecraftWorldDirectory implements WorldDirectory {

    private final MinecraftServerHandle server;

    public MinecraftWorldDirectory(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public List<String> loadedWorldIds() {
        var current = server.current();
        return current.isEmpty()
                ? List.of()
                : StreamSupport.stream(current.orElseThrow().getAllLevels().spliterator(), false)
                        .map(level -> level.dimension().identifier().toString())
                        .sorted()
                        .toList();
    }

    @Override
    public WorldResolution resolve(String input) {
        var normalized = input.trim().toLowerCase(Locale.ROOT);
        var loaded = loadedWorldIds();
        var exact = loaded.stream().filter(id -> id.equalsIgnoreCase(normalized)).toList();

        if (exact.size() == 1) {
            return WorldResolution.resolved(exact.getFirst());
        }
        if (normalized.contains(":")) {
            return WorldResolution.notFound();
        }

        var shorthand = loaded.stream()
                .filter(id -> id.substring(id.indexOf(':') + 1).equalsIgnoreCase(normalized))
                .toList();
        return shorthand.size() == 1
                ? WorldResolution.resolved(shorthand.getFirst())
                : shorthand.size() > 1
                        ? WorldResolution.ambiguous(shorthand)
                        : WorldResolution.notFound();
    }

}

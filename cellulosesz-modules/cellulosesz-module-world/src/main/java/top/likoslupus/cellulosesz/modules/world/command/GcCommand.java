package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;

import java.util.List;
import java.util.Map;

public final class GcCommand implements CellCommand {

    private static final long MEBIBYTE = 1024L * 1024L;
    private final WorldPlatformService worlds;

    public GcCommand(WorldPlatformService worlds) {
        this.worlds = worlds;
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public String permission() {
        return "cellulosesz.command.gc";
    }

    @Override
    public String name() {
        return "gc";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.world.gc.usage", Map.of("usage", usage()));
            return 0;
        }
        var result = worlds.diagnostics();
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        var snapshot = result.value().orElseThrow();
        invocation.replyKey("commands.world.gc.summary", Map.ofEntries(
                Map.entry("uptimeSeconds", snapshot.uptimeMillis() / 1000L),
                Map.entry("usedMb", snapshot.usedMemoryBytes() / MEBIBYTE),
                Map.entry("allocatedMb", snapshot.allocatedMemoryBytes() / MEBIBYTE),
                Map.entry("maximumMb", snapshot.maximumMemoryBytes() / MEBIBYTE),
                Map.entry("availableMb", snapshot.availableMemoryBytes() / MEBIBYTE),
                Map.entry("tps", round(snapshot.ticksPerSecond())),
                Map.entry("tickMillis", snapshot.averageTickMillis().isPresent()
                        ? round(snapshot.averageTickMillis().orElseThrow()) : "-")
        ));
        snapshot.worlds().forEach(world -> invocation.replyKey("commands.world.gc.world", Map.of(
                "world", world.worldId(),
                "chunks", world.loadedChunks(),
                "entities", world.entities(),
                "blockEntities", world.blockEntities().isPresent() ? world.blockEntities().orElseThrow() : "-"
        )));
        return 1;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

}

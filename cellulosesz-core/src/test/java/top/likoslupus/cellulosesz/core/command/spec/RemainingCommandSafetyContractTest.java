package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RemainingCommandSafetyContractTest {

    @Test
    void destructiveOperationsUseTypedServicesInsteadOfConsoleDispatch() throws IOException {
        var source = mainSource();
        assertFalse(source.contains("dispatchConsoleCommand(\"kill"));
        assertFalse(source.contains("dispatchConsoleCommand(\"experience"));
        assertFalse(source.contains("dispatchConsoleCommand(\"setblock"));
        assertFalse(source.contains("dispatchConsoleCommand(\"summon"));
        assertTrue(source.contains("PlayerStatePlatformService"));
        assertTrue(source.contains("WorldPlatformService"));
        assertTrue(source.contains("EntityPlatformService"));
    }

    private static String mainSource() throws IOException {
        var builder = new StringBuilder();
        try (var files = Files.walk(projectRoot())) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .forEach(path -> {
                        try {
                            builder.append(Files.readString(path)).append('\n');
                        } catch (IOException failure) {
                            throw new java.io.UncheckedIOException(failure);
                        }
                    });
        }
        return builder.toString();
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "Project root not found");
    }

    @Test
    void inventoryCommandsRetainTransactionAndConflictGuards() throws IOException {
        var clear = read("cellulosesz-modules/cellulosesz-module-item/src/main/java/top/likoslupus/cellulosesz/modules/item/command/ClearInventoryCommand.java");
        assertTrue(clear.contains("inventory.inventorySlots(target)"));
        assertTrue(clear.contains("prepareInventoryRemoval"));
        assertTrue(clear.contains("InventoryMutation::rollback"));
        assertTrue(clear.contains("targetUuids"));
        assertTrue(clear.contains("requestedAt"));
        var condense = read("cellulosesz-modules/cellulosesz-module-item/src/main/java/top/likoslupus/cellulosesz/modules/item/command/CondenseCommand.java");
        assertTrue(condense.contains("prepareInventoryExchange"));
        var inventory = read("cellulosesz-fabric/src/main/java/top/likoslupus/cellulosesz/fabric/FabricInventoryOperations.java");
        assertTrue(inventory.contains("firstInsertionSlot"));
        assertTrue(inventory.contains("Held item changed while profile resolved"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(projectRoot().resolve(relative));
    }

    @Test
    void indirectCommandDispatchHasSharedBoundedFinallyReleasedGuard() throws IOException {
        var guard = read("cellulosesz-fabric/src/main/java/top/likoslupus/cellulosesz/fabric/FabricPlayerCommandDispatchService.java");
        assertTrue(guard.contains("MAX_DEPTH"));
        assertTrue(guard.contains("MAX_INDIRECT_EXECUTIONS_PER_TICK"));
        assertTrue(guard.contains("containsCycle"));
        assertTrue(guard.contains("containsControl"));
        assertTrue(guard.contains("finally"));
        assertTrue(guard.contains("stack.pop()"));
        var sudo = read("cellulosesz-modules/cellulosesz-module-admin/src/main/java/top/likoslupus/cellulosesz/modules/admin/command/SudoCommand.java");
        assertTrue(sudo.contains("CommandDispatchOrigin.SUDO"));
        var powerTool = read("cellulosesz-modules/cellulosesz-module-item/src/main/java/top/likoslupus/cellulosesz/modules/item/service/DefaultItemAutomationService.java");
        assertTrue(powerTool.contains("CommandDispatchOrigin.POWER_TOOL"));
    }

    @Test
    void temporaryEntitiesHaveLifecycleCleanupAndBoundedConfiguration() throws IOException {
        var entities = read("cellulosesz-fabric/src/main/java/top/likoslupus/cellulosesz/fabric/FabricEntityOperations.java");
        assertTrue(entities.contains("expiresAt"));
        assertTrue(entities.contains("clearTrackedEntities"));
        assertTrue(entities.contains("entity.discard()"));
        var config = read("cellulosesz-modules/cellulosesz-module-world/src/main/java/top/likoslupus/cellulosesz/modules/world/config/WorldConfig.java");
        assertTrue(config.contains("maximumProjectileSpeed"));
        assertTrue(config.contains("projectileLifetimeTicks"));
        assertTrue(config.contains("spawnMobMaximumAmount"));
        assertTrue(config.contains("nukeTntPerTarget"));
        assertTrue(config.contains("validate()"));
    }

    @Test
    void reloadValidatesBeforePublishingMutableSnapshots() throws IOException {
        for (var module : new String[]{
                "cellulosesz-modules/cellulosesz-module-admin/src/main/java/top/likoslupus/cellulosesz/modules/admin/AdminModule.java",
                "cellulosesz-modules/cellulosesz-module-item/src/main/java/top/likoslupus/cellulosesz/modules/item/ItemModule.java",
                "cellulosesz-modules/cellulosesz-module-world/src/main/java/top/likoslupus/cellulosesz/modules/world/WorldModule.java"
        }) {
            var text = read(module);
            var validate = text.indexOf("candidate.validate()");
            var copy = text.indexOf("copyFrom(candidate)");
            assertTrue(validate >= 0 && copy > validate, module);
        }
    }

}

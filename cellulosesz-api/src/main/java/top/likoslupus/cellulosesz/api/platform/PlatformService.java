package top.likoslupus.cellulosesz.api.platform;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface PlatformService {

    default boolean supports(PlatformCapability capability) {
        return capabilities().contains(capability);
    }

    /**
     * Declares concrete platform operations available to modules.
     */
    default Set<PlatformCapability> capabilities() {
        return Set.of();
    }

    /**
     * Executes a platform mutation on the Minecraft server thread.
     */
    default void runOnServerThread(Runnable task) {
        task.run();
    }

    /**
     * Runs a server-thread task and completes after the task has actually executed.
     */
    default CompletableFuture<Void> runOnServerThreadAsync(Runnable task) {
        return callOnServerThread(() -> {
            task.run();
            return Boolean.TRUE;
        }).thenAccept(_ -> {
        });
    }

    /**
     * Evaluates a platform operation on the Minecraft server thread without blocking the caller.
     */
    default <T> CompletableFuture<T> callOnServerThread(Supplier<T> task) {
        try {
            return CompletableFuture.completedFuture(task.get());
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    Optional<CellPlayer> player(CommandInvocation invocation);

    Optional<CellPlayer> player(Object nativeHandle);

    Optional<CellPlayer> onlinePlayer(String name);

    List<CellPlayer> onlinePlayers();

    List<String> worlds();

    String defaultWorld();

    CellLocation location(CellPlayer player);

    default Optional<String> address(CellPlayer player) {
        return Optional.empty();
    }

    CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location);

    Optional<CellLocation> safeLocation(CellLocation location);

    Optional<CellLocation> highestLocation(
            String world,
            double x,
            double z
    );

    Optional<CellLocation> targetLocation(CellPlayer player, int maxDistance);

    default void sendMessage(CellPlayer player, String message) {
        sendMessage(player, RichText.plain(message));
    }

    default void sendMessage(CellPlayer player, RichText message) {
    }

    default String locale(CellPlayer player) {
        return "";
    }

    default void setDisplayName(CellPlayer player, RichText displayName) {
    }

    default void kick(CellPlayer player, String reason) {
    }

    default boolean setFlying(CellPlayer player, boolean enabled) {
        return false;
    }

    default boolean setInvulnerable(CellPlayer player, boolean enabled) {
        return false;
    }

    default boolean heal(CellPlayer player) {
        return false;
    }

    default boolean feed(CellPlayer player) {
        return false;
    }

    default boolean setTime(String world, long time) {
        return false;
    }

    default boolean setWeather(
            String world,
            String weather,
            int seconds
    ) {
        return false;
    }

    default boolean setGameMode(CellPlayer player, String gameMode) {
        return false;
    }

    default Optional<String> gameMode(CellPlayer player) {
        return Optional.empty();
    }

    default boolean flying(CellPlayer player) {
        return false;
    }

    default boolean setMovementSpeed(
            CellPlayer player,
            MovementSpeedType type,
            double speed
    ) {
        return false;
    }

    default boolean setPersonalTime(CellPlayer player, @Nullable Long time) {
        return false;
    }

    default boolean setPersonalWeather(CellPlayer player, @Nullable String weather) {
        return false;
    }

    default CompletableFuture<Path> backup(Path destinationDirectory) {
        return CompletableFuture.failedFuture(new IllegalStateException("Backups are not supported"));
    }

    default int removeEntities(
            String selector,
            CellPlayer origin,
            int radius
    ) {
        return -1;
    }

    default boolean giveItem(
            CellPlayer player,
            String itemArgument,
            int count
    ) {
        return false;
    }

    default int countItem(CellPlayer player, String itemId) {
        return 0;
    }

    default boolean takeItem(
            CellPlayer player,
            String itemId,
            int count
    ) {
        return false;
    }

    default Optional<String> heldItemId(CellPlayer player) {
        return Optional.empty();
    }

    default boolean validItem(String itemId) {
        return false;
    }

    default int maxStackSize(String itemId) {
        return 0;
    }

    default boolean setHeldItemName(CellPlayer player, @Nullable String name) {
        return false;
    }

    default boolean setHeldItemLore(CellPlayer player, List<String> lore) {
        return false;
    }

    default boolean setHeldItemComponent(
            CellPlayer player,
            String componentId,
            String rawValue
    ) {
        return false;
    }

    default boolean clearHeldItemComponent(CellPlayer player, String componentId) {
        return false;
    }

    default boolean openWorkstation(CellPlayer player, String workstation) {
        return false;
    }

    default List<ItemDescriptor> inventoryItems(CellPlayer player) {
        return List.of();
    }

    /**
     * Returns a lossless snapshot of every non-empty player inventory slot.
     */
    default Optional<List<InventoryItemSnapshot>> inventorySnapshot(CellPlayer player) {
        return Optional.empty();
    }

    default Optional<InventoryItemSnapshot> heldInventorySnapshot(CellPlayer player) {
        return Optional.empty();
    }

    /**
     * Decodes a lossless snapshot for user-facing descriptions and validation.
     */
    default Optional<ItemDescriptor> describeInventoryItem(InventoryItemSnapshot snapshot) {
        return Optional.empty();
    }

    /**
     * Returns true only when the stack has no non-default data components.
     */
    default boolean plainInventoryItem(InventoryItemSnapshot snapshot) {
        return false;
    }

    /**
     * Prepares an exact-slot, all-or-nothing inventory grant without changing the inventory. An empty result means the
     * payload is invalid or one of the required slots is unavailable.
     */
    default Optional<InventoryGrant> prepareInventoryGrant(
            CellPlayer player,
            List<? extends InventoryItemSnapshot> snapshots
    ) {
        return Optional.empty();
    }

    /**
     * Prepares exact-slot removal of the selected stack quantities.
     */
    default Optional<InventoryMutation> prepareInventoryRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    ) {
        return Optional.empty();
    }

    /**
     * Prepares one atomic inventory exchange using complete item descriptors.
     */
    default Optional<InventoryMutation> prepareInventoryExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    ) {
        return Optional.empty();
    }

    default void sendChatMessage(CellPlayer player, String message) {
    }

    default boolean enchantHeldItem(
            CellPlayer player,
            String enchantment,
            int level
    ) {
        return false;
    }

    default int repairItems(CellPlayer player, boolean all) {
        return 0;
    }

    default boolean validEntityType(String entityType) {
        return false;
    }

    default int spawnMob(
            CellPlayer player,
            String entityType,
            int count
    ) {
        return 0;
    }

    default boolean openInventory(
            CellPlayer viewer,
            CellPlayer target
    ) {
        return false;
    }

    default boolean openEnderChest(
            CellPlayer viewer,
            CellPlayer target
    ) {
        return false;
    }

    default void maintainItemCount(
            CellPlayer player,
            String itemId,
            int minimum
    ) {
    }

    default void setPlayerVisible(
            CellPlayer viewer,
            CellPlayer target,
            boolean visible
    ) {
    }

    default void setVanishedState(CellPlayer player, boolean vanished) {
    }

    default void refreshCommandTree() {
    }

    default boolean replaceSignText(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> expectedLines,
            List<String> replacementLines
    ) {
        return false;
    }

    default boolean breakSignBlock(
            CellPlayer player,
            CellLocation location,
            List<String> expectedFrontLines,
            List<String> expectedBackLines
    ) {
        return false;
    }

}

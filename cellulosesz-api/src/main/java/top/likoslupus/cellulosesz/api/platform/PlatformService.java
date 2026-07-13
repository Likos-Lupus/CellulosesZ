package top.likoslupus.cellulosesz.api.platform;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PlatformService {

    Optional<CellPlayer> player(CommandInvocation invocation);

    Optional<CellPlayer> player(Object nativeHandle);

    Optional<CellPlayer> onlinePlayer(String name);

    List<CellPlayer> onlinePlayers();

    List<String> worlds();

    String defaultWorld();

    CellLocation location(CellPlayer player);

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

    default List<ItemDescriptor> inventoryItems(CellPlayer player) {
        return List.of();
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

    default boolean dispatchPlayerCommand(
            CellPlayer player,
            String command
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

    default boolean dispatchConsoleCommand(String command) {
        return false;
    }

    /**
     * Executes a command against the platform command tree captured before CellulosesZ replaces roots with the same
     * labels. Use this only for internal calls that must reach the native platform implementation rather than a
     * CellulosesZ command with the same name.
     */
    default boolean dispatchNativeConsoleCommand(String command) {
        return false;
    }

}

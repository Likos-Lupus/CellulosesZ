package top.likoslupus.cellulosesz.common.item;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemGrantResult;
import top.likoslupus.cellulosesz.api.item.RepairScope;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayerUnavailableException;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Ordinary item-stack operations backed by Minecraft data-component codecs.
 */
public final class MinecraftItemOperations implements ItemPlatformService {

    private static final Gson GSON = new Gson();
    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion",
            "minecraft:splash_potion",
            "minecraft:lingering_potion",
            "minecraft:tipped_arrow"
    );

    private final MinecraftServerHandle server;
    private final MinecraftInventoryStore inventory;
    private final CellulosesZLogger logger;

    public MinecraftItemOperations(
            MinecraftServerHandle server,
            CellulosesZLogger logger
    ) {
        this.server = requireNonNull(server, "server");
        this.inventory = new MinecraftInventoryStore(server);
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public PlatformResult<Void> registryStatus() {
        try {
            server.requireRunning();
        } catch (IllegalStateException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_READY,
                    failure.getMessage() == null
                            ? "Server is not ready"
                            : failure.getMessage()
            );
        }
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Registry access requires the server thread"
            );
        }
        return PlatformResult.success();
    }

    @Override
    public PlatformResult<ItemDescriptor> parse(String input) {
        return onServerThread(() -> inventory.parseDescriptor(input)
                .map(PlatformResult::success)
                .orElseGet(() -> PlatformResult.failure(
                        PlatformOperationStatus.INVALID_INPUT,
                        "Invalid item input"
                ))
        );
    }

    @Override
    public Set<String> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> enchantmentIds() {
        var registry = server.requireRunning()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        return registry.keySet().stream()
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> potionEffectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream()
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public PlatformResult<Boolean> validItem(String itemId) {
        return onServerThread(() -> PlatformResult.success(
                inventory.parseItem(normalize(itemId)).isPresent()
        ));
    }

    @Override
    public PlatformResult<Integer> maxStackSize(String itemId) {
        return onServerThread(() -> inventory.parseItem(normalize(itemId))
                .map(stack -> PlatformResult.success(stack.getMaxStackSize()))
                .orElseGet(() -> PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Unknown item: " + itemId
                ))
        );
    }

    @Override
    public PlatformResult<ItemGrantResult> grant(CellPlayer player, ItemDescriptor descriptor) {
        return onServerThread(() -> {
            var argument = descriptor.normalizedArgument();
            var prepared = inventory.prepareExchange(
                    player,
                    List.of(),
                    List.of(new InventoryItemRequest(
                            argument,
                            descriptor.count()
                    ))
            );

            if (!prepared.successful()) {
                return PlatformResult.failure(prepared.status(), prepared.detail());
            }

            var committed = prepared.value().commit();
            if (!committed.successful()) {
                return PlatformResult.failure(
                        committed.status(),
                        committed.detail()
                );
            }

            return PlatformResult.success(new ItemGrantResult(
                    descriptor.count(),
                    descriptor.count()
            ));
        });
    }

    @Override
    public PlatformResult<Integer> count(CellPlayer player, ItemDescriptor descriptor) {
        return onServerThread(() -> {
            var parsed = inventory.parseItem(descriptor.normalizedArgument());

            if (parsed.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid item"
                );
            }

            var template = parsed.orElseThrow();
            var nativeInventory = MinecraftPlayers.requireOnline(server, player).getInventory();
            var count = IntStream.range(0, nativeInventory.getContainerSize())
                    .mapToObj(nativeInventory::getItem)
                    .filter(stack -> !stack.isEmpty()
                            && ItemStack.isSameItemSameComponents(stack, template)
                    )
                    .mapToInt(ItemStack::getCount)
                    .sum();

            return PlatformResult.success(count);
        });
    }

    @Override
    public PlatformResult<Void> take(CellPlayer player, ItemDescriptor descriptor) {
        return onServerThread(() -> {
            var prepared = inventory.prepareExchange(
                    player,
                    List.of(new InventoryItemRequest(
                            descriptor.normalizedArgument(),
                            descriptor.count()
                    )),
                    List.of()
            );

            if (!prepared.successful()) {
                return PlatformResult.failure(prepared.status(), prepared.detail());
            }

            return prepared.value().commit();
        });
    }

    @Override
    public PlatformResult<String> heldItemId(CellPlayer player) {
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            return held.isEmpty()
                    ?
                    PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Main hand is empty"
                    )
                    : PlatformResult.success(MinecraftItems.id(held));
        });
    }

    @Override
    public PlatformResult<Void> setHeldName(CellPlayer player, Optional<RichText> name) {
        requireNonNull(name, "name");
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            if (name.isPresent()) {
                held.set(
                        DataComponents.CUSTOM_NAME,
                        MinecraftTextAdapter.toComponent(name.orElseThrow(), logger)
                );
            } else {
                held.remove(DataComponents.CUSTOM_NAME);
            }

            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Void> setHeldLore(CellPlayer player, List<RichText> lore) {
        requireNonNull(lore, "lore");
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            if (lore.isEmpty()) {
                held.remove(DataComponents.LORE);
            } else {
                held.set(
                        DataComponents.LORE,
                        new ItemLore(lore.stream()
                                .map(value -> MinecraftTextAdapter.toComponent(value, logger))
                                .toList()
                        )
                );
            }

            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Void> enchant(
            CellPlayer player,
            String enchantmentId,
            int level,
            boolean unsafe
    ) {
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            var location = Identifier.tryParse(normalize(enchantmentId));
            if (location == null) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid enchantment"
                );
            }

            var registry = server.requireRunning()
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT);
            var enchantment = registry.getValue(location);
            if (enchantment == null) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Unknown enchantment"
                );
            }

            var holder = registry.wrapAsHolder(enchantment);
            if (!unsafe && !holder.value().canEnchant(held)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Enchantment is incompatible with held item"
                );
            }

            var maximum = holder.value().getMaxLevel();
            if (!unsafe && level > maximum) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Enchantment level exceeds maximum"
                );
            }

            var mutable = new ItemEnchantments.Mutable(held.getOrDefault(
                    DataComponents.ENCHANTMENTS,
                    ItemEnchantments.EMPTY
            ));

            mutable.set(holder, level);
            held.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Integer> repair(CellPlayer player, RepairScope scope) {
        requireNonNull(scope, "scope");
        return onServerThread(() -> {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            if (scope == RepairScope.HAND) {
                return PlatformResult.success(repair(nativePlayer.getMainHandItem())
                        ? 1
                        : 0
                );
            }

            var nativeInventory = nativePlayer.getInventory();
            var repaired = (int) IntStream.range(0, nativeInventory.getContainerSize())
                    .filter(slot -> repair(nativeInventory.getItem(slot)))
                    .count();

            nativeInventory.setChanged();
            return PlatformResult.success(repaired);
        });
    }

    @Override
    public PlatformResult<Void> applyPotion(CellPlayer player, PotionItemRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            if (held.isEmpty()
                    || !POTION_ITEMS.contains(MinecraftItems.id(held))
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Held item does not support potion contents"
                );
            }

            if (request.effectId().isEmpty()) {
                held.remove(DataComponents.POTION_CONTENTS);
                return PlatformResult.success();
            }

            var effect = normalize(request.effectId().orElseThrow());
            if (!potionEffectIds().contains(effect)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Unknown effect"
                );
            }

            var raw = "{custom_effects:[{id:\"%s\",amplifier:%d,duration:%d}]}".formatted(
                    effect,
                    request.amplifier(),
                    Math.multiplyExact(request.durationSeconds(), 20)
            );
            return copyTypedComponent(
                    held,
                    "minecraft:potion_contents",
                    raw
            );
        });
    }

    @Override
    public PlatformResult<Void> applyFirework(CellPlayer player, FireworkItemRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var held = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            var itemId = held.isEmpty()
                    ? ""
                    : MinecraftItems.id(held);
            var componentId = itemId.equals("minecraft:firework_rocket")
                    ? "minecraft:fireworks"
                    : itemId.equals("minecraft:firework_star")
                            ? "minecraft:firework_explosion"
                            : "";

            if (componentId.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Held item does not support firework data"
                );
            }

            if (request.operation() == FireworkItemRequest.Operation.CLEAR) {
                componentType(componentId).ifPresent(held::remove);
                return PlatformResult.success();
            }

            if (request.operation() == FireworkItemRequest.Operation.POWER) {
                if (!itemId.equals("minecraft:firework_rocket")) {
                    return PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Flight power requires a firework rocket"
                    );
                }

                return copyTypedComponent(
                        held,
                        componentId,
                        "{flight_duration:" + request.power() + ",explosions:[]}"
                );
            }

            if (!itemId.equals("minecraft:firework_star")) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Explosion data requires a firework star"
                );
            }

            var payload = new FireworkExplosionPayload(
                    request.shape().orElseThrow().name().toLowerCase(Locale.ROOT),
                    request.colors(),
                    request.fadeColors(),
                    request.trail(),
                    request.flicker()
            );
            return copyTypedComponent(held, componentId, GSON.toJson(payload));
        });
    }

    @Override
    public PlatformResult<Void> maintainCount(CellPlayer player, String itemId, int minimum) {
        return onServerThread(() -> {
            var descriptor = new ItemDescriptor(normalize(itemId), 1);
            var current = count(player, descriptor);
            var currentCount = current.value();
            if (!current.successful()
                    || currentCount == null
            ) {
                return PlatformResult.failure(current.status(), current.detail());
            }

            var missing = Math.max(0, minimum - currentCount);
            if (missing == 0) {
                return PlatformResult.success();
            }

            var result = grant(
                    player,
                    new ItemDescriptor(normalize(itemId), missing)
            );

            return result.successful()
                    ? PlatformResult.success()
                    : PlatformResult.failure(result.status(), result.detail());
        });
    }

    private PlatformResult<Void> copyTypedComponent(
            ItemStack target,
            String componentId,
            String rawValue
    ) {
        try {
            var argument = MinecraftItems.id(target) + "[" + componentId + "=" + rawValue + "]";
            var reader = new StringReader(argument);
            var parsed = new ItemParser(server.requireRunning().registryAccess()).parse(reader);

            if (reader.canRead()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid component data"
                );
            }

            var source = parsed.createItemStack(1);
            var component = componentType(componentId);
            if (component.isEmpty()
                    || !copyComponent(source, target, component.orElseThrow())
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Component codec rejected data"
                );
            }

            return PlatformResult.success();
        } catch (RuntimeException | CommandSyntaxException exception) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private Optional<DataComponentType<?>> componentType(String id) {
        var location = Identifier.tryParse(normalize(id));
        return location == null
                ? Optional.empty()
                : Optional.ofNullable(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(location));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean copyComponent(
            ItemStack source,
            ItemStack target,
            DataComponentType<?> component
    ) {
        var value = source.get((DataComponentType) component);
        if (value == null) {
            return false;
        }

        target.set((DataComponentType) component, value);
        return true;
    }

    private static boolean repair(ItemStack stack) {
        if (stack.isEmpty()
                || !stack.isDamageableItem()
                || stack.getDamageValue() <= 0
        ) {
            return false;
        }

        stack.setDamageValue(0);
        return true;
    }

    private static String normalize(String value) {
        var normalized = requireNonNull(value, "value").strip().toLowerCase(Locale.ROOT);
        return normalized.contains(":")
                ? normalized
                : "minecraft:" + normalized;
    }

    private <T> PlatformResult<T> onServerThread(
            Supplier<PlatformResult<T>> operation
    ) {
        try {
            server.requireRunning();
        } catch (IllegalStateException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_READY,
                    failure.getMessage() == null
                            ? "Server is not ready"
                            : failure.getMessage()
            );
        }
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (IllegalArgumentException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    failure.getMessage() == null
                            ? "Invalid item operation"
                            : failure.getMessage()
            );
        } catch (MinecraftPlayerUnavailableException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private record FireworkExplosionPayload(
            String shape,
            List<Integer> colors,
            @SerializedName("fade_colors") List<Integer> fadeColors,
            @SerializedName("has_trail") boolean trail,
            @SerializedName("has_twinkle") boolean twinkle
    ) {

        private FireworkExplosionPayload {
            colors = List.copyOf(colors);
            fadeColors = List.copyOf(fadeColors);
        }

    }

}

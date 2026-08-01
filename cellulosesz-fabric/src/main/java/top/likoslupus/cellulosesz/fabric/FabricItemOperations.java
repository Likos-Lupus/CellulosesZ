package top.likoslupus.cellulosesz.fabric;

import com.google.gson.Gson;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Ordinary item-stack operations backed by Minecraft data-component codecs.
 */
public final class FabricItemOperations implements ItemPlatformService {

    private static final Gson GSON = new Gson();
    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion",
            "minecraft:splash_potion",
            "minecraft:lingering_potion",
            "minecraft:tipped_arrow"
    );

    private final FabricServerAccess access;
    private final FabricInventoryStore inventory;
    private final CellulosesZLogger logger;

    public FabricItemOperations(
            FabricServerAccess access,
            CellulosesZLogger logger
    ) {
        this.access = requireNonNull(access, "access");
        this.inventory = new FabricInventoryStore(access);
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public Set<String> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> enchantmentIds() {
        var registry = access
                .requireServer()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        return registry.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> potionEffectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean validItem(String itemId) {
        return inventory.parseItem(normalize(itemId)).isPresent();
    }

    @Override
    public int maxStackSize(String itemId) {
        return inventory.parseItem(normalize(itemId))
                .map(input ->
                        input.createItemStack(1, false).getMaxStackSize()
                )
                .orElse(0);
    }

    @Override
    public PlatformResult<ItemGrantResult> grant(CellPlayer player, ItemDescriptor descriptor) {
        return onServerThread(() -> {
            var argument = commandArgument(descriptor);
            var prepared = inventory.prepareExchange(
                    player,
                    List.of(),
                    List.of(new InventoryItemRequest(
                            argument,
                            descriptor.count
                    ))
            );

            if (prepared.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Inventory has insufficient space"
                );
            }

            if (!prepared.orElseThrow().commit()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory changed before grant"
                );
            }

            return PlatformResult.success(new ItemGrantResult(
                    descriptor.count,
                    descriptor.count
            ));
        });
    }

    @Override
    public PlatformResult<Integer> count(CellPlayer player, ItemDescriptor descriptor) {
        return onServerThread(() -> {
            var parsed = inventory.parseItem(commandArgument(descriptor));

            if (parsed.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid item"
                );
            }

            var template = parsed.orElseThrow().createItemStack(1, false);
            var nativeInventory = access.player(player).getInventory();
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
                            commandArgument(descriptor),
                            descriptor.count
                    )),
                    List.of()
            );

            if (prepared.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Required item stack is unavailable"
                );
            }

            return prepared.orElseThrow().commit()
                    ? PlatformResult.success()
                    : PlatformResult.failure(
                            PlatformOperationStatus.CONFLICT,
                            "Inventory changed before removal"
                    );
        });
    }

    @Override
    public PlatformResult<String> heldItemId(CellPlayer player) {
        return onServerThread(() -> {
            var held = access.player(player).getMainHandItem();
            return held.isEmpty()
                    ?
                    PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Main hand is empty"
                    )
                    : PlatformResult.success(access.itemId(held));
        });
    }

    @Override
    public PlatformResult<Void> setHeldName(CellPlayer player, Optional<RichText> name) {
        requireNonNull(name, "name");
        return onServerThread(() -> {
            var held = access.player(player).getMainHandItem();
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
            var held = access.player(player).getMainHandItem();
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
            var held = access.player(player).getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            var location = ResourceLocation.tryParse(normalize(enchantmentId));
            if (location == null) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid enchantment"
                );
            }

            var registry = access
                    .requireServer()
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
            var nativePlayer = access.player(player);
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
            var held = access.player(player).getMainHandItem();
            if (held.isEmpty()
                    || !POTION_ITEMS.contains(access.itemId(held))
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
            var held = access.player(player).getMainHandItem();
            var itemId = held.isEmpty()
                    ? ""
                    : access.itemId(held);
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

            var payload = new LinkedHashMap<String, Object>();
            payload.put("shape", request.shape().orElseThrow().name().toLowerCase(Locale.ROOT));
            payload.put("colors", request.colors());
            payload.put("fade_colors", request.fadeColors());
            payload.put("has_trail", request.trail());
            payload.put("has_twinkle", request.flicker());
            return copyTypedComponent(held, componentId, GSON.toJson(payload));
        });
    }

    @Override
    public PlatformResult<Void> maintainCount(CellPlayer player, String itemId, int minimum) {
        return onServerThread(() -> {
            var descriptor = new ItemDescriptor(normalize(itemId), 1);
            var current = count(player, descriptor);
            if (!current.successful()
                    || current.value().isEmpty()
            ) {
                return PlatformResult.failure(current.status(), current.detail());
            }

            var missing = Math.max(0, minimum - current.value().orElseThrow());
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
            var argument = access.itemId(target) + "[" + componentId + "=" + rawValue + "]";
            var reader = new StringReader(argument);
            var parsed = ItemParser.parseForItem(access.requireServer().registryAccess(), reader);

            if (reader.canRead()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Invalid component data"
                );
            }

            var source = parsed.createItemStack(1, false);
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
        var location = ResourceLocation.tryParse(normalize(id));
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

    private <T> PlatformResult<T> onServerThread(
            Supplier<PlatformResult<T>> operation
    ) {
        if (!access.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private String commandArgument(ItemDescriptor descriptor) {
        var id = descriptor.normalizedItem();
        var components = descriptor.normalizedComponents();
        if (components.isEmpty()) {
            return id;
        }

        var parts = new ArrayList<String>();
        components.forEach((key, value) -> parts.add(key + "=" + serialize(value)));
        return id + "[" + String.join(",", parts) + "]";
    }

    private static String serialize(Object value) {
        if (value instanceof RawItemComponent(String value1)) {
            return value1;
        }

        return GSON.toJson(value);
    }

    private static String normalize(String value) {
        var normalized = requireNonNull(value, "value").strip().toLowerCase(Locale.ROOT);
        return normalized.contains(":")
                ? normalized
                : "minecraft:" + normalized;
    }

}

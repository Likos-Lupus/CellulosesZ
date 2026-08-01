package top.likoslupus.cellulosesz.modules.item.application;

import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Item application operations shared by commands and functional signs.
 */
public final class ItemCommandService {

    private final ItemService items;
    private final ItemPlatformService platform;

    public ItemCommandService(
            ItemService items,
            ItemPlatformService platform
    ) {
        this.items = requireNonNull(items, "items");
        this.platform = requireNonNull(platform, "platform");
    }

    public PlatformResult<?> grant(
            CellPlayer player,
            ItemDescriptor descriptor,
            boolean allowBlacklist,
            boolean allowOversized
    ) {
        requireNonNull(player, "player");
        descriptor = requireNonNull(descriptor, "descriptor").copy();

        if (!items.valid(descriptor)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "invalid-item"
            );
        }

        if (items.blacklisted(descriptor) && !allowBlacklist) {
            return PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "blacklisted-item"
            );
        }

        if (descriptor.count > items.maxStackSize(descriptor) && !allowOversized) {
            return PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "oversized-stack"
            );
        }

        return platform.grant(player, descriptor);
    }

    public PlatformResult<?> enchant(
            CellPlayer player,
            String enchantmentId,
            int level,
            boolean unsafe
    ) {
        return platform.enchant(player, enchantmentId, level, unsafe);
    }

    public PlatformResult<?> repair(CellPlayer player, RepairScope scope) {
        return platform.repair(player, scope);
    }

    public PlatformResult<?> setName(CellPlayer player, Optional<RichText> name) {
        return platform.setHeldName(player, name);
    }

    public PlatformResult<?> setLore(CellPlayer player, List<RichText> lore) {
        return platform.setHeldLore(player, List.copyOf(lore));
    }

    public PlatformResult<?> potion(CellPlayer player, PotionItemRequest request) {
        return platform.applyPotion(player, request);
    }

    public PlatformResult<?> firework(CellPlayer player, FireworkItemRequest request) {
        return platform.applyFirework(player, request);
    }

    public ItemPlatformService platform() {
        return platform;
    }

}

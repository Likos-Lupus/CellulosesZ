package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemGrantResult;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.item.ItemPlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.*;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultItemService implements ItemService {

    private final ItemPlatformService platform;
    private volatile @Nullable ItemConfig config;
    private volatile Map<String, String> aliases = Map.of();
    private volatile Map<String, ItemDescriptor> customItems = Map.of();
    private volatile Set<String> blacklist = Set.of();
    private volatile boolean registryValidated;

    public DefaultItemService(
            ItemPlatformService platform,
            ItemConfig config
    ) {
        this.platform = requireNonNull(platform, "platform");
        configure(config);
    }

    public void configure(ItemConfig config) {
        configure(prepareConfiguration(config));
    }

    public void configure(PreparedConfiguration prepared) {
        var configuration = requireNonNull(prepared, "prepared");
        this.config = configuration.config();
        this.aliases = configuration.aliases();
        this.customItems = configuration.customItems();
        this.blacklist = configuration.blacklist();
        this.registryValidated = platform.registryStatus().successful();
    }

    public PreparedConfiguration prepareConfiguration(ItemConfig config) {
        var snapshot = new ItemConfig();
        snapshot.copyFrom(requireNonNull(config, "config"));
        snapshot.validate();

        requirePositive(snapshot.maxCommandCount, "maxCommandCount");
        requirePositive(snapshot.maxLoreLines, "maxLoreLines");

        var aliasCopy = new LinkedHashMap<String, String>();
        requireNonNull(snapshot.aliases, "aliases").forEach((alias, argument) -> {
            var key = key(alias);
            var value = requireNonNull(argument, "alias argument").trim();

            if (key.isBlank() || value.isBlank()) {
                throw new IllegalArgumentException("Invalid item alias: " + alias);
            }

            if (platform.registryStatus().successful() && !platform.parse(value).successful()) {
                throw new IllegalArgumentException("Invalid item alias target: " + alias);
            }

            if (aliasCopy.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate item alias: " + alias);
            }
        });

        var customCopy = new LinkedHashMap<String, ItemDescriptor>();
        requireNonNull(snapshot.customItems, "customItems").forEach((name, configured) -> {
            var key = key(name);
            requireNonNull(configured, "custom item");
            var descriptor = new ItemDescriptor(
                    requireNonNull(configured.item, "custom item id"),
                    configured.count,
                    configured.argument == null
                            ? configured.item
                            : configured.argument
            );

            if (key.isBlank() || !validDescriptorShape(descriptor, snapshot.maxCommandCount)) {
                throw new IllegalArgumentException("Invalid custom item: " + name);
            }

            if (platform.registryStatus().successful()) {
                var parsed = platform.parse(descriptor.normalizedArgument());
                var parsedValue = parsed.value();
                if (!parsed.successful() || parsedValue == null) {
                    throw new IllegalArgumentException("Invalid custom item: " + name);
                }

                descriptor = new ItemDescriptor(
                        parsedValue.normalizedItem(),
                        descriptor.count(),
                        parsedValue.normalizedArgument()
                );
            }

            if (customCopy.put(key, descriptor) != null) {
                throw new IllegalArgumentException("Duplicate custom item: " + name);
            }
        });

        var blacklistCopy = new LinkedHashSet<String>();
        requireNonNull(snapshot.blacklist, "blacklist").forEach(item -> {
            var normalized = normalizeId(item);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Invalid blacklisted item: " + item);
            }

            blacklistCopy.add(normalized);
        });

        return new PreparedConfiguration(
                snapshot,
                Map.copyOf(aliasCopy),
                Map.copyOf(customCopy),
                Set.copyOf(blacklistCopy)
        );
    }

    private static String key(String value) {
        return requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validDescriptorShape(ItemDescriptor item, int maxCommandCount) {
        if (item.count() <= 0
                || item.count() > Math.max(1, maxCommandCount)
        ) {
            return false;
        }

        return !item.normalizedItem().isBlank() && !item.normalizedArgument().isBlank();
    }

    private static String normalizeId(String value) {
        var normalized = key(value);
        return normalized.indexOf(':') < 0
                ? "minecraft:" + normalized
                : normalized;
    }

    /** Validates startup-staged item inputs at the first registry-aware command boundary. */
    public synchronized void validateRegistryInputs() {
        if (registryValidated || !platform.registryStatus().successful()) {
            return;
        }

        configure(prepareConfiguration(config));
    }

    @Override
    public Set<String> itemNames() {
        var names = new LinkedHashSet<>(platform.itemIds());
        names.addAll(aliases.keySet());
        names.addAll(customItems.keySet());
        return Set.copyOf(names);
    }

    @Override
    public PlatformResult<ItemDescriptor> parse(String input) {
        if (input.isBlank()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "Item input must not be blank"
            );
        }

        var value = input.trim();
        var tokenEnd = aliasEnd(value);
        var token = value.substring(0, tokenEnd);
        var custom = customItems.get(key(token));

        if (custom != null) {
            var tail = value.substring(tokenEnd).trim();
            var requested = custom.count();
            if (!tail.isEmpty()) {
                try {
                    requested = Integer.parseInt(tail);
                } catch (NumberFormatException failure) {
                    return PlatformResult.failure(
                            PlatformOperationStatus.INVALID_INPUT,
                            "Invalid item count: " + tail
                    );
                }
            }

            try {
                return PlatformResult.success(new ItemDescriptor(
                        custom.normalizedItem(),
                        requested,
                        custom.normalizedArgument()
                ));
            } catch (IllegalArgumentException failure) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_INPUT,
                        failure.getMessage() == null
                                ? "Invalid item input"
                                : failure.getMessage()
                );
            }
        }

        var alias = aliases.get(key(token));
        if (alias != null) {
            value = alias + value.substring(tokenEnd);
        }
        return platform.parse(value);
    }

    private static int aliasEnd(String input) {
        for (var index = 0; index < input.length(); index++) {
            var current = input.charAt(index);
            if (Character.isWhitespace(current) || current == '[') {
                return index;
            }
        }

        return input.length();
    }

    @Override
    public PlatformResult<Boolean> valid(ItemDescriptor item) {
        requireNonNull(item, "item");
        if (!validDescriptorShape(item)) {
            return PlatformResult.success(false);
        }

        var parsed = platform.parse(item.normalizedArgument());
        if (!parsed.successful() || parsed.value() == null) {
            return PlatformResult.failure(parsed.status(), parsed.detail());
        }

        return PlatformResult.success(
                parsed.value().normalizedItem().equals(item.normalizedItem())
        );
    }

    private boolean validDescriptorShape(ItemDescriptor item) {
        return validDescriptorShape(item, config.maxCommandCount);
    }

    @Override
    public boolean blacklisted(ItemDescriptor item) {
        return blacklist.contains(requireNonNull(item, "item").normalizedItem());
    }

    @Override
    public PlatformResult<Integer> maxStackSize(ItemDescriptor item) {
        requireNonNull(item, "item");
        return platform.maxStackSize(item.normalizedItem());
    }

    @Override
    public PlatformResult<ItemGrantResult> give(
            CellPlayer player,
            ItemDescriptor item
    ) {
        requireNonNull(item, "item");
        if (item.count() > config.maxCommandCount) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Item count exceeds configured command maximum"
            );
        }

        return platform.grant(player, item);
    }

    @Override
    public PlatformResult<Integer> count(CellPlayer player, ItemDescriptor item) {
        return platform.count(player, requireNonNull(item, "item"));
    }

    @Override
    public PlatformResult<Void> take(CellPlayer player, ItemDescriptor item) {
        return platform.take(player, requireNonNull(item, "item"));
    }

    @Override
    public PlatformResult<String> heldItemId(CellPlayer player) {
        return platform.heldItemId(player);
    }

    public record PreparedConfiguration(
            ItemConfig config,
            Map<String, String> aliases,
            Map<String, ItemDescriptor> customItems,
            Set<String> blacklist
    ) {

        public PreparedConfiguration {
            requireNonNull(config, "config");
            aliases = Map.copyOf(requireNonNull(aliases, "aliases"));
            customItems = Map.copyOf(requireNonNull(customItems, "customItems"));
            blacklist = Set.copyOf(requireNonNull(blacklist, "blacklist"));
        }

    }

}

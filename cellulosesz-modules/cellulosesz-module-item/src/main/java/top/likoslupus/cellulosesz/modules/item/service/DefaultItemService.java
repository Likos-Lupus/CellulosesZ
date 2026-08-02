package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.*;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultItemService implements ItemService {

    private final ItemPlatformService platform;
    private volatile ItemConfig config;
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
        this.config = configuration.config;
        this.aliases = configuration.aliases;
        this.customItems = configuration.customItems;
        this.blacklist = configuration.blacklist;
        this.registryValidated = platform.registryReady();
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

            if (platform.registryReady() && platform.parse(value).isEmpty()) {
                throw new IllegalArgumentException("Invalid item alias target: " + alias);
            }

            if (aliasCopy.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate item alias: " + alias);
            }
        });

        var customCopy = new LinkedHashMap<String, ItemDescriptor>();
        requireNonNull(snapshot.customItems, "customItems").forEach((name, item) -> {
            var key = key(name);
            requireNonNull(item, "custom item");

            var copy = item.copy();
            if (key.isBlank() || !validDescriptorShape(copy, snapshot.maxCommandCount)) {
                throw new IllegalArgumentException("Invalid custom item: " + name);
            }

            if (platform.registryReady()) {
                var parsed = platform.parse(copy.normalizedArgument());
                if (parsed.isEmpty()) {
                    throw new IllegalArgumentException("Invalid custom item: " + name);
                }

                copy = new ItemDescriptor(
                        parsed.orElseThrow().normalizedItem(),
                        copy.count,
                        parsed.orElseThrow().normalizedArgument()
                );
            }

            if (customCopy.put(key, copy) != null) {
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
        if (item.count <= 0
                || item.count > Math.max(1, maxCommandCount)
        ) {
            return false;
        }

        try {
            return !item.normalizedItem().isBlank() && !item.normalizedArgument().isBlank();
        } catch (RuntimeException _) {
            return false;
        }
    }

    private static String normalizeId(String value) {
        var normalized = key(value);
        return normalized.indexOf(':') < 0
                ? "minecraft:" + normalized
                : normalized;
    }

    /** Validates startup-staged item inputs at the first registry-aware command boundary. */
    public synchronized void validateRegistryInputs() {
        if (registryValidated || !platform.registryReady()) {
            return;
        }
        configure(prepareConfiguration(config));
    }

    @Override
    public Optional<ItemDescriptor> parse(String input) {
        if (input.isBlank()) {
            return Optional.empty();
        }

        var value = input.trim();
        var tokenEnd = aliasEnd(value);
        var token = value.substring(0, tokenEnd);
        var custom = customItems.get(key(token));
        if (custom != null) {
            var tail = value.substring(tokenEnd).trim();
            var requested = custom.count;

            if (!tail.isEmpty()) {
                try {
                    requested = Integer.parseInt(tail);
                } catch (NumberFormatException _) {
                    return Optional.empty();
                }
            }

            var result = new ItemDescriptor(
                    custom.normalizedItem(),
                    requested,
                    custom.normalizedArgument()
            );

            return validDescriptorShape(result)
                    ? Optional.of(result)
                    : Optional.empty();
        }

        var alias = aliases.get(key(token));
        if (alias != null) {
            value = alias + value.substring(tokenEnd);
        }

        return platform.parse(value)
                .filter(this::validDescriptorShape)
                .map(ItemDescriptor::copy);
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

    private boolean validDescriptorShape(ItemDescriptor item) {
        return validDescriptorShape(item, config.maxCommandCount);
    }

    @Override
    public boolean valid(ItemDescriptor item) {
        if (!validDescriptorShape(item)) {
            return false;
        }

        var parsed = platform.parse(item.normalizedArgument());
        return parsed.isPresent()
                && parsed.orElseThrow().normalizedItem().equals(item.normalizedItem());
    }

    @Override
    public boolean blacklisted(ItemDescriptor item) {
        return blacklist.contains(item.normalizedItem());
    }

    @Override
    public int maxStackSize(ItemDescriptor item) {
        return Math.max(1, platform.maxStackSize(item.normalizedItem()));
    }

    @Override
    public boolean give(CellPlayer player, ItemDescriptor item) {
        return valid(item)
                && item.count <= config.maxCommandCount
                && platform.grant(player, item.copy()).successful();
    }

    @Override
    public int count(CellPlayer player, ItemDescriptor item) {
        if (!valid(item)) {
            return 0;
        }

        var result = platform.count(player, item.copy());
        return result.successful() && result.value().isPresent()
                ? result.value().orElseThrow()
                : 0;
    }

    @Override
    public boolean take(CellPlayer player, ItemDescriptor item) {
        return valid(item) && platform.take(player, item.copy()).successful();
    }

    @Override
    public Optional<String> heldItemId(CellPlayer player) {
        var result = platform.heldItemId(player);
        return result.successful()
                ? result.value()
                : Optional.empty();
    }

}

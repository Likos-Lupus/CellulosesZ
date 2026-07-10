package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Optional;
import java.util.regex.Pattern;

public final class DefaultItemService implements ItemService {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    private final PlatformService platform;

    public DefaultItemService(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public Optional<ItemDescriptor> parse(String input) {
        if (input.isBlank()) {
            return Optional.empty();
        }

        var parts = input.trim().split("\\s+");
        var item = normalize(parts[0]);
        if (!ID_PATTERN.matcher(item).matches()) {
            return Optional.empty();
        }

        var count = 1;
        if (parts.length >= 2) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        if (count <= 0) return Optional.empty();
        return Optional.of(new ItemDescriptor(item, count));
    }

    @Override
    public boolean give(CellPlayer player, ItemDescriptor item) {
        var descriptor = item.normalizedItem();
        if (!ID_PATTERN.matcher(descriptor).matches() || item.count <= 0) {
            return false;
        }

        return platform.dispatchConsoleCommand("give %s %s %d".formatted(player.name(), descriptor, item.count));
    }

    private String normalize(String value) {
        var normalized = value.trim().toLowerCase();
        return normalized.indexOf(':') < 0 ? "minecraft:" + normalized : normalized;
    }

}

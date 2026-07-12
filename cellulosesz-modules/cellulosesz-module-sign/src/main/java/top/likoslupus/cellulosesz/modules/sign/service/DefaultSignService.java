package top.likoslupus.cellulosesz.modules.sign.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.modules.sign.SignConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultSignService implements SignService {

    private final SignConfig config;
    private final PermissionService permissions;
    private final Map<String, CellSignHandler> handlers = new LinkedHashMap<>();
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public DefaultSignService(
            SignConfig config,
            PermissionService permissions
    ) {
        this.config = config;
        this.permissions = permissions;
    }

    @Override
    public void register(CellSignHandler handler) {
        handlers.put(handler.id().toLowerCase(Locale.ROOT), handler);
    }

    @Override
    public List<String> handlers() {
        return List.copyOf(handlers.keySet());
    }

    @Override
    public SignUseResult use(
            CellPlayer player,
            List<String> lines,
            boolean sneaking
    ) {
        if (!config.enabled || lines.isEmpty()) return SignUseResult.pass();

        var id = normalizeHeader(lines.getFirst());
        var handler = handlers.get(id);
        if (handler == null || !enabled(id)) return SignUseResult.pass();

        if (!permissions.has(player.nativeHandle(), "cellulosesz.sign.use." + id)) {
            return SignUseResult.failure(
                    "service.sign.no-permission",
                    Map.of("sign", handler.id())
            );
        }

        var now = System.currentTimeMillis();
        var cooldownMillis = Math.max(0, config.interaction.cooldownTicks) * 50L;
        var previous = lastUse.get(player.uuid());
        if (previous != null && now - previous < cooldownMillis) {
            return SignUseResult.failure("service.sign.cooldown");
        }

        try {
            var result = handler.use(new SignUseContext(player, lines, sneaking));
            if (result.handled()) lastUse.put(player.uuid(), now);
            return result;
        } catch (RuntimeException exception) {
            return SignUseResult.failure(
                    "service.sign.execution-failed",
                    Map.of("reason", String.valueOf(exception.getMessage()))
            );
        }
    }

    private String normalizeHeader(String value) {
        var normalized = value.trim();
        normalized = normalized.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private boolean enabled(String id) {
        return switch (id) {
            case "warp" -> config.signs.warp;
            case "buy" -> config.signs.buy;
            case "sell" -> config.signs.sell;
            case "kit" -> config.signs.kit;
            default -> false;
        };
    }

}

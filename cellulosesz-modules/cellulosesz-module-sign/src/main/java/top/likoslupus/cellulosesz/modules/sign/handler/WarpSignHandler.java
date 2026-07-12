package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;

import java.util.Map;

public final class WarpSignHandler implements CellSignHandler {

    private final WarpService warps;
    private final TeleportService teleports;
    private final PermissionService permissions;

    public WarpSignHandler(
            WarpService warps,
            TeleportService teleports,
            PermissionService permissions
    ) {
        this.warps = warps;
        this.teleports = teleports;
        this.permissions = permissions;
    }

    @Override
    public String id() {
        return "Warp";
    }

    @Override
    public SignUseResult use(SignUseContext context) {
        var name = context.line(1);
        if (name.isBlank()) return SignUseResult.failure("service.sign.warp-name-required");

        var warp = warps.warp(name).join();
        if (warp.isEmpty()) {
            return SignUseResult.failure("service.sign.warp-not-found", Map.of("warp", name));
        }

        var permission = warp.get().permission;
        if (permission != null && !permission.isBlank()
                && !permissions.has(context.player().nativeHandle(), permission)
        ) {
            return SignUseResult.failure("service.sign.warp-no-permission");
        }

        var result = teleports.teleport(
                context.player(),
                warp.get().location,
                new TeleportOptions().safe(true).warmupSeconds(0)
        ).join();
        return result.success() ? SignUseResult.success(
                "service.sign.warp-success",
                Map.of("warp", warp.get().displayName)
        ) : SignUseResult.failure(result.message());
    }

}

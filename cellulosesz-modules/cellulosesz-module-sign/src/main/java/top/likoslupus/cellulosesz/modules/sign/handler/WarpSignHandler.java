package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;

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
        if (name.isBlank()) return SignUseResult.failure("[Warp] 第二行必须填写 Warp 名称。");

        var warp = warps.warp(name).join();
        if (warp.isEmpty()) return SignUseResult.failure("Warp 不存在: " + name);

        var permission = warp.get().permission;
        if (permission != null && !permission.isBlank()
                && !permissions.has(context.player().nativeHandle(), permission)
        ) {
            return SignUseResult.failure("你没有权限使用此 Warp。");
        }

        var result = teleports.teleport(
                context.player(),
                warp.get().location,
                new TeleportOptions().safe(true).warmupSeconds(0)
        ).join();
        return result.success()
                ? SignUseResult.success("已传送到 Warp: " + warp.get().displayName)
                : SignUseResult.failure(result.message());
    }

}

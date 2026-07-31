package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class WarpSignHandler implements CellSignHandler {

    private final WarpService warps;
    private final TeleportService teleports;
    private final PermissionService permissions;

    public WarpSignHandler(
            WarpService warps,
            TeleportService teleports,
            PermissionService permissions
    ) {
        this.warps = requireNonNull(warps, "warps");
        this.teleports = requireNonNull(teleports, "teleports");
        this.permissions = requireNonNull(permissions, "permissions");
    }

    @Override
    public String id() {
        return "Warp";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var name = context.line(1);
        if (name.isBlank()) {
            return SignUseResult.failure("service.sign.warp-name-required");
        }
        return warps.cachedWarp(name).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure(
                        "service.sign.warp-not-found",
                        Map.of("warp", name)
                );
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var name = context.line(1);
        if (name.isBlank()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.warp-name-required"
            ));
        }

        return warps.warp(name).thenCompose(warp -> {
            if (warp.isEmpty()) {
                return CompletableFuture.completedFuture(SignUseResult.failure(
                        "service.sign.warp-not-found",
                        Map.of("warp", name)
                ));
            }

            var value = warp.orElseThrow();
            var requiredPermission = warps.requiredPermission(value);
            if (requiredPermission.isPresent()
                    && !permissions.has(context.player().nativeHandle(), requiredPermission.orElseThrow())
            ) {
                return CompletableFuture.completedFuture(SignUseResult.failure(
                        "service.sign.warp-no-permission"
                ));
            }

            return teleports.teleport(
                            context.player(),
                            value.location,
                            TeleportOptions.defaults().withSafe(true).withWarmup(0)
                    )
                    .thenApply(result -> result.success() ?
                            SignUseResult.success(
                                    "service.sign.warp-success",
                                    Map.of("warp", value.displayName)
                            ) :
                            SignUseResult.failure(result.message()));
        });
    }

}

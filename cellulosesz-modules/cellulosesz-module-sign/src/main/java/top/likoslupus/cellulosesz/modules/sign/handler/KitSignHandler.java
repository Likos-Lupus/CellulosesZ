package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class KitSignHandler implements CellSignHandler {

    private final KitService kits;
    private final PermissionService permissions;

    public KitSignHandler(KitService kits, PermissionService permissions) {
        this.kits = Objects.requireNonNull(kits, "kits");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
    }

    @Override
    public String id() {
        return "Kit";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var id = context.line(1).toLowerCase(Locale.ROOT);
        if (id.isBlank()) return SignUseResult.failure("service.sign.kit-name-required");
        return kits.kit(id).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.kit-not-found", Map.of("kit", id));
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var id = context.line(1).toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.kit-name-required"));
        }
        var kit = kits.kit(id);
        if (kit.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.kit-not-found", Map.of("kit", id)));
        }
        if (!kit.orElseThrow().permission.isBlank()
                && !permissions.has(context.player().nativeHandle(), kit.orElseThrow().permission)) {
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.kit-no-permission"));
        }
        return kits.claim(context.player(), kit.orElseThrow())
                .thenApply(result -> result.success()
                        ? SignUseResult.success(result.message())
                        : SignUseResult.failure(result.message()));
    }

}

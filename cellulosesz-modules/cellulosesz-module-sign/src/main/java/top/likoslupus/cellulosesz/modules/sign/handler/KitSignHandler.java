package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class KitSignHandler implements CellSignHandler {

    private final KitService kits;
    private final PermissionService permissions;

    public KitSignHandler(KitService kits, PermissionService permissions) {
        this.kits = requireNonNull(kits, "kits");
        this.permissions = requireNonNull(permissions, "permissions");
    }

    @Override
    public String id() {
        return "Kit";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var id = context.line(1).toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return SignUseResult.failure("service.sign.kit-name-required");
        }
        return kits.kit(id).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure(
                        "service.sign.kit-not-found",
                        MessageArguments.builder().put("kit", id).build()
                );
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var id = context.line(1).toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.kit-name-required"));
        }
        var kit = kits.kit(id);
        if (kit.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.kit-not-found",
                    MessageArguments.builder().put("kit", id).build()
            ));
        }
        var permission = kit.orElseThrow().permission();
        if (permission.isPresent()
                && !permissions.has(context.player(), permission.orElseThrow())) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.kit-no-permission"));
        }
        return kits.claim(context.player(), kit.orElseThrow())
                .thenApply(result -> result.success()
                        ? SignUseResult.success(result.message())
                        : SignUseResult.failure(result.message()));
    }

}

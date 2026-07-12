package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.Map;

public final class KitSignHandler implements CellSignHandler {

    private final KitService kits;
    private final PermissionService permissions;

    public KitSignHandler(
            KitService kits,
            PermissionService permissions
    ) {
        this.kits = kits;
        this.permissions = permissions;
    }

    @Override
    public String id() {
        return "Kit";
    }

    @Override
    public SignUseResult use(SignUseContext context) {
        var id = context.line(1).toLowerCase();
        if (id.isBlank()) return SignUseResult.failure("service.sign.kit-name-required");

        var kit = kits.kit(id);
        if (kit.isEmpty()) {
            return SignUseResult.failure("service.sign.kit-not-found", Map.of("kit", id));
        }

        if (!kit.get().permission.isBlank()
                && !permissions.has(context.player().nativeHandle(), kit.get().permission)
        ) {
            return SignUseResult.failure("service.sign.kit-no-permission");
        }

        var result = kits.claim(context.player(), kit.get()).join();
        return result.success()
                ? SignUseResult.success(result.message())
                : SignUseResult.failure(result.message());
    }

}

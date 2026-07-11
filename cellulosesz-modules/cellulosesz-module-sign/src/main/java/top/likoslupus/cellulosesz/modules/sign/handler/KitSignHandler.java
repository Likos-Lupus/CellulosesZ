package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

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
        if (id.isBlank()) return SignUseResult.failure("[Kit] 第二行必须填写 Kit 名称。");

        var kit = kits.kit(id);
        if (kit.isEmpty()) return SignUseResult.failure("Kit 不存在: " + id);

        if (!kit.get().permission.isBlank()
                && !permissions.has(context.player().nativeHandle(), kit.get().permission)
        ) {
            return SignUseResult.failure("你没有权限领取此 Kit。");
        }

        var result = kits.claim(context.player(), kit.get()).join();
        return result.success()
                ? SignUseResult.success(result.message())
                : SignUseResult.failure(result.message());
    }

}

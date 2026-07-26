package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RepairSignHandler implements SynchronousSignHandler {

    private static final Set<String> MODES = Set.of("", "hand", "all");
    private final PlatformService platform;

    public RepairSignHandler(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String id() {
        return "Repair";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return MODES.contains(context.line(1).toLowerCase(Locale.ROOT))
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.repair-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var count = platform.repairItems(context.player(), context.line(1).equalsIgnoreCase("all"));
        return count > 0
                ? SignUseResult.success("service.sign.repair-success", Map.of("count", count))
                : SignUseResult.failure("service.sign.repair-nothing");
    }

}

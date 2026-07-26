package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Map;
import java.util.Objects;

public final class WorkstationSignHandler implements SynchronousSignHandler {

    private final PlatformService platform;
    private final String id;
    private final String workstation;

    public WorkstationSignHandler(PlatformService platform, String id, String workstation) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.id = requireText(id, "id");
        this.workstation = requireText(workstation, "workstation");
    }

    private static String requireText(String value, String field) {
        var checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return checked;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return context.line(1).isBlank() && context.line(2).isBlank() && context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.workstation-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return platform.openWorkstation(context.player(), workstation)
                ? SignUseResult.success("service.sign.workstation-opened", Map.of("workstation", id))
                : SignUseResult.failure("service.sign.workstation-failed", Map.of("workstation", id));
    }

}

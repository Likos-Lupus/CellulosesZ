package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.WorkstationKind;
import top.likoslupus.cellulosesz.api.item.WorkstationPlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class WorkstationSignHandler implements SynchronousSignHandler {

    private final WorkstationPlatformService workstations;
    private final String id;
    private final WorkstationKind kind;

    public WorkstationSignHandler(
            WorkstationPlatformService workstations,
            String id,
            WorkstationKind kind
    ) {
        this.workstations = requireNonNull(workstations, "workstations");
        this.id = requireNonBlank(id, "id").trim();
        this.kind = requireNonNull(kind, "kind");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return context.line(1).isBlank()
                && context.line(2).isBlank()
                && context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.workstation-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return workstations.open(context.player(), kind).successful()
                ?
                SignUseResult.success(
                        "service.sign.workstation-opened",
                        MessageArguments.builder().put("workstation", id).build()
                )
                : SignUseResult.failure(
                        "service.sign.workstation-failed",
                        MessageArguments.builder().put("workstation", id).build()
                );
    }

}

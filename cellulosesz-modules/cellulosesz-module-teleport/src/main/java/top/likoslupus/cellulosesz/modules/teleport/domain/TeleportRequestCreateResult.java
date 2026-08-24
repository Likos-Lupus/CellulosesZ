package top.likoslupus.cellulosesz.modules.teleport.domain;

import static java.util.Objects.requireNonNull;

public record TeleportRequestCreateResult(
        TeleportRequestCreateStatus status,
        TeleportRequest request
) {

    public TeleportRequestCreateResult {
        requireNonNull(status, "status");
        requireNonNull(request, "request");
    }

    public boolean created() {
        return status == TeleportRequestCreateStatus.CREATED;
    }

}

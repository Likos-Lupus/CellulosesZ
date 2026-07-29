package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandSourceKind;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

public record CommandDescriptor(
        String moduleId,
        String canonicalName,
        String permission,
        CommandSourceKind requiredSourceKind
) {

    public CommandDescriptor {
        moduleId = requireNonBlank(moduleId, "moduleId");
        canonicalName = requireNonBlank(canonicalName, "canonicalName");
        permission = requireNonNull(permission, "permission").trim();
        requiredSourceKind = requireNonNull(requiredSourceKind, "requiredSourceKind");
    }

}

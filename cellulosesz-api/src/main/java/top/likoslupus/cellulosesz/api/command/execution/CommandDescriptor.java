package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandSourceKind;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

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
        requireNonNull(requiredSourceKind, "requiredSourceKind");
    }

}

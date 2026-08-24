package top.likoslupus.cellulosesz.api.command.execution

import top.likoslupus.cellulosesz.api.command.CommandSourceKind
import top.likoslupus.cellulosesz.api.validation.requireNonBlank

@JvmRecord
public data class CommandDescriptor(
    public val moduleId: String,
    public val canonicalName: String,
    public val permission: String,
    public val requiredSourceKind: CommandSourceKind
) {

    init {
        moduleId.requireNonBlank { "moduleId" }
        canonicalName.requireNonBlank { "canonicalName" }
    }

}

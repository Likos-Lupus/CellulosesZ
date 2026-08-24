package top.likoslupus.cellulosesz.api.command.execution

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import java.util.*

public interface CommandPolicyContext {

    public fun invokedLabel(): String

    public fun canonicalRoot(): String

    public fun player(): Boolean

    public fun playerUuid(): UUID?

    public fun playerName(): String?

    public fun hasPermission(permission: String): Boolean

    public fun auditSummary(): String

    public fun reply(message: LocalizedMessage)

    public fun error(message: LocalizedMessage)

}

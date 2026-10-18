package top.likoslupus.cellulosesz.utility.powertool

import java.util.*

/** Which interaction fires a bound powertool command. */
internal enum class PowertoolTrigger {

    LEFT,
    RIGHT,

    /** Both left and right click. */
    BOTH,

}

/**
 * A parsed `/powertool` request. A bare command binds to both clicks; the `l:`/`r:`/`c:` prefixes
 * select left, right or both. `Recursive` rejects a command that would re-enter the powertool
 * commands (which would recurse through the interaction handler).
 */
internal sealed interface PowertoolSpec {

    data class Bound(
        val trigger: PowertoolTrigger,
        val command: String,
    ) : PowertoolSpec

    data object Clear : PowertoolSpec

    data object Recursive : PowertoolSpec

    data object Invalid : PowertoolSpec

    companion object {

        private val RESERVED = setOf(
            "powertool",
            "powertoollist",
            "powertooltoggle",
            "cellulosesz:powertool",
            "cellulosesz:powertoollist",
            "cellulosesz:powertooltoggle",
        )

        fun parse(raw: String): PowertoolSpec {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                return Clear
            }
            val (trigger, body) = when {
                trimmed.startsWith("l:", ignoreCase = true) ->
                    PowertoolTrigger.LEFT to trimmed.substring(2)

                trimmed.startsWith("r:", ignoreCase = true) ->
                    PowertoolTrigger.RIGHT to trimmed.substring(2)

                trimmed.startsWith("c:", ignoreCase = true) ->
                    PowertoolTrigger.BOTH to trimmed.substring(2)

                else -> PowertoolTrigger.BOTH to trimmed
            }
            val command = body.trim().removePrefix("/").trim()
            if (command.isEmpty()) {
                return Invalid
            }
            val first = command.split(' ', limit = 2).first().lowercase(Locale.ROOT)
            if (first in RESERVED) {
                return Recursive
            }
            return Bound(trigger, command)
        }

        /** True when a stored command still points back into a powertool command. */
        fun isRecursive(command: String): Boolean {
            val first = command.trim()
                    .removePrefix("/")
                    .split(' ', limit = 2)
                    .first()
                    .lowercase(Locale.ROOT)
            return first in RESERVED
        }

    }

}

package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.ListRow
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.utility.powertool.PowertoolBinding
import top.likoslupus.cellulosesz.utility.powertool.PowertoolTrigger

internal fun UtilityMessages.powertoolBound(itemId: String, trigger: PowertoolTrigger): Notice =
    success(
        triggerBoundKey(trigger),
        identifier(itemId),
    )

internal fun UtilityMessages.powertoolCleared(itemId: String): Notice =
    success(
        MessageKey("utility.powertool.cleared"),
        identifier(itemId),
    )

internal fun UtilityMessages.powertoolRejected(recursive: Boolean): Notice =
    notice(
        when (recursive) {
            true -> MessageKey("utility.powertool.recursive")
            false -> MessageKey("utility.powertool.invalid")
        }
    )

internal fun UtilityMessages.powertoolNoItem(): Notice =
    notice(MessageKey("utility.powertool.no_item"))

internal fun UtilityMessages.powertoolStorage(): Notice =
    notice(MessageKey("utility.powertool.storage"))

internal fun UtilityMessages.powertoolDisabled(): Notice =
    notice(MessageKey("utility.powertool.disabled"))

internal fun UtilityMessages.powertoolToggle(enabled: Boolean): Notice =
    info(
        when (enabled) {
            true -> MessageKey("utility.powertool.toggled_on")
            false -> MessageKey("utility.powertool.toggled_off")
        }
    )

internal fun UtilityMessages.powertoolList(bindings: List<PowertoolBinding>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("utility.powertool.list_title")),
        rows = bindings.map { binding ->
            ListRow(
                main = LocalizedText(
                    triggerRowKey(binding.trigger),
                    listOf(
                        identifier(binding.itemId),
                        literal("/" + binding.command),
                    ),
                )
            )
        },
        emptyText = LocalizedText(MessageKey("utility.powertool.list_empty")),
    )

private fun UtilityMessages.triggerBoundKey(trigger: PowertoolTrigger): MessageKey =
    when (trigger) {
        PowertoolTrigger.LEFT -> MessageKey("utility.powertool.bound.left")
        PowertoolTrigger.RIGHT -> MessageKey("utility.powertool.bound.right")
        PowertoolTrigger.BOTH -> MessageKey("utility.powertool.bound.both")
    }

private fun UtilityMessages.triggerRowKey(trigger: PowertoolTrigger): MessageKey =
    when (trigger) {
        PowertoolTrigger.LEFT -> MessageKey("utility.powertool.list_row.left")
        PowertoolTrigger.RIGHT -> MessageKey("utility.powertool.list_row.right")
        PowertoolTrigger.BOTH -> MessageKey("utility.powertool.list_row.both")
    }

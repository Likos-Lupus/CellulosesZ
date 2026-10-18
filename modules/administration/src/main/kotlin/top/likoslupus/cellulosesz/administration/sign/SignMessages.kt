package top.likoslupus.cellulosesz.administration.sign

import top.likoslupus.cellulosesz.administration.format.AdminText
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument

/** Semantic results for `/editsign`. */
internal object SignMessages {

    fun setLine(line: Int): Notice =
        success(
            MessageKey("administration.editsign.set"),
            AdminText.count(line)
        )

    fun clearedLine(line: Int): Notice =
        success(
            MessageKey("administration.editsign.cleared"),
            AdminText.count(line)
        )

    fun clearedAll(): Notice =
        success(MessageKey("administration.editsign.cleared_all"))

    fun copied(): Notice =
        success(MessageKey("administration.editsign.copied"))

    fun pasted(): Notice =
        success(MessageKey("administration.editsign.pasted"))

    fun noClipboard(): Notice =
        error(MessageKey("administration.editsign.no_clipboard"))

    fun notSign(): Notice =
        error(MessageKey("administration.editsign.not_sign"))

    fun disabled(): Notice =
        error(MessageKey("administration.editsign.disabled"))

    fun backDisabled(): Notice =
        error(MessageKey("administration.editsign.back_disabled"))

    fun tooFar(): Notice =
        error(MessageKey("administration.editsign.too_far"))

    fun tooLong(): Notice =
        error(MessageKey("administration.editsign.too_long"))

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun error(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

}

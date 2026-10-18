package top.likoslupus.cellulosesz.administration.sign

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport

/** Semantic results for `/editsign`. */
internal object SignMessages : MessageSupport() {

    fun setLine(line: Int): Notice =
        success(
            MessageKey("administration.editsign.set"),
            count(line)
        )

    fun clearedLine(line: Int): Notice =
        success(
            MessageKey("administration.editsign.cleared"),
            count(line)
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

}

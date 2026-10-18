package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.nickname.NicknameCurrent
import top.likoslupus.cellulosesz.player.nickname.NicknameForResult
import top.likoslupus.cellulosesz.player.nickname.NicknameSetResult

internal fun PlayerMessages.nicknameFeedback(result: NicknameSetResult): Notice =
    when (result) {
        is NicknameSetResult.Set -> success(
            MessageKey("player.nickname.set"),
            literal(result.display)
        )

        NicknameSetResult.Cleared -> success(MessageKey("player.nickname.cleared"))
        NicknameSetResult.Disabled -> error(MessageKey("player.nickname.disabled"))
        NicknameSetResult.Empty -> error(MessageKey("player.nickname.empty"))
        is NicknameSetResult.TooShort -> error(
            MessageKey("player.nickname.too_short"),
            count(result.minimum)
        )

        is NicknameSetResult.TooLong -> error(
            MessageKey("player.nickname.too_long"),
            count(result.maximum)
        )

        NicknameSetResult.InvalidCharacters ->
            error(MessageKey("player.nickname.invalid_characters"))

        NicknameSetResult.ColorsNotAllowed ->
            error(MessageKey("player.nickname.colors_not_allowed"))

        NicknameSetResult.PatternMismatch ->
            error(MessageKey("player.nickname.pattern"))

        NicknameSetResult.Blacklisted ->
            error(MessageKey("player.nickname.blacklisted"))

        NicknameSetResult.Taken -> error(MessageKey("player.nickname.taken"))
        NicknameSetResult.Reserved -> error(MessageKey("player.nickname.reserved"))
        NicknameSetResult.StorageUnavailable -> storageUnavailable()
    }

/** The bare `/nick` status line: the current nickname, or none. */
internal fun PlayerMessages.nicknameSelfStatus(current: NicknameCurrent): Notice =
    when (current) {
        is NicknameCurrent.Set -> info(
            MessageKey("player.nickname.status.set"),
            literal(current.display)
        )

        NicknameCurrent.None -> info(MessageKey("player.nickname.status.none"))
        NicknameCurrent.StorageUnavailable -> storageUnavailable()
    }

/** The subcommand list shown next to the bare `/nick` status line. */
internal fun PlayerMessages.nicknameUsage(): Notice =
    info(MessageKey("player.nickname.usage"))

/** Feedback for a staff `<setfor|clearfor>` change against a resolved target. */
internal fun PlayerMessages.nicknameFor(targetName: String, result: NicknameForResult): Notice =
    when (result) {
        is NicknameForResult.Applied -> when (val outcome = result.result) {
            is NicknameSetResult.Set -> success(
                MessageKey("player.nickname.admin.set"),
                playerName(result.targetName),
                literal(outcome.display)
            )

            NicknameSetResult.Cleared -> success(
                MessageKey("player.nickname.admin.cleared"),
                playerName(result.targetName)
            )

            else -> nicknameFeedback(outcome)
        }

        NicknameForResult.TargetNotFound -> notFound(targetName)
    }

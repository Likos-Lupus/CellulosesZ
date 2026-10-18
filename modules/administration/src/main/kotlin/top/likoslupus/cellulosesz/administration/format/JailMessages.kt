package top.likoslupus.cellulosesz.administration.format

import top.likoslupus.cellulosesz.administration.jail.*
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import java.time.Duration
import java.time.Instant

internal fun AdministrationMessages.jail(result: JailActionOutcome, now: Instant): Notice =
    when (result) {
        is JailActionOutcome.Jailed -> {
            val notice = when {
                !result.transferred ->
                    success(
                        MessageKey("administration.jail.success_unmoved"),
                        player(result.target.name),
                    )

                result.expiresAt != null ->
                    success(
                        MessageKey("administration.jail.success_temp"),
                        player(result.target.name),
                        identifier(result.jailName.value),
                        elapsed(result.expiresAt, now),
                    )

                result.remainingSeconds != null ->
                    success(
                        MessageKey("administration.jail.success_temp"),
                        player(result.target.name),
                        identifier(result.jailName.value),
                        elapsed(Duration.ofSeconds(result.remainingSeconds)),
                    )

                else ->
                    success(
                        MessageKey("administration.jail.success"),
                        player(result.target.name),
                        identifier(result.jailName.value),
                    )
            }
            when {
                !result.transferred ->
                    notice.copy(
                        detail = LocalizedText(
                            MessageKey("administration.jail.transfer_warning")
                        )
                    )

                !result.returnRecorded ->
                    notice.copy(
                        detail = LocalizedText(
                            MessageKey("administration.jail.return_missing")
                        )
                    )

                !result.auditRecorded ->
                    notice.copy(
                        detail = LocalizedText(
                            MessageKey("administration.error.audit_warning")
                        )
                    )

                else -> notice
            }
        }

        is JailActionOutcome.ReleaseRequested ->
            withAudit(
                success(
                    MessageKey("administration.jail.release_success"),
                    player(result.target.name),
                ),
                result.auditRecorded,
            )

        JailActionOutcome.TargetUnknown -> error(MessageKey("administration.jail.target_unknown"))
        JailActionOutcome.SelfTarget -> error(MessageKey("administration.jail.self"))
        JailActionOutcome.TargetProtected -> error(MessageKey("administration.jail.protected"))
        JailActionOutcome.AlreadyJailed -> error(MessageKey("administration.jail.already"))
        JailActionOutcome.NotJailed -> error(MessageKey("administration.jail.not_jailed"))
        JailActionOutcome.InvalidJailName -> error(MessageKey("administration.jail.invalid_name"))

        is JailActionOutcome.JailUnknown ->
            error(
                MessageKey("administration.jail.jail_unknown"),
                identifier(result.jailName.value),
            )

        JailActionOutcome.NoDefaultJail -> error(MessageKey("administration.jail.no_default"))
        JailActionOutcome.Disabled -> error(MessageKey("administration.jail.disabled"))
        JailActionOutcome.StorageUnavailable -> error(MessageKey("administration.jail.storage"))
        JailActionOutcome.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.setJail(result: SetJailOutcome): Notice =
    when (result) {
        is SetJailOutcome.Saved ->
            success(
                MessageKey("administration.setjail.success"),
                identifier(result.name.value),
            )

        SetJailOutcome.InvalidName -> error(MessageKey("administration.setjail.invalid_name"))
        SetJailOutcome.LimitReached -> error(MessageKey("administration.setjail.limit"))
        SetJailOutcome.Disabled -> error(MessageKey("administration.setjail.disabled"))
        SetJailOutcome.StorageUnavailable -> error(MessageKey("administration.setjail.storage"))
        SetJailOutcome.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.deleteJail(name: String, result: DeleteJailOutcome): Notice =
    when (result) {
        DeleteJailOutcome.Deleted ->
            success(
                MessageKey("administration.deljail.success"),
                identifier(name),
            )

        DeleteJailOutcome.NotFound ->
            error(
                MessageKey("administration.deljail.not_found"),
                identifier(name),
            )

        DeleteJailOutcome.HasSentences ->
            error(
                MessageKey("administration.deljail.in_use"),
                identifier(name),
            )

        DeleteJailOutcome.StorageUnavailable -> error(MessageKey("administration.deljail.storage"))
        DeleteJailOutcome.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.jails(locations: List<JailLocation>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("administration.jails.title")),
        rows = locations.map { location ->
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(identifier(location.name.value)),
                ),
                supporting = LocalizedText(
                    MessageKey("core.value"),
                    listOf(
                        position(
                            location.x,
                            location.y,
                            location.z,
                            location.dimension
                        )
                    ),
                ),
            )
        },
        emptyText = LocalizedText(MessageKey("administration.jails.empty")),
    )

internal fun AdministrationMessages.jailedPlayers(
    sentences: List<JailSentence>,
    now: Instant
): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("administration.jailedplayers.title")),
        rows = sentences.map { sentence ->
            val supporting = when {
                sentence.mode == JailSentenceMode.ONLINE &&
                        sentence.remainingSeconds != null -> LocalizedText(
                    MessageKey("administration.jailedplayers.row_online"),
                    listOf(
                        identifier(sentence.jailName.value),
                        elapsed(Duration.ofSeconds(sentence.remainingSeconds)),
                    ),
                )

                sentence.expiresAt == null -> LocalizedText(
                    MessageKey("administration.jailedplayers.row"),
                    listOf(identifier(sentence.jailName.value)),
                )

                else -> LocalizedText(
                    MessageKey("administration.jailedplayers.row_temp"),
                    listOf(
                        identifier(sentence.jailName.value),
                        elapsed(sentence.expiresAt, now),
                    ),
                )
            }
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(player(sentence.playerName)),
                ),
                supporting = supporting,
            )
        },
        emptyText = LocalizedText(MessageKey("administration.jailedplayers.empty")),
    )

internal fun AdministrationMessages.jailedTargetNotice(reason: String): Notice =
    Notice(
        LocalizedText(
            MessageKey("administration.jail.target_notice"),
            listOf(literal(reason)),
        ),
        NoticeTone.WARNING,
    )

internal fun AdministrationMessages.releasedTargetNotice(): Notice =
    Notice(
        LocalizedText(MessageKey("administration.jail.release_target_notice")),
        NoticeTone.INFO,
    )

internal fun AdministrationMessages.notifyJail(actor: String, target: String): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.jail"),
            literal(actor),
            player(target),
        )
    )

internal fun AdministrationMessages.notifyJailTemp(
    actor: String,
    target: String,
    expiresAt: Instant,
    now: Instant,
): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.jail_temp"),
            literal(actor),
            player(target),
            elapsed(expiresAt, now),
        )
    )

internal fun AdministrationMessages.notifyUnjail(actor: String, target: String): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.unjail"),
            literal(actor),
            player(target),
        )
    )

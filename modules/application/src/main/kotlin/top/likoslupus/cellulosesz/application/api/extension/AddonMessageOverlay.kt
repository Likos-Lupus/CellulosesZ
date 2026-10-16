package top.likoslupus.cellulosesz.application.api.extension

import top.likoslupus.cellulosesz.api.presentation.AddonTextKey
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.core.text.i18n.MessageOverlay
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate
import top.likoslupus.cellulosesz.core.text.i18n.template.compileMessageTemplate
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import java.util.concurrent.ConcurrentHashMap

/**
 * A runtime overlay of addon-registered translations. Population happens only during the addon
 * registration window, but lookups happen on the per-recipient render path, so the backing maps are
 * concurrent. Addon keys are namespaced as `<addonId>.<key>` so an addon can never shadow a core or
 * another addon's key.
 */
internal class AddonMessageOverlay : MessageOverlay {

    private val templates =
        ConcurrentHashMap<Pair<MessageKey, LanguageId>, MessageTemplate>()

    private val knownKeys = ConcurrentHashMap.newKeySet<MessageKey>()

    /** Registers one template. Returns false for a malformed key/template or a duplicate locale. */
    fun register(
        addonId: String,
        key: String,
        language: LanguageId,
        source: String,
    ): Boolean {
        val messageKey = messageKey(addonId, key) ?: return false
        val template = compileMessageTemplate(source) ?: return false
        val inserted = templates.putIfAbsent(messageKey to language, template) == null
        if (inserted) {
            knownKeys += messageKey
        }
        return inserted
    }

    /** Resolves an addon text key to a renderable [LocalizedText], or null when it was never registered. */
    fun resolve(key: AddonTextKey, language: LanguageId): LocalizedText? {
        val messageKey = messageKey(key.addonId, key.key) ?: return null
        return if (messageKey in knownKeys) LocalizedText(messageKey) else null
    }

    override fun template(language: LanguageId, key: MessageKey): MessageTemplate? =
        templates[key to language]

    companion object {

        fun messageKey(addonId: String, key: String): MessageKey? =
            runCatching { MessageKey("$addonId.$key") }.getOrNull()

    }

}

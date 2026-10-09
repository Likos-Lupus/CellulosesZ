package top.likoslupus.cellulosesz.core.text.i18n.preference

import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.util.*

/** Durable explicit player language override. No row means "inherit the server default". */
public interface PlayerLanguagePreferenceRepository {

    public suspend fun loadAll(): Map<UUID, LanguageId>

    public suspend fun set(playerId: UUID, language: LanguageId)

    public suspend fun remove(playerId: UUID)

}

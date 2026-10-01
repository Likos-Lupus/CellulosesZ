package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.foundation.persistence.StorageJson

internal object ConfigCodec {

    fun encode(config: CellulosesConfig): String =
        StorageJson.format.encodeToString(
            CellulosesConfig.serializer(),
            config
        )

    fun decode(text: String): CellulosesConfig =
        StorageJson.format.decodeFromString(
            CellulosesConfig.serializer(),
            text
        )

}

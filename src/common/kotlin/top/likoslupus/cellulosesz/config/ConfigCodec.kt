package top.likoslupus.cellulosesz.config

import top.likoslupus.cellulosesz.persistence.StorageJson

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

package top.likoslupus.cellulosesz.foundation.config

/** Text -> typed config decoder. Keeps [ConfigStore] independent of any concrete format. */
public fun interface ConfigDecoder<T> {

    public fun decode(text: String): T

}

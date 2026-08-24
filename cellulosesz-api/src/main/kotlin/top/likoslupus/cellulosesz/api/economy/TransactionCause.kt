package top.likoslupus.cellulosesz.api.economy

@JvmRecord
public data class TransactionCause(
    public val type: String,
    public val actor: String,
    public val note: String
) {

    public companion object {

        @JvmStatic
        public fun command(actor: String, note: String): TransactionCause =
            TransactionCause("command", actor, note)

        @JvmStatic
        public fun system(note: String): TransactionCause =
            TransactionCause("system", "system", note)

    }

}

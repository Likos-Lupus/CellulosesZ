package top.likoslupus.cellulosesz.api.user

@JvmRecord
public data class UserUpdate<T>(
    public val user: CellUser,
    public val result: T
) {

    public companion object {

        @JvmStatic
        public fun replacing(user: CellUser): UserUpdate<Void?> =
            UserUpdate(user, null)

        @JvmStatic
        public fun <T> of(user: CellUser, result: T): UserUpdate<T> =
            UserUpdate(user, result)

    }

}

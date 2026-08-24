package top.likoslupus.cellulosesz.api.user

import top.likoslupus.cellulosesz.api.util.toImmutableSet
import java.util.*

public class UserRelations(
    ignored: Set<UUID>
) {

    @get:JvmName("ignored")
    public val ignored: Set<UUID> = ignored.toImmutableSet()

    public fun withIgnored(value: Set<UUID>): UserRelations =
        UserRelations(value.toImmutableSet())

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is UserRelations -> false
            else -> ignored == other.ignored
        }
    }

    override fun hashCode(): Int = ignored.hashCode()

    override fun toString(): String = "UserRelations[ignored=$ignored]"

    public companion object {

        @JvmStatic
        public fun defaults(): UserRelations = UserRelations(emptySet())

    }

}

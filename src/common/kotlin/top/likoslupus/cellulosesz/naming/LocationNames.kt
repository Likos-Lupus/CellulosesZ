package top.likoslupus.cellulosesz.naming

internal object LocationNames {

    private val VALID = Regex("[a-z0-9_-]{1,32}")

    fun isValid(raw: String): Boolean = VALID.matches(raw)

}

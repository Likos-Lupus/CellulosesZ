package top.likoslupus.cellulosesz.api.service

public interface Registration : AutoCloseable {

    public fun owner(): String

    public fun closed(): Boolean

    override fun close()

}

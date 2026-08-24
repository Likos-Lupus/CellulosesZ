package top.likoslupus.cellulosesz.api.text

public interface TextService {

    public fun info(): List<String>

    public fun motd(): List<String>

    public fun rules(): List<String>

    public fun custom(name: String): List<String>

    public fun customNames(): Set<String>

    public fun pageSize(): Int

}

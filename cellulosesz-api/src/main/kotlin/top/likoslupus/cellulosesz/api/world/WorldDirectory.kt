package top.likoslupus.cellulosesz.api.world

public interface WorldDirectory {

    public fun loadedWorldIds(): List<String>

    public fun resolveLoadedWorld(input: String): String? {
        val resolution = resolve(input)
        return if (resolution is WorldResolution.Resolved) {
            resolution.worldId
        } else {
            null
        }
    }

    public fun resolve(input: String): WorldResolution

}

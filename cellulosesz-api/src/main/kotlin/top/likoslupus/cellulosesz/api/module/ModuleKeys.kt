package top.likoslupus.cellulosesz.api.module

object ModuleKeys {

    val COMMAND: ModuleKey = ModuleKey("command")
    val PERMISSION: ModuleKey = ModuleKey("permission")
    val USER: ModuleKey = ModuleKey("user")
    val TEXT: ModuleKey = ModuleKey("text")
    val ITEM: ModuleKey = ModuleKey("item")
    val TELEPORT: ModuleKey = ModuleKey("teleport")
    val PLAYERSTATE: ModuleKey = ModuleKey("playerstate")
    val WORLD: ModuleKey = ModuleKey("world")
    val ECONOMY: ModuleKey = ModuleKey("economy")
    val HOME: ModuleKey = ModuleKey("home")
    val KIT: ModuleKey = ModuleKey("kit")
    val MESSAGING: ModuleKey = ModuleKey("messaging")
    val WARP: ModuleKey = ModuleKey("warp")
    val ADMIN: ModuleKey = ModuleKey("admin")
    val SIGN: ModuleKey = ModuleKey("sign")

    val ALL: Set<ModuleKey> = setOf(
        COMMAND,
        PERMISSION,
        USER,
        TEXT,
        ITEM,
        TELEPORT,
        PLAYERSTATE,
        WORLD,
        ECONOMY,
        HOME,
        KIT,
        MESSAGING,
        WARP,
        ADMIN,
        SIGN
    )

}

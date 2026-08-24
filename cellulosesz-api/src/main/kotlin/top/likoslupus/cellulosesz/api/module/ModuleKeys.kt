package top.likoslupus.cellulosesz.api.module

public object ModuleKeys {

    public val COMMAND: ModuleKey = ModuleKey("command")
    public val PERMISSION: ModuleKey = ModuleKey("permission")
    public val USER: ModuleKey = ModuleKey("user")
    public val TEXT: ModuleKey = ModuleKey("text")
    public val ITEM: ModuleKey = ModuleKey("item")
    public val TELEPORT: ModuleKey = ModuleKey("teleport")
    public val PLAYERSTATE: ModuleKey = ModuleKey("playerstate")
    public val WORLD: ModuleKey = ModuleKey("world")
    public val ECONOMY: ModuleKey = ModuleKey("economy")
    public val HOME: ModuleKey = ModuleKey("home")
    public val KIT: ModuleKey = ModuleKey("kit")
    public val MESSAGING: ModuleKey = ModuleKey("messaging")
    public val WARP: ModuleKey = ModuleKey("warp")
    public val ADMIN: ModuleKey = ModuleKey("admin")
    public val SIGN: ModuleKey = ModuleKey("sign")

    public val ALL: Set<ModuleKey> = setOf(
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

package top.likoslupus.cellulosesz.fabric.module

import top.likoslupus.cellulosesz.api.module.ModuleKeys
import top.likoslupus.cellulosesz.core.module.ModuleCatalog
import top.likoslupus.cellulosesz.core.module.moduleCatalog
import top.likoslupus.cellulosesz.modules.admin.AdminModule
import top.likoslupus.cellulosesz.modules.command.CommandModule
import top.likoslupus.cellulosesz.modules.economy.EconomyModule
import top.likoslupus.cellulosesz.modules.home.HomeModule
import top.likoslupus.cellulosesz.modules.item.ItemModule
import top.likoslupus.cellulosesz.modules.kit.KitModule
import top.likoslupus.cellulosesz.modules.messaging.MessagingModule
import top.likoslupus.cellulosesz.modules.permission.PermissionModule
import top.likoslupus.cellulosesz.modules.playerstate.PlayerStateModule
import top.likoslupus.cellulosesz.modules.sign.SignModule
import top.likoslupus.cellulosesz.modules.teleport.TeleportModule
import top.likoslupus.cellulosesz.modules.text.TextModule
import top.likoslupus.cellulosesz.modules.user.UserModule
import top.likoslupus.cellulosesz.modules.warp.WarpModule
import top.likoslupus.cellulosesz.modules.world.WorldModule

object BuiltInModules {

    @JvmStatic
    fun catalog(): ModuleCatalog = moduleCatalog {
        core(
            key = ModuleKeys.COMMAND,
            name = "Command",
            description = "Registers the CellulosesZ root command and command infrastructure.",
            factory = ::CommandModule,
        )

        core(
            key = ModuleKeys.PERMISSION,
            name = "Permission",
            description = "Permission provider integration and cache.",
            factory = ::PermissionModule,
        )

        feature(
            key = ModuleKeys.USER,
            name = "User",
            description = "User cache and profile foundation.",
            factory = ::UserModule,
        ) {
            requires(
                ModuleKeys.COMMAND,
                ModuleKeys.PERMISSION
            )
        }

        feature(
            key = ModuleKeys.TEXT,
            name = "Text",
            description = "Info, MOTD, rules, and custom paged text commands.",
            factory = ::TextModule,
        ) {
            requires(ModuleKeys.COMMAND)
        }

        feature(
            key = ModuleKeys.ITEM,
            name = "Item",
            description = "Complex item descriptors, inventory utilities, power tools, and unlimited items.",
            factory = ::ItemModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND
            )
        }

        feature(
            key = ModuleKeys.TELEPORT,
            name = "Teleport",
            description = "Teleport, request, back and random teleport services.",
            factory = ::TeleportModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND
            )
            optional(ModuleKeys.PLAYERSTATE)
        }

        feature(
            key = ModuleKeys.PLAYERSTATE,
            name = "PlayerState",
            description = "Persistent player state, AFK automation, player lookup, and per-player world settings.",
            factory = ::PlayerStateModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.PERMISSION,
                ModuleKeys.COMMAND
            )
        }

        feature(
            key = ModuleKeys.WORLD,
            name = "World",
            description = "World time, weather, and entity cleanup commands.",
            factory = ::WorldModule,
        ) {
            requires(ModuleKeys.COMMAND)
        }

        feature(
            key = ModuleKeys.ECONOMY,
            name = "Economy",
            description = "Internal economy, balance, payments, balance top, and worth services.",
            factory = ::EconomyModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND,
                ModuleKeys.ITEM
            )
        }

        feature(
            key = ModuleKeys.HOME,
            name = "Home",
            description = "Player home storage and teleport commands.",
            factory = ::HomeModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.TELEPORT,
                ModuleKeys.COMMAND
            )
        }

        feature(
            key = ModuleKeys.KIT,
            name = "Kit",
            description = "Kit storage, preview, claim, cooldown, and cost services.",
            factory = ::KitModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND
            )
            optional(ModuleKeys.ECONOMY)
        }

        feature(
            key = ModuleKeys.MESSAGING,
            name = "Messaging",
            description = "Private messages, replies, ignore, mail, social spy, helpop, broadcast, and list commands.",
            factory = ::MessagingModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND
            )
        }

        feature(
            key = ModuleKeys.WARP,
            name = "Warp",
            description = "Named shared teleport locations.",
            factory = ::WarpModule,
        ) {
            requires(
                ModuleKeys.TELEPORT,
                ModuleKeys.COMMAND,
                ModuleKeys.USER
            )
        }

        feature(
            key = ModuleKeys.ADMIN,
            name = "Admin",
            description = "Administration, punishments, mute, and jail services.",
            factory = ::AdminModule,
        ) {
            requires(
                ModuleKeys.USER,
                ModuleKeys.COMMAND,
                ModuleKeys.PERMISSION,
                ModuleKeys.TELEPORT
            )
        }

        feature(
            key = ModuleKeys.SIGN,
            name = "Sign",
            description = "Persistent validated interactive sign handlers.",
            factory = ::SignModule,
        ) {
            requires(
                listOf(
                    ModuleKeys.PERMISSION,
                    ModuleKeys.ECONOMY,
                    ModuleKeys.ITEM,
                    ModuleKeys.TELEPORT,
                    ModuleKeys.WARP,
                    ModuleKeys.KIT,
                    ModuleKeys.PLAYERSTATE,
                    ModuleKeys.WORLD,
                    ModuleKeys.TEXT,
                    ModuleKeys.MESSAGING,
                    ModuleKeys.COMMAND,
                )
            )
        }
    }

}

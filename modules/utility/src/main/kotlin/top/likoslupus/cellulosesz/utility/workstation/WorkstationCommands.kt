package top.likoslupus.cellulosesz.utility.workstation

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object WorkstationCommands {

    fun commands(service: WorkstationService): List<CommandDefinition> = listOf(
        command(
            name = "workbench",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.WORKBENCH,
            documentation = "utility/workbench",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.CRAFTING
                        )
                    )
                )
            }
        },

        command(
            name = "anvil",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ANVIL,
            documentation = "utility/anvil",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.ANVIL
                        )
                    )
                )
            }
        },

        command(
            name = "grindstone",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.GRINDSTONE,
            documentation = "utility/grindstone",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.GRINDSTONE
                        )
                    )
                )
            }
        },

        command(
            name = "stonecutter",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.STONECUTTER,
            documentation = "utility/stonecutter",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.STONECUTTER
                        )
                    )
                )
            }
        },

        command(
            name = "loom",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.LOOM,
            documentation = "utility/loom",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.LOOM
                        )
                    )
                )
            }
        },

        command(
            name = "cartographytable",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CARTOGRAPHY_TABLE,
            documentation = "utility/cartographytable",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.CARTOGRAPHY
                        )
                    )
                )
            }
        },

        command(
            name = "smithingtable",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.SMITHING_TABLE,
            documentation = "utility/smithingtable",
        ) {
            executesPlayer {
                source.reply(
                    UtilityMessages.workstation(
                        service.open(
                            playerId,
                            WorkstationType.SMITHING
                        )
                    )
                )
            }
        },
    )

}

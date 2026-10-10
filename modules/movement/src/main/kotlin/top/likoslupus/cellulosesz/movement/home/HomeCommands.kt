package top.likoslupus.cellulosesz.movement.home

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportFeedback

internal object HomeCommands {

    fun commands(
        service: HomeService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "sethome",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_HOME,
            documentation = "movement/sethome",
        ) {
            executesPlayer {
                setHome(
                    context,
                    null,
                    service,
                    backend,
                    kernel
                )
            }
            argument("name", word()) { name ->
                executesPlayer {
                    setHome(
                        context,
                        get(name),
                        service,
                        backend,
                        kernel
                    )
                }
            }
        },
        command(
            name = "home",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.HOME,
            documentation = "movement/home",
        ) {
            executesPlayer {
                goHome(
                    context,
                    null,
                    service,
                    teleports,
                    teleportSettings,
                    kernel
                )
            }
            argument("name", word()) { name ->
                executesPlayer {
                    goHome(
                        context,
                        get(name),
                        service,
                        teleports,
                        teleportSettings,
                        kernel
                    )
                }
            }
        },
        command(
            name = "delhome",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.DEL_HOME,
            documentation = "movement/delhome",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    deleteHome(
                        context,
                        get(name),
                        service,
                        kernel
                    )
                }
            }
        },
        command(
            name = "homes",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.HOMES,
            documentation = "movement/homes",
        ) {
            executesPlayer {
                listHomes(
                    context,
                    service,
                    kernel
                )
            }
        },
    )

    private fun setHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
        backend: TeleportBackend,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val position = backend.position(playerId)
            val message = Messages.prefixed(
                when (position) {
                    null -> "you are no longer online"
                    else -> when (service.set(playerId, rawName, position)) {
                        SetHomeResult.Success -> "home saved"
                        SetHomeResult.InvalidName -> "invalid home name; use a-z, 0-9, '_' or '-', up to 32 characters"
                        SetHomeResult.LimitReached -> "home limit reached"
                    }
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "saving home...")
    }

    private fun goHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = when (val result = service.get(playerId, rawName)) {
                is HomeLookupResult.Found -> {
                    val intent = TeleportIntent(
                        subjectId = playerId,
                        destination = TeleportDestination.Fixed(result.home.position),
                        cause = TeleportCause.HOME,
                        policy = teleportPolicyFor(TeleportCause.HOME, teleportSettings()),
                    )
                    when (val outcome = teleports.execute(intent)) {
                        is TeleportOutcome.Success ->
                            Messages.prefixed("teleported to home '${result.home.name.value}'")

                        else -> TeleportFeedback.failure(outcome)
                            ?: return@launch
                    }
                }

                HomeLookupResult.InvalidName ->
                    Messages.prefixed("invalid home name")

                HomeLookupResult.NotFound ->
                    Messages.prefixed("home not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "teleporting...")
    }

    private fun deleteHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = Messages.prefixed(
                when (service.delete(playerId, rawName)) {
                    DeleteHomeResult.Deleted -> "home '$rawName' deleted"
                    DeleteHomeResult.InvalidName -> "invalid home name"
                    DeleteHomeResult.NotFound -> "home '$rawName' not found"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "deleting home...")
    }

    private fun listHomes(
        context: CommandContext<CommandSourceStack>,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val homes = service.list(playerId).map { it.name.value }
            val message = Messages.prefixed(
                when {
                    homes.isEmpty() -> "you have no homes"
                    else -> "homes: ${homes.joinToString(", ")}"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "loading homes...")
    }

}

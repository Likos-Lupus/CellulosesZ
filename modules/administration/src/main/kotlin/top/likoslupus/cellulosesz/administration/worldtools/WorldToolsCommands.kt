package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.data.worldgen.features.TreeFeatures
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import top.likoslupus.cellulosesz.administration.config.WorldToolsSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/**
 * World/environment tools. Every executor runs on the server thread; each mutates Minecraft state
 * directly through [WorldToolsService] and replies with a semantic notice.
 */
internal object WorldToolsCommands {

    private const val REACH: Double = 8.0

    fun commands(
        service: WorldToolsService,
        personal: PersonalViews,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        timeCommand(service, adventure, presentation),
        weatherCommand(service, adventure, presentation),
        thunderCommand(service, adventure, presentation),
        lightningCommand(service, adventure, presentation),
        fireballCommand(service, adventure, presentation),
        fireworkCommand(service, settings, adventure, presentation),
        nukeCommand(service, settings, adventure, presentation),
        command(
            name = "tree",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TREE,
            documentation = "administration/tree",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(TREE_SUGGESTIONS) }
                executesPlayer {
                    applyTree(
                        source,
                        get(type),
                        false,
                        service,
                        adventure,
                        presentation
                    )
                }
            }
        },
        command(
            name = "bigtree",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BIG_TREE,
            documentation = "administration/bigtree",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(BIG_TREE_SUGGESTIONS) }
                executesPlayer {
                    applyTree(
                        source,
                        get(type),
                        true,
                        service,
                        adventure,
                        presentation
                    )
                }
            }
        },
        command(
            name = "break",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BREAK,
            documentation = "administration/break",
        ) {
            executesPlayer {
                val pos = lookedAtBlock(source)
                    ?: return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.lookAtBlock(),
                    )
                val player = source.player!!
                service.breakBlock(player.level(), pos, player)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.brokeBlock()
                )
            }
        },
        command(
            name = "ice",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.ICE,
            documentation = "administration/ice",
        ) {
            executesPlayer {
                val player = source.player!!
                val frozen = service.freezeWater(
                    player.level(),
                    player.blockPosition(),
                    2
                )
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.iceFrozen(frozen)
                )
            }
        },
        command(
            name = "ext",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.EXT,
            documentation = "administration/ext",
        ) {
            executesPlayer {
                val player = source.player!!
                service.extinguish(player, 3)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.extinguished()
                )
            }
        },
        burnCommand(service, settings, adventure, presentation),
        gcCommand(service, adventure, presentation),
        personalTimeCommand(service, personal, adventure, presentation),
        personalWeatherCommand(service, personal, adventure, presentation),
    )

    // ---------------------------------------------------------------- helpers

    private fun reply(
        source: CommandSourceStack,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        notice: Notice,
    ): Int =
        source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            notice
        )

    private fun resolveTarget(
        source: CommandSourceStack,
        name: String?
    ): ServerPlayer? =
        if (name == null) {
            source.player
        } else {
            PlayerResolver.onlineByName(source.server, name)
        }

    private fun resolveLevels(
        source: CommandSourceStack,
        scope: String?
    ): List<ServerLevel> =
        when {
            scope == null -> listOf(source.level)
            scope.equals("all", ignoreCase = true) -> source.server.allLevels.toList()
            else -> source.server.allLevels.filter {
                val id = it.dimension().identifier()
                id.toString() == scope || id.path == scope
            }
        }

    private fun lookedAtBlock(source: CommandSourceStack): BlockPos? {
        val player = source.player
            ?: return null
        return blockPos(player.pick(REACH, 0f, false))
    }

    private fun blockPos(hit: HitResult?): BlockPos? =
        (hit as? BlockHitResult)?.blockPos

    // ---------------------------------------------------------------- /time

    private fun timeCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "time",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TIME,
            documentation = "administration/time",
        ) {
            executes {
                val source = context.source
                when (val ticks = service.currentTime(source.level)) {
                    null -> reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.timeUnavailable()
                    )

                    else -> reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.timeCurrent(ticks)
                    )
                }
            }
            literal("set") {
                argument("value", word()) { value ->
                    suggests { suggestAll(TIME_SUGGESTIONS) }
                    executes {
                        setTime(
                            context.source,
                            get(value),
                            null,
                            false,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            setTime(
                                context.source,
                                get(value),
                                get(scope),
                                false,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
            literal("add") {
                argument("value", word()) { value ->
                    suggests { suggestAll(TIME_SUGGESTIONS) }
                    executes {
                        setTime(
                            context.source,
                            get(value),
                            null,
                            true,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            setTime(
                                context.source,
                                get(value),
                                get(scope),
                                true,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun setTime(
        source: CommandSourceStack,
        rawValue: String,
        scope: String?,
        add: Boolean,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val ticks = TimeValueParser.parse(rawValue)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeInvalid()
            )
        val targets = resolveLevels(source, scope)
        if (targets.isEmpty()) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeUnavailable()
            )
        }
        targets.forEach {
            when {
                add -> service.addTime(it, ticks)
                else -> service.setTime(it, ticks)
            }
        }
        val notice = when {
            add -> WorldToolsMessages.timeAdded(ticks)
            else -> WorldToolsMessages.timeSet(ticks)
        }
        return reply(source, adventure, presentation, notice)
    }

    // ---------------------------------------------------------------- /weather

    private fun weatherCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "weather",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.WEATHER,
            documentation = "administration/weather",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(WEATHER_SUGGESTIONS) }
                executes {
                    applyWeather(
                        context.source,
                        get(type),
                        null,
                        null,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("duration", integer(1, 1_000_000)) { duration ->
                    executes {
                        applyWeather(
                            context.source,
                            get(type),
                            get(duration).toLong(),
                            null,
                            service,
                            adventure,
                            presentation,
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            applyWeather(
                                context.source,
                                get(type),
                                get(duration).toLong(),
                                get(scope),
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun applyWeather(
        source: CommandSourceStack,
        rawType: String,
        seconds: Long?,
        scope: String?,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val type = WeatherType.parse(rawType)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.weatherInvalid()
            )
        val durationTicks = seconds?.let { (it * 20L).toInt() }
        resolveLevels(source, scope).forEach {
            service.setWeather(it, type, durationTicks)
        }
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.weatherSet(type, seconds)
        )
    }

    // ---------------------------------------------------------------- /thunder

    private fun thunderCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "thunder",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.THUNDER,
            documentation = "administration/thunder",
        ) {
            literal("true") {
                executes {
                    val source = context.source
                    resolveLevels(source, null).forEach {
                        service.setThunder(it, true, null)
                    }
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.thunderSet(true)
                    )
                }
                argument("duration", integer(1, 1_000_000)) { duration ->
                    executes {
                        val source = context.source
                        val ticks = get(duration) * 20
                        resolveLevels(source, null).forEach {
                            service.setThunder(it, true, ticks)
                        }
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.thunderSet(true)
                        )
                    }
                }
            }
            literal("false") {
                executes {
                    val source = context.source
                    resolveLevels(source, null).forEach {
                        service.setThunder(it, false, null)
                    }
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.thunderSet(false)
                    )
                }
            }
        }

    // ---------------------------------------------------------------- /lightning

    private fun lightningCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "lightning",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.LIGHTNING,
            documentation = "administration/lightning",
        ) {
            executesPlayer {
                strike(
                    source,
                    source.player,
                    1,
                    service,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { target ->
                executes {
                    val source = context.source
                    val player = resolveTarget(source, get(target))
                        ?: return@executes reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.playerNotFound(),
                        )
                    strike(
                        source,
                        player,
                        1,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("power", integer(1, 10)) { power ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        strike(
                            source,
                            player,
                            get(power),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        }

    private fun strike(
        source: CommandSourceStack,
        player: ServerPlayer?,
        power: Int,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        var struck = 0
        repeat(power) {
            if (
                service.strikeLightning(
                    player.level(),
                    player.x,
                    player.y,
                    player.z,
                    false
                )
            ) {
                struck++
            }
        }
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.lightningStruck(struck)
        )
    }

    // ---------------------------------------------------------------- /fireball

    private fun fireballCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "fireball",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FIREBALL,
            documentation = "administration/fireball",
        ) {
            executesPlayer {
                launchFireball(
                    source,
                    "large",
                    1.0,
                    service,
                    adventure,
                    presentation
                )
            }
            argument("type", word()) { type ->
                suggests { suggestAll(FIREBALL_SUGGESTIONS) }
                executesPlayer {
                    launchFireball(
                        source,
                        get(type),
                        1.0,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("speed", double(0.1, 10.0)) { speed ->
                    executesPlayer {
                        launchFireball(
                            source,
                            get(type),
                            get(speed),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        }

    private fun launchFireball(
        source: CommandSourceStack,
        rawType: String,
        speed: Double,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val type = FireballType.parse(rawType)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.fireballInvalid()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        service.launchFireball(player, type, speed)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.fireballLaunched(type)
        )
    }

    // ---------------------------------------------------------------- /firework

    private fun fireworkCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "firework",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FIREWORK,
            documentation = "administration/firework",
        ) {
            argument("power", integer(1, 32)) { power ->
                executesPlayer {
                    val capped = minOf(get(power), settings().maxFireworkPower)
                    if (service.setFireworkPower(source.player!!, capped)) {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkPower(capped)
                        )
                    } else {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkNoRocket()
                        )
                    }
                }
            }
            literal("clear") {
                executesPlayer {
                    if (service.clearFirework(source.player!!)) {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkCleared()
                        )
                    } else {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkNoRocket()
                        )
                    }
                }
            }
            literal("fire") {
                executesPlayer {
                    val launched = service.launchFireworks(source.player!!, 1, 1)
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.fireworkFired(launched)
                    )
                }
                argument("amount", integer(1, 128)) { amount ->
                    executesPlayer {
                        val capped = minOf(get(amount), settings().maxFireworkAmount)
                        val launched = service.launchFireworks(
                            source.player!!,
                            1,
                            capped
                        )
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkFired(launched)
                        )
                    }
                }
            }
        }

    // ---------------------------------------------------------------- /nuke

    private fun nukeCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "nuke",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.NUKE,
            documentation = "administration/nuke",
        ) {
            executesPlayer {
                detonate(
                    source,
                    source.player,
                    service,
                    settings,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { target ->
                executes {
                    val source = context.source
                    detonate(
                        source,
                        resolveTarget(source, get(target)),
                        service,
                        settings,
                        adventure,
                        presentation,
                    )
                }
            }
        }

    private fun detonate(
        source: CommandSourceStack,
        player: ServerPlayer?,
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        service.nuke(
            player.level(),
            player.x,
            player.y,
            player.z,
            settings().maxNukeRadius
        )
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.nukeDetonated()
        )
    }

    // ---------------------------------------------------------------- /tree

    private fun applyTree(
        source: CommandSourceStack,
        rawType: String,
        big: Boolean,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val key = treeFeature(rawType, big)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.treeInvalid()
            )
        val pos = lookedAtBlock(source)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.lookAtBlock()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        return when {
            service.growTree(player.level(), pos, key) ->
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.treeGrown(rawType)
                )

            else ->
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.treeFailed()
                )
        }
    }

    // ---------------------------------------------------------------- /burn

    private fun burnCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "burn",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BURN,
            documentation = "administration/burn",
        ) {
            argument("player", word()) { target ->
                argument("seconds", double(0.1, 3_600.0)) { seconds ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        val capped = minOf(get(seconds), settings().maxBurnSeconds)
                        service.burn(player, capped)
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.burned(
                                player.gameProfile.name,
                                capped
                            ),
                        )
                    }
                }
            }
        }

    // ---------------------------------------------------------------- /gc

    private fun gcCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "gc",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.GC,
            documentation = "administration/gc",
        ) {
            executes {
                val source = context.source
                source.replyDetail(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    WorldToolsMessages.stats(service.stats(source.server)),
                )
            }
        }

    // ---------------------------------------------------------------- /ptime

    private fun personalTimeCommand(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "ptime",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.P_TIME,
            documentation = "administration/ptime",
        ) {
            executesPlayer {
                listPersonalTime(
                    source,
                    personal,
                    adventure,
                    presentation
                )
            }
            literal("list") {
                executesPlayer {
                    listPersonalTime(
                        source,
                        personal,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_TIME_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        listPersonalTime(
                            source,
                            personal,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            literal("reset") {
                executesPlayer {
                    resetPersonalTime(
                        source,
                        personal,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_TIME_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        resetPersonalTime(
                            source,
                            personal,
                            service,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            argument("value", word()) { value ->
                suggests { suggestAll(PERSONAL_TIME_SUGGESTIONS) }
                executesPlayer {
                    setPersonalTime(
                        source,
                        personal,
                        service,
                        get(value),
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_TIME_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        setPersonalTime(
                            source,
                            personal,
                            service,
                            get(value),
                            adventure,
                            presentation,
                            player,
                        )
                    }
                }
            }
        }

    private fun listPersonalTime(
        source: CommandSourceStack,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val notice = when (val view = personal.time(player.uuid)) {
            null -> WorldToolsMessages.personalTimeNone()
            else -> WorldToolsMessages.personalTimeList(formatPersonalTime(view))
        }
        return reply(
            source,
            adventure,
            presentation,
            notice
        )
    }

    private fun setPersonalTime(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        raw: String,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val view = parsePersonalTime(raw)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeInvalid()
            )
        personal.setTime(player.uuid, view)
        service.sendPersonalTime(player, view)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalTimeSet(formatPersonalTime(view)),
        )
    }

    private fun resetPersonalTime(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        personal.clearTime(player.uuid)
        service.currentTime(player.level())?.let {
            service.sendPersonalTime(
                player,
                PersonalTimeView(it, false)
            )
        }
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalTimeReset()
        )
    }

    private fun parsePersonalTime(raw: String): PersonalTimeView? {
        val frozen = raw.startsWith("@")
        val ticks = TimeValueParser.parse(if (frozen) raw.substring(1) else raw)
            ?: return null
        return PersonalTimeView(ticks, frozen)
    }

    private fun formatPersonalTime(view: PersonalTimeView): String =
        when {
            view.frozen -> "${view.ticks} ticks (fixed)"
            else -> "${view.ticks} ticks"
        }

    // ---------------------------------------------------------------- /pweather

    private fun personalWeatherCommand(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "pweather",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.P_WEATHER,
            documentation = "administration/pweather",
        ) {
            executesPlayer {
                listPersonalWeather(
                    source,
                    personal,
                    adventure,
                    presentation
                )
            }
            literal("list") {
                executesPlayer {
                    listPersonalWeather(
                        source,
                        personal,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        listPersonalWeather(
                            source,
                            personal,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            literal("reset") {
                executesPlayer {
                    resetPersonalWeather(
                        source,
                        personal,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        resetPersonalWeather(
                            source,
                            personal,
                            service,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            argument("value", word()) { value ->
                suggests { suggestAll(WEATHER_SUGGESTIONS) }
                executesPlayer {
                    setPersonalWeather(
                        source,
                        personal,
                        service,
                        get(value),
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        setPersonalWeather(
                            source,
                            personal,
                            service,
                            get(value),
                            adventure,
                            presentation,
                            player,
                        )
                    }
                }
            }
        }

    private fun listPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val notice = when (val weather = personal.weather(player.uuid)) {
            null -> WorldToolsMessages.personalWeatherNone()
            else -> WorldToolsMessages.personalWeatherList(weather.name.lowercase())
        }
        return reply(source, adventure, presentation, notice)
    }

    private fun setPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        raw: String,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val weather = PersonalWeather.parse(raw)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.weatherInvalid()
            )
        personal.setWeather(player.uuid, weather)
        service.sendPersonalWeather(player, weather)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalWeatherSet(weather.name.lowercase()),
        )
    }

    private fun resetPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        personal.clearWeather(player.uuid)
        val level = player.level()
        val weather = when {
            level.isThundering -> PersonalWeather.THUNDER
            level.isRaining -> PersonalWeather.RAIN
            else -> PersonalWeather.CLEAR
        }
        service.sendPersonalWeather(player, weather)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalWeatherReset()
        )
    }

    // ---------------------------------------------------------------- tree registry

    private fun treeFeature(raw: String, big: Boolean): ResourceKey<ConfiguredFeature<*, *>>? =
        when {
            big -> when (raw.lowercase()) {
                "tree", "oak" -> TreeFeatures.FANCY_OAK
                "redwood", "spruce" -> TreeFeatures.MEGA_SPRUCE
                "jungle" -> TreeFeatures.MEGA_JUNGLE_TREE
                "darkoak", "dark_oak" -> TreeFeatures.DARK_OAK
                else -> null
            }

            else -> when (raw.lowercase()) {
                "tree", "oak" -> TreeFeatures.OAK
                "birch" -> TreeFeatures.BIRCH
                "redwood", "spruce" -> TreeFeatures.MEGA_SPRUCE
                "redmushroom", "red_mushroom" -> TreeFeatures.HUGE_RED_MUSHROOM
                "brownmushroom", "brown_mushroom" -> TreeFeatures.HUGE_BROWN_MUSHROOM
                "jungle" -> TreeFeatures.JUNGLE_TREE
                "junglebush", "jungle_bush" -> TreeFeatures.JUNGLE_BUSH
                "swamp", "swamp_oak" -> TreeFeatures.SWAMP_OAK
                else -> null
            }
        }

    private val TIME_SUGGESTIONS: List<String> = listOf(
        "day",
        "noon",
        "night",
        "midnight",
        "dawn",
        "dusk",
        "17:30",
        "4pm",
        "4000ticks",
    )

    private val PERSONAL_TIME_SUGGESTIONS: List<String> =
        TIME_SUGGESTIONS + TIME_SUGGESTIONS.map { "@$it" }

    private val WEATHER_SUGGESTIONS: List<String> = listOf(
        "clear",
        "rain",
        "thunder"
    )

    private val FIREBALL_SUGGESTIONS: List<String> = listOf(
        "small",
        "large",
        "dragon",
        "wither"
    )

    private val TREE_SUGGESTIONS: List<String> = listOf(
        "tree",
        "birch",
        "redwood",
        "redmushroom",
        "brownmushroom",
        "jungle",
        "junglebush",
        "swamp",
    )

    private val BIG_TREE_SUGGESTIONS: List<String> = listOf(
        "tree",
        "redwood",
        "jungle",
        "darkoak",
    )

}

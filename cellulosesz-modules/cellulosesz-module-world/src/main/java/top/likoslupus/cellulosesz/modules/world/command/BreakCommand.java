package top.likoslupus.cellulosesz.modules.world.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BreakCommand implements CommandContributor {

    private final WorldPlatformService worlds;
    private final WorldRuntimeSettings config;

    public BreakCommand(
            WorldPlatformService worlds,
            WorldRuntimeSettings config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "break",
                "cellulosesz.command.break",
                CommandSourceKind.PLAYER_ONLY
        );
        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.break",
                "/break",
                Commands.literal("break")
                        .executes(command -> WorldCommandSupport.sync(
                                context,
                                command,
                                descriptor,
                                "break target",
                                policy -> {
                                    var player = WorldCommandSupport.current(policy);
                                    return player
                                            .<PlatformResult<?>>map(value -> worlds.breakTarget(
                                                    value,
                                                    config.targetDistance(),
                                                    policy.hasPermission(
                                                            "cellulosesz.command.break.unbreakable")
                                            ))
                                            .orElseGet(() -> PlatformResult.failure(
                                                    PlatformOperationStatus.INVALID_SOURCE,
                                                    "player-only"
                                            ));
                                }
                        ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

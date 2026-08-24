package top.likoslupus.cellulosesz.modules.world.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.common.entity.TemporaryMobRequest;
import top.likoslupus.cellulosesz.common.entity.TemporaryMobType;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class KittyCannonCommand implements CommandContributor {

    private final EntityPlatformService entities;
    private final WorldRuntimeSettings config;

    public KittyCannonCommand(
            EntityPlatformService entities,
            WorldRuntimeSettings config
    ) {
        this.entities = requireNonNull(entities, "entities");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "kittycannon",
                "cellulosesz.command.kittycannon",
                CommandSourceKind.PLAYER_ONLY
        );
        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.kittycannon",
                "/kittycannon",
                Commands.literal("kittycannon")
                        .executes(command -> WorldCommandSupport.sync(
                                context,
                                command,
                                descriptor,
                                "kittycannon",
                                policy -> {
                                    var player = WorldCommandSupport.current(policy);
                                    return player
                                            .<PlatformResult<?>>map(value -> entities.launchTemporaryMob(
                                                    new TemporaryMobRequest(
                                                            value,
                                                            TemporaryMobType.CAT,
                                                            config.temporaryMobSpeed(),
                                                            config.temporaryMobLifetimeTicks(),
                                                            config.temporaryMobExplosionPower(),
                                                            false
                                                    )
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

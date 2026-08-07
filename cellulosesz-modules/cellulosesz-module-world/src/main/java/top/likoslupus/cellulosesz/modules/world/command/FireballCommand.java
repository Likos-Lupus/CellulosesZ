package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.ProjectileRequest;
import top.likoslupus.cellulosesz.api.entity.ProjectileType;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.world.command.argument.ProjectileTypes;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class FireballCommand implements CommandContributor {

    private final EntityPlatformService entities;
    private final WorldRuntimeSettings config;

    public FireballCommand(
            EntityPlatformService entities,
            WorldRuntimeSettings config
    ) {
        this.entities = requireNonNull(entities, "entities");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "fireball",
                "cellulosesz.command.fireball",
                CommandSourceKind.PLAYER_ONLY
        );
        var type = Commands.argument("projectile", StringArgumentType.word())
                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                        ProjectileTypes.suggestions(),
                        builder
                ))
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ProjectileTypes.parse(
                                StringArgumentType.getString(command, "projectile")
                        ),
                        config.defaultProjectileSpeed()
                ))
                .then(Commands.argument(
                                        "speed",
                                        DoubleArgumentType.doubleArg(0.01D, config.maximumProjectileSpeed())
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        ProjectileTypes.parse(
                                                StringArgumentType.getString(command, "projectile")
                                        ),
                                        DoubleArgumentType.getDouble(command, "speed")
                                ))
                );

        var root = Commands.literal("fireball")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ProjectileType.FIREBALL,
                        config.defaultProjectileSpeed()
                ))
                .then(type);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.fireball",
                "/fireball [projectile] [speed]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ProjectileType type,
            double speed
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "fireball " + type,
                policy -> {
                    var permission = "cellulosesz.command.fireball.projectile."
                            + type.name().toLowerCase(Locale.ROOT)
                            .replace("experience_bottle", "expbottle")
                            .replace("splash_potion", "splashpotion")
                            .replace("lingering_potion", "lingeringpotion");
                    if (!policy.hasPermission(permission)) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                permission
                        );
                    }

                    return WorldCommandSupport.current(policy)
                            .<PlatformResult<?>>map(value -> entities.launchProjectile(
                                    new ProjectileRequest(
                                            value,
                                            type,
                                            speed,
                                            config.projectileLifetimeTicks()
                                    )
                            ))
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

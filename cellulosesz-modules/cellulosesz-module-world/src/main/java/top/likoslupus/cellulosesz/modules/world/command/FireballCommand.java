package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.ProjectileRequest;
import top.likoslupus.cellulosesz.api.entity.ProjectileType;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FireballCommand implements CellCommand {

    private final PlatformService platform;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public FireballCommand(PlatformService platform, EntityPlatformService entities, WorldConfig config) {
        this.platform = platform;
        this.entities = entities;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.fireball";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/fireball [fireball|small|large|arrow|skull|egg|snowball|expbottle|dragon|splashpotion|lingeringpotion|trident] [speed]";
    }

    @Override
    public String name() {
        return "fireball";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 2) return usage(invocation);
        var type = invocation.args().length == 0
                ? Optional.of(ProjectileType.FIREBALL)
                : parse(invocation.args()[0]);
        if (type.isEmpty()) return usage(invocation);
        var permission = "cellulosesz.command.fireball.projectile." + token(type.orElseThrow());
        if (!invocation.hasPermission(permission)) {
            invocation.errorKey("commands.common.no-permission");
            return 0;
        }
        var speed = config.defaultProjectileSpeed;
        if (invocation.args().length == 2) {
            try {
                speed = Double.parseDouble(invocation.args()[1]);
                if (!Double.isFinite(speed) || speed <= 0.0D || speed > config.maximumProjectileSpeed) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException failure) {
                invocation.errorKey("commands.world.fireball.invalid-speed", Map.of("maximum", config.maximumProjectileSpeed));
                return 0;
            }
        }
        var result = entities.launchProjectile(new ProjectileRequest(
                platform.player(invocation).orElseThrow(),
                type.orElseThrow(),
                speed,
                config.projectileLifetimeTicks
        ));
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.fireball.success", Map.of(
                "type", token(type.orElseThrow()),
                "speed", speed
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.fireball.usage", Map.of("usage", usage()));
        return 0;
    }

    static Optional<ProjectileType> parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "fireball" -> Optional.of(ProjectileType.FIREBALL);
            case "small" -> Optional.of(ProjectileType.SMALL);
            case "large" -> Optional.of(ProjectileType.LARGE);
            case "arrow" -> Optional.of(ProjectileType.ARROW);
            case "skull" -> Optional.of(ProjectileType.SKULL);
            case "egg" -> Optional.of(ProjectileType.EGG);
            case "snowball" -> Optional.of(ProjectileType.SNOWBALL);
            case "expbottle" -> Optional.of(ProjectileType.EXPERIENCE_BOTTLE);
            case "dragon" -> Optional.of(ProjectileType.DRAGON);
            case "splashpotion" -> Optional.of(ProjectileType.SPLASH_POTION);
            case "lingeringpotion" -> Optional.of(ProjectileType.LINGERING_POTION);
            case "trident" -> Optional.of(ProjectileType.TRIDENT);
            default -> Optional.empty();
        };
    }

    static String token(ProjectileType type) {
        return switch (type) {
            case FIREBALL -> "fireball";
            case SMALL -> "small";
            case LARGE -> "large";
            case ARROW -> "arrow";
            case SKULL -> "skull";
            case EGG -> "egg";
            case SNOWBALL -> "snowball";
            case EXPERIENCE_BOTTLE -> "expbottle";
            case DRAGON -> "dragon";
            case SPLASH_POTION -> "splashpotion";
            case LINGERING_POTION -> "lingeringpotion";
            case TRIDENT -> "trident";
        };
    }

}

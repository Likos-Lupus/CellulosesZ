package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.common.entity.ProjectileType;

import java.util.List;
import java.util.Locale;

public final class ProjectileTypes {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported projectile type")
    );
    private static final List<String> SUGGESTIONS = List.of(
            "fireball", "small", "large", "arrow", "skull", "egg", "snowball",
            "expbottle", "dragon", "splashpotion", "lingeringpotion", "trident"
    );

    private ProjectileTypes() {
    }

    public static ProjectileType parse(String raw) throws CommandSyntaxException {
        var type = switch (raw.toLowerCase(Locale.ROOT)) {
            case "fireball" -> ProjectileType.FIREBALL;
            case "small" -> ProjectileType.SMALL;
            case "large" -> ProjectileType.LARGE;
            case "arrow" -> ProjectileType.ARROW;
            case "skull" -> ProjectileType.SKULL;
            case "egg" -> ProjectileType.EGG;
            case "snowball" -> ProjectileType.SNOWBALL;
            case "expbottle" -> ProjectileType.EXPERIENCE_BOTTLE;
            case "dragon" -> ProjectileType.DRAGON;
            case "splashpotion" -> ProjectileType.SPLASH_POTION;
            case "lingeringpotion" -> ProjectileType.LINGERING_POTION;
            case "trident" -> ProjectileType.TRIDENT;
            default -> null;
        };

        if (type == null) {
            throw INVALID.create();
        }

        return type;
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}

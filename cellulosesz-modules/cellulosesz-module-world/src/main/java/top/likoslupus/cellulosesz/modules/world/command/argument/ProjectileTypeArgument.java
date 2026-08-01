package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.entity.ProjectileType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class ProjectileTypeArgument implements ArgumentType<ProjectileType> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported projectile type")
    );

    public static ProjectileTypeArgument projectileType() {
        return new ProjectileTypeArgument();
    }

    public static ProjectileType get(CommandContext<?> context, String name) {
        return context.getArgument(name, ProjectileType.class);
    }

    @Override
    public ProjectileType parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        var type = switch (value) {
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
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
        return type;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "fireball",
                "arrow",
                "trident"
        );
    }

}

package top.likoslupus.cellulosesz.core.command.spec;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameter;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameterType;
import top.likoslupus.cellulosesz.api.command.spec.CommandRoute;
import top.likoslupus.cellulosesz.api.command.spec.CommandSpec;
import top.likoslupus.cellulosesz.core.command.DirectCommandMigrationIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.command.spec.CommandParameter.*;
import static top.likoslupus.cellulosesz.api.command.spec.CommandParameterType.*;

/**
 * Converts the platform-neutral command contract into a Brigadier-friendly route model. Explicit routes are used for
 * every command whose usage text is ambiguous; inference is deliberately conservative and never turns an arbitrary
 * item/name token into a greedy tail.
 */
public final class DefaultCommandSpecFactory {

    public CommandSpec spec(CellCommand command) {
        if (DirectCommandMigrationIndex.contains(command.name())) {
            throw new IllegalArgumentException("Direct command must not request a legacy spec: " + command.name());
        }
        var declared = requireNonNull(
                command.commandSpec(),
                "Command spec must not be null: " + command.name()
        );
        if (!declared.automatic()) {
            return declared;
        }
        return explicit(command);
    }

    private CommandSpec explicit(CellCommand command) {
        return switch (command.name().toLowerCase(Locale.ROOT)) {
            // Core and economy
            case "gc", "beezooka", "kittycannon" -> routes(route());
            case "clearinventory" -> routes(
                    route(),
                    route(choice("target", false, "self")),
                    route(choice("target", false, "self"), required("filter", WORD)),
                    route(choice("target", false, "self"), required("filter", WORD), required("amount", INTEGER)),
                    route(required("player", PLAYER)),
                    route(required("player", PLAYER), required("filter", WORD)),
                    route(required("player", PLAYER), required("filter", WORD), required("amount", INTEGER)),
                    route(choice("target", false, "*")),
                    route(choice("target", false, "*"), required("filter", WORD)),
                    route(choice("target", false, "*"), required("filter", WORD), required("amount", INTEGER)),
                    route(choice("action", false, "confirm"), required("token", WORD))
            );
            case "clearinventoryconfirmtoggle", "powertooltoggle" -> routes(
                    route(),
                    route(choice("state", false, "on", "off"))
            );
            case "more" -> routes(route(), route(required("amount", INTEGER)));
            case "hat" -> routes(route(), route(choice("action", false, "remove")));
            case "powertoollist" -> routes(route(), route(required("page", INTEGER)));
            case "itemdb" -> routes(route(), route(required("item", ITEM)));
            case "condense" -> routes(route(), route(required("item", ITEM)));
            case "recipe" -> routes(
                    route(required("item", WORD)),
                    route(required("item", WORD), required("number", INTEGER))
            );
            case "book" -> routes(
                    route(),
                    route(choice("action", false, "title"), required("title", GREEDY_STRING)),
                    route(choice("action", false, "author"), required("author", GREEDY_STRING))
            );
            case "skull" -> routes(
                    route(),
                    route(required("owner", STRING)),
                    route(required("owner", STRING), required("player", PLAYER))
            );
            case "break", "antioch" -> command.name().equalsIgnoreCase("antioch")
                    ? routes(route(), route(optional("message", GREEDY_STRING)))
                    : routes(route());
            case "editsign" -> routes(
                    route(choice("action", false, "set"), required("line", INTEGER), required("text", GREEDY_STRING)),
                    route(choice("action", false, "clear"), required("line", INTEGER)),
                    route(choice("action", false, "copy", "paste"))
            );
            case "spawner" -> routes(
                    route(required("entity", WORD)),
                    route(required("entity", WORD), required("delay", INTEGER))
            );
            case "spawnmob" -> routes(
                    route(required("entity", WORD)),
                    route(required("entity", WORD), required("amount", INTEGER)),
                    route(required("entity", WORD), required("amount", INTEGER), required("player", PLAYER))
            );
            case "tree" -> routes(
                    route(),
                    route(choice("type", false, "tree", "oak", "birch", "redwood", "spruce", "redmushroom", "brownmushroom", "jungle", "junglebush", "swamp"))
            );
            case "bigtree" -> routes(
                    route(),
                    route(choice("type", false, "tree", "oak", "redwood", "spruce", "jungle", "darkoak"))
            );
            case "thunder" -> routes(
                    route(required("enabled", BOOLEAN)),
                    route(required("enabled", BOOLEAN), required("duration", INTEGER))
            );
            case "lightning" -> routes(
                    route(),
                    route(required("player", PLAYER)),
                    route(required("player", PLAYER), required("damage", DOUBLE))
            );
            case "fireball" -> routes(
                    route(),
                    route(choice("projectile", false, "fireball", "small", "large", "arrow", "skull", "egg", "snowball", "expbottle", "dragon", "splashpotion", "lingeringpotion", "trident")),
                    route(choice("projectile", false, "fireball", "small", "large", "arrow", "skull", "egg", "snowball", "expbottle", "dragon", "splashpotion", "lingeringpotion", "trident"), required("speed", DOUBLE))
            );
            case "nuke" -> routes(route(), route(required("player", PLAYER)));

            // Items and kits. ITEM is a greedy Minecraft item descriptor and must remain last.
            case "give" -> routes(route(
                    required("player", PLAYER),
                    required("item", ITEM)
            ));
            case "item" -> routes(route(
                    required("item", ITEM)
            ));
            case "enchant" -> routes(route(
                    required("enchantment", WORD),
                    optional("level", INTEGER)
            ));
            case "repair" -> routes(
                    route(),
                    route(
                            choice(
                                    "scope",
                                    false,
                                    "hand", "all"
                            )
                    )
            );
            case "invsee" -> routes(route(
                    required("player", PLAYER)
            ));
            case "enderchest" -> routes(
                    route(),
                    route(
                            required("player", PLAYER)
                    )
            );
            case "powertool" -> routes(
                    route(),
                    route(
                            required("command", GREEDY_STRING)
                    )
            );
            case "unlimited" -> routes(
                    route(),
                    route(
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "enable", "disable", "true", "false", "list", "clear"
                            )
                    )
            );
            case "itemname", "itemlore" -> routes(
                    route(),
                    route(
                            required("value", GREEDY_STRING)
                    )
            );
            case "potion" -> routes(
                    route(
                            required("effect", WORD)
                    ),
                    route(
                            required("effect", WORD),
                            required("duration", INTEGER)
                    ),
                    route(
                            required("effect", WORD),
                            required("duration", INTEGER),
                            required("amplifier", INTEGER)
                    )
            );
            case "firework" -> routes(
                    route(
                            choice(
                                    "action",
                                    false,
                                    "clear"
                            )
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "power"
                            ),
                            required("power", INTEGER)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "effect"
                            ),
                            required("shape", WORD),
                            required("color", WORD)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "effect"
                            ),
                            required("shape", WORD),
                            required("color", WORD),
                            required("fade", WORD)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "effect"
                            ),
                            required("shape", WORD),
                            required("color", WORD),
                            required("fade", WORD),
                            required("flags", WORD)
                    )
            );
            case "anvil", "cartographytable", "grindstone", "loom", "smithingtable", "workbench", "disposal",
                 "stonecutter" -> routes(route());

            // Messaging
            case "remove" -> routes(
                    route(
                            required("selector", WORD)
                    ),
                    route(
                            required("selector", WORD),
                            required("radius", INTEGER)
                    )
            );
            case "time" -> routes(
                    route(
                            required("time", WORD)
                    ),
                    route(
                            required("time", WORD),
                            required("world", WORLD)
                    )
            );
            case "backup" -> routes(route());
            case "weather" -> routes(
                    route(
                            choice(
                                    "weather",
                                    false,
                                    "clear", "rain", "thunder"
                            )
                    ),
                    route(
                            choice(
                                    "weather",
                                    false,
                                    "clear", "rain", "thunder"),
                            required("seconds", INTEGER)
                    ),
                    route(
                            choice(
                                    "weather",
                                    false,
                                    "clear",
                                    "rain",
                                    "thunder"
                            ),
                            required("seconds", INTEGER),
                            required("world", WORLD)
                    )
            );
            default -> infer(command);
        };
    }

    private CommandSpec routes(CommandRoute... routes) {
        return CommandSpec.of(routes);
    }

    private CommandRoute route(CommandParameter... parameters) {
        return CommandRoute.of(parameters);
    }

    private CommandSpec infer(CellCommand command) {
        var usage = command.usage();
        if (usage.isBlank() || usage.equals("/" + command.name())) {
            return routes(route());
        }

        var routes = new ArrayList<CommandRoute>();
        Arrays.stream(usage.split("\\s+(?:\\||or|或)\\s+"))
                .map(String::trim)
                .forEach(normalized -> {
                    var firstSpace = normalized.indexOf(' ');
                    if (firstSpace < 0) {
                        routes.add(route());
                        return;
                    }
                    routes.add(new CommandRoute(parseTokens(normalized.substring(firstSpace + 1).trim())));
                });
        return new CommandSpec(routes.isEmpty() ? List.of(route()) : routes);
    }

    private List<CommandParameter> parseTokens(String input) {
        var result = new ArrayList<CommandParameter>();
        for (var raw : input.split("\\s+")) {
            var token = raw.trim();
            if (token.isBlank() || token.equals("...") || token.startsWith("/")) continue;

            var optional = token.startsWith("[");
            if (!(optional || token.startsWith("<")) || token.length() < 2) continue;

            token = token
                    .substring(1, token.length() - 1)
                    .replace("...", "");
            var choices = token.contains("|")
                    ? List.of(token.split("\\|"))
                    : List.<String>of();
            if (!choices.isEmpty() && choices.stream().allMatch(this::literalChoice)) {
                result.add(new CommandParameter(semanticName(choices), WORD, optional, choices));
                continue;
            }

            result.add(new CommandParameter(token, inferredType(token), optional, List.of()));
        }
        return result;
    }

    private boolean literalChoice(String value) {
        return value.matches("[A-Za-z0-9_.:-]+");
    }

    private String semanticName(List<String> choices) {
        return choices.stream()
                .map(String::toLowerCase)
                .anyMatch(value -> value.equals("on") || value.equals("off"))
                ? "state"
                : "value";
    }

    private CommandParameterType inferredType(String rawName) {
        var name = rawName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");

        return switch (name) {
            case "players" -> PLAYERS;
            case String s when s.contains("player") || s.equals("target") || s.equals("user") -> KNOWN_PLAYER;
            case "world", "dimension" -> WORLD;
            case "message", "reason", "action", "command", "components" -> GREEDY_STRING;
            case "count", "page", "level", "radius", "seconds", "ticks", "limit" -> INTEGER;
            case "amount", "value", "x", "y", "z" -> DOUBLE;
            case "enabled", "boolean" -> BOOLEAN;
            case "name", "old", "new", "kit", "home", "warp", "jail", "nickname" -> STRING;
            default -> WORD;
        };
    }

}

package top.likoslupus.cellulosesz.core.command.spec;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameter;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameterType;
import top.likoslupus.cellulosesz.api.command.spec.CommandRoute;
import top.likoslupus.cellulosesz.api.command.spec.CommandSpec;

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
            // Administration
            case "ban" -> routes(route(
                    required("player", KNOWN_PLAYER),
                    optional("reason", GREEDY_STRING)
            ));
            case "kick" -> routes(route(
                    required("player", PLAYER),
                    optional("reason", GREEDY_STRING)
            ));
            case "tempban" -> routes(route(
                    required("player", KNOWN_PLAYER),
                    required("duration", WORD),
                    optional("reason", GREEDY_STRING)
            ));
            case "mute" -> routes(route(
                    required("player", KNOWN_PLAYER),
                    optional("duration", WORD),
                    optional("reason", GREEDY_STRING)
            ));
            case "jail" -> routes(route(
                    required("player", PLAYER),
                    required("jail", STRING),
                    optional("duration", WORD),
                    optional("reason", GREEDY_STRING)
            ));
            case "setjail", "deljail" -> routes(route(
                    required("name", STRING)
            ));
            case "banip" -> routes(route(
                    required("address", WORD),
                    optional("reason", GREEDY_STRING)
            ));
            case "tempbanip" -> routes(route(
                    required("address", WORD),
                    required("duration", WORD),
                    optional("reason", GREEDY_STRING)
            ));
            case "unban" -> routes(route(
                    required("player", KNOWN_PLAYER)
            ));
            case "unbanip" -> routes(route(
                    required("address", WORD)
            ));
            case "kickall" -> routes(
                    route(),
                    route(
                            required("reason", GREEDY_STRING)
                    )
            );

            // Core and economy
            case "cellulosesz" -> routes(
                    route(),
                    route(choice(
                            "action",
                            false,
                            "version",
                            "reload",
                            "modules",
                            "debug"
                    ))
            );
            case "help" -> routes(
                    route(),
                    route(
                            required("page", INTEGER)
                    ),
                    route(
                            required("query", STRING)
                    ),
                    route(
                            required("query", STRING),
                            required("page", INTEGER)
                    )
            );
            case "balance" -> routes(
                    route(),
                    route(
                            required("player", KNOWN_PLAYER)
                    )
            );
            case "balancetop" -> routes(
                    route(),
                    route(
                            required("page", INTEGER)
                    ),
                    route(
                            required("page", INTEGER),
                            required("minimum", DOUBLE)
                    ),
                    route(
                            required("page", INTEGER),
                            required("minimum", DOUBLE),
                            required("maximum", DOUBLE)
                    )
            );
            case "eco" -> routes(route(
                    choice(
                            "action",
                            false,
                            "give", "take", "set"
                    ),
                    required("player", KNOWN_PLAYER),
                    required("amount", DOUBLE)
            ));
            case "pay" -> routes(route(
                    required("player", WORD),
                    required("amount", DOUBLE),
                    optional("confirmation", WORD)
            ));
            case "sell" -> routes(
                    route(
                            choice(
                                    "scope",
                                    false,
                                    "hand", "all"
                            )
                    ),
                    route(
                            choice(
                                    "scope",
                                    false,
                                    "hand"
                            ),
                            required("amount", INTEGER)
                    ),
                    route(
                            required("item", WORD)
                    ),
                    route(
                            required("item", WORD),
                            required("amount", INTEGER)
                    )
            );
            case "setworth" -> routes(route(
                    required("item", WORD),
                    required("value", WORD)
            ));
            case "worth" -> routes(
                    route(),
                    route(
                            choice(
                                    "scope",
                                    false,
                                    "hand",
                                    "inventory"
                            )
                    ),
                    route(
                            required("item", WORD)
                    ),
                    route(
                            required("item", WORD),
                            required("amount", INTEGER)
                    )
            );

            // Homes and warps
            case "home" -> routes(
                    route(),
                    route(
                            required("name", STRING)
                    )
            );
            case "sethome" -> routes(
                    route(),
                    route(
                            required("name", STRING)
                    )
            );
            case "delhome" -> routes(route(
                    required("name", STRING)
            ));
            case "renamehome" -> routes(route(
                    required("old", STRING),
                    required("new", STRING)
            ));
            case "warp" -> routes(
                    route(),
                    route(
                            required("name", STRING)
                    )
            );
            case "setwarp", "delwarp", "warpinfo" -> routes(route(
                    required("name", STRING)
            ));
            case "ping" -> routes(
                    route(),
                    route(optional("message", GREEDY_STRING))
            );
            case "compass", "depth", "gc", "rest", "suicide", "beezooka", "kittycannon" -> routes(route());
            case "getpos" -> routes(route(), route(required("player", PLAYER)));
            case "realname" -> routes(route(required("nickname", STRING)));
            case "exp" -> routes(
                    route(choice("action", false, "show")),
                    route(choice("action", false, "show"), required("player", PLAYER)),
                    route(choice("action", false, "reset")),
                    route(choice("action", false, "reset"), required("player", PLAYER)),
                    route(choice("action", false, "set", "give", "take"), required("amount", WORD)),
                    route(choice("action", false, "set", "give", "take"), required("player", PLAYER), required("amount", WORD))
            );
            case "burn" -> routes(route(required("player", PLAYER), required("seconds", INTEGER)));
            case "ext", "ice" -> routes(route(), route(required("player", PLAYER)));
            case "kill" -> routes(route(required("player", PLAYER)));
            case "sudo" -> routes(route(required("player", PLAYER), required("command", GREEDY_STRING)));
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
            case "createkit" -> routes(route(
                    required("name", STRING),
                    required("cooldown", WORD)
            ));
            case "delkit", "showkit" -> routes(route(
                    required("name", STRING)
            ));
            case "kit" -> routes(
                    route(),
                    route(
                            required("name", STRING)
                    )
            );
            case "kitreset" -> routes(
                    route(
                            required("kit", STRING)
                    ),
                    route(
                            required("kit", STRING),
                            required("player", KNOWN_PLAYER)
                    )
            );

            // Messaging
            case "broadcast", "helpop", "me", "r" -> routes(route(
                    required("message", GREEDY_STRING)
            ));
            case "msg" -> routes(route(
                    required("player", PLAYER),
                    required("message", GREEDY_STRING)
            ));
            case "ignore" -> routes(route(
                    required("player", PLAYER)
            ));
            case "mail" -> routes(
                    route(),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "read", "unread", "clear"
                            )
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "read"
                            ),
                            required("page", INTEGER)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "delete"
                            ),
                            required("id", WORD)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "send"
                            ),
                            required("player", KNOWN_PLAYER),
                            required("message", GREEDY_STRING)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "sendtemp"
                            ),
                            required("player", KNOWN_PLAYER),
                            required("duration", WORD),
                            required("message", GREEDY_STRING)
                    ),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "sendall"
                            ),
                            required("message", GREEDY_STRING)
                    )
            );
            case "socialspy" -> routes(
                    route(),
                    route(
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    ),
                    route(
                            required("player", KNOWN_PLAYER)
                    ),
                    route(
                            required("player", KNOWN_PLAYER),
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    )
            );

            // Player state
            case "afk" -> routes(route());
            case "feed", "heal" -> routes(
                    route(),
                    route(
                            required("player", PLAYER)
                    )
            );
            case "fly", "god" -> routes(
                    route(),
                    route(
                            required("player", PLAYER)
                    ),
                    route(
                            required("player", PLAYER),
                            choice(
                                    "state",
                                    false,
                                    "on", "off"
                            )
                    )
            );
            case "vanish" -> routes(
                    route(),
                    route(
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    ),
                    route(
                            required("player", PLAYER)
                    ),
                    route(
                            required("player", PLAYER),
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    )
            );
            case "nick" -> routes(route(
                    required("nickname", STRING)
            ));
            case "seen", "whois" -> routes(route(
                    required("player", KNOWN_PLAYER)
            ));
            case "playtime" -> routes(
                    route(),
                    route(
                            required("player", KNOWN_PLAYER)
                    )
            );
            case "near" -> routes(
                    route(),
                    route(
                            required("radius", INTEGER)
                    )
            );
            case "gamemode" -> routes(
                    route(
                            choice(
                                    "mode",
                                    false,
                                    "survival", "creative", "adventure", "spectator"
                            )
                    ),
                    route(
                            choice(
                                    "mode",
                                    false,
                                    "survival", "creative", "adventure", "spectator"
                            ),
                            required("player", PLAYER)
                    )
            );
            case "speed" -> routes(
                    route(
                            required("speed", DOUBLE)
                    ),
                    route(
                            choice("type", false, "walk", "fly"),
                            required("speed", DOUBLE)
                    ),
                    route(
                            choice("type", false, "walk", "fly"),
                            required("speed", DOUBLE),
                            required("player", PLAYER)
                    )
            );
            case "ptime" -> routes(
                    route(
                            required("time", WORD)
                    ),
                    route(
                            required("time", WORD),
                            required("player", PLAYER)
                    )
            );
            case "pweather" -> routes(
                    route(
                            choice(
                                    "weather",
                                    false,
                                    "clear", "rain", "thunder", "reset"
                            )
                    ),
                    route(
                            choice(
                                    "weather",
                                    false,
                                    "clear",
                                    "rain",
                                    "thunder",
                                    "reset"
                            ),
                            required("player", PLAYER)
                    )
            );

            // Teleport and world
            case "tp" -> routes(
                    route(
                            required("target", PLAYER)
                    ),
                    route(
                            required("player", PLAYER),
                            required("target", PLAYER)
                    )
            );
            case "tpa", "tpahere" -> routes(route(
                    required("player", PLAYER)
            ));
            case "tpaccept", "tpdeny", "tpacancel" -> routes(
                    route(),
                    route(
                            required("request-or-player", STRING)
                    )
            );
            case "tpaall" -> routes(route());
            case "tpauto" -> routes(
                    route(),
                    route(
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    )
            );
            case "tpall" -> routes(
                    route(),
                    route(
                            required("player", PLAYER)
                    )
            );
            case "tphere", "tpohere" -> routes(route(
                    required("player", PLAYER)
            ));
            case "tpo" -> routes(
                    route(
                            required("target", PLAYER)
                    ),
                    route(
                            required("player", PLAYER),
                            required("target", PLAYER)
                    )
            );
            case "tpoffline" -> routes(route(
                    required("player", KNOWN_PLAYER)
            ));
            case "tptoggle" -> routes(
                    route(),
                    route(
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    ),
                    route(
                            required("player", KNOWN_PLAYER)
                    ),
                    route(
                            required("player", KNOWN_PLAYER),
                            choice(
                                    "state",
                                    false,
                                    "on", "off", "true", "false", "enable", "disable"
                            )
                    )
            );
            case "settpr" -> routes(
                    route(
                            required("world", WORLD),
                            choice(
                                    "action",
                                    false,
                                    "center"
                            )
                    ),
                    route(
                            required("world", WORLD),
                            choice(
                                    "action",
                                    false,
                                    "minrange", "maxrange"
                            )
                    ),
                    route(
                            required("world", WORLD),
                            choice(
                                    "action",
                                    false,
                                    "minrange",
                                    "maxrange"
                            ),
                            required("value", INTEGER)
                    )
            );
            case "tppos" -> routes(
                    route(
                            required("position", POSITION)
                    ),
                    route(
                            required("position", POSITION),
                            required("world", WORLD)
                    )
            );
            case "world" -> routes(
                    route(),
                    route(
                            required("world", WORLD)
                    )
            );
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
            case "info", "motd", "rules" -> routes(
                    route(),
                    route(
                            required("page", INTEGER)
                    )
            );
            case "customtext" -> routes(
                    route(
                            required("name", STRING)
                    ),
                    route(
                            required("name", STRING),
                            required("page", INTEGER)
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

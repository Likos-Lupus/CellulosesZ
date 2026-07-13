package top.likoslupus.cellulosesz.core.command.spec;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameter;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameterType;
import top.likoslupus.cellulosesz.api.command.spec.CommandRoute;
import top.likoslupus.cellulosesz.api.command.spec.CommandSpec;

import java.util.*;

import static top.likoslupus.cellulosesz.api.command.spec.CommandParameter.*;
import static top.likoslupus.cellulosesz.api.command.spec.CommandParameterType.*;

/**
 * Converts the platform-neutral command contract into a Brigadier-friendly route model. Explicit routes are used for
 * every command whose usage text is ambiguous; inference is deliberately conservative and never turns an arbitrary
 * item/name token into a greedy tail.
 */
public final class DefaultCommandSpecFactory {

    public CommandSpec spec(CellCommand command) {
        var declared = Objects.requireNonNull(
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
            case "balance" -> routes(
                    route(),
                    route(required("player", KNOWN_PLAYER))
            );
            case "balancetop" -> routes(
                    route(),
                    route(required("page", INTEGER))
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
                    required("player", KNOWN_PLAYER),
                    required("amount", DOUBLE),
                    optional("confirmation", WORD)
            ));
            case "setworth" -> routes(route(
                    required("item", WORD),
                    required("value", WORD)
            ));
            case "worth" -> routes(route(
                    required("item", WORD)
            ));

            // Homes and warps
            case "home" -> routes(
                    route(),
                    route(required("name", STRING))
            );
            case "sethome" -> routes(
                    route(),
                    route(required("name", STRING))
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
                    route(required("name", STRING))
            );
            case "setwarp", "delwarp", "warpinfo" -> routes(route(
                    required("name", STRING)
            ));

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
                    route(choice(
                            "scope",
                            false,
                            "hand", "all"
                    ))
            );
            case "invsee" -> routes(route(
                    required("player", PLAYER)
            ));
            case "enderchest" -> routes(
                    route(),
                    route(required("player", PLAYER))
            );
            case "powertool" -> routes(
                    route(),
                    route(required("command", GREEDY_STRING))
            );
            case "unlimited" -> routes(
                    route(),
                    route(choice(
                            "state",
                            false,
                            "on", "off", "enable", "disable", "true", "false", "list", "clear"
                    ))
            );
            case "createkit" -> routes(route(
                    required("name", STRING),
                    required("cooldown", WORD)
            ));
            case "delkit", "showkit" -> routes(route(
                    required("name", STRING)
            ));
            case "kit" -> routes(
                    route(),
                    route(required("name", STRING))
            );
            case "kitreset" -> routes(
                    route(required("kit", STRING)),
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
                    route(choice(
                            "action",
                            false,
                            "read"
                    )),
                    route(choice(
                            "action",
                            false,
                            "clear"
                    )),
                    route(
                            choice(
                                    "action",
                                    false,
                                    "send"
                            ),
                            required("player", KNOWN_PLAYER),
                            required("message", GREEDY_STRING)
                    )
            );

            // Player state
            case "feed", "heal" -> routes(
                    route(),
                    route(required("player", PLAYER))
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
                    route(choice(
                            "state",
                            false,
                            "on", "off", "true", "false", "enable", "disable"
                    )),
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
            case "tphere" -> routes(route(
                    required("player", PLAYER)
            ));
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
                    route(required("world", WORLD))
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

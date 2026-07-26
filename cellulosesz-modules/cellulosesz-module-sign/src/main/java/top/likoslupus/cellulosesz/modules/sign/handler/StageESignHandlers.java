package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;

import java.util.*;
import java.util.function.Function;

public final class StageESignHandlers {

    private static final Set<String> GAME_MODES = Set.of("survival", "creative", "adventure", "spectator");
    private static final Set<String> WEATHER = Set.of("clear", "rain", "thunder");

    private StageESignHandlers() {
    }

    public static List<CellSignHandler> create(
            PlatformService platform,
            EconomyService economy,
            ItemService items,
            PlayerStateService playerStates,
            WorldService worlds,
            TextService texts,
            MailService mail,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService randomSettings,
            TeleportService teleports
    ) {
        var handlers = new ArrayList<CellSignHandler>();
        handlers.add(simple("Balance", context -> SignUseResult.success(
                "service.sign.balance",
                Map.of("balance", economy.format(economy.balance(context.player().uuid())))
        )));
        handlers.add(new FunctionalSignHandler(
                "Free",
                context -> validateItem(items, freeItem(items, context), "service.sign.free-format"),
                context -> freeItem(items, context)
                        .map(item -> items.give(context.player(), item)
                                ? SignUseResult.success("service.sign.free-success", itemPlaceholders(item))
                                : SignUseResult.failure("service.sign.free-inventory-full"))
                        .orElseGet(() -> SignUseResult.failure("service.sign.free-format"))
        ));
        handlers.add(new FunctionalSignHandler(
                "Trade",
                context -> {
                    var cost = items.parse(context.line(1));
                    var reward = items.parse(context.line(2));
                    var first = validateItem(items, cost, "service.sign.trade-format");
                    if (!first.success()) return first;
                    return validateItem(items, reward, "service.sign.trade-format");
                },
                context -> trade(items, context)
        ));
        handlers.add(new FunctionalSignHandler(
                "Enchant",
                context -> enchantParameters(context).isPresent()
                        ? SignUseResult.success("service.sign.valid")
                        : SignUseResult.failure("service.sign.enchant-format"),
                context -> enchantParameters(context)
                        .map(parameters -> platform.enchantHeldItem(context.player(), parameters.name, parameters.level)
                                ? SignUseResult.success("service.sign.enchant-success", Map.of(
                                "enchantment", parameters.name, "level", parameters.level))
                                : SignUseResult.failure("service.sign.enchant-failed"))
                        .orElseGet(() -> SignUseResult.failure("service.sign.enchant-format"))
        ));
        handlers.add(new FunctionalSignHandler(
                "Repair",
                context -> context.line(1).isBlank() || Set.of("hand", "all")
                        .contains(context.line(1).toLowerCase(Locale.ROOT))
                        ? SignUseResult.success("service.sign.valid")
                        : SignUseResult.failure("service.sign.repair-format"),
                context -> {
                    var count = platform.repairItems(context.player(), context.line(1).equalsIgnoreCase("all"));
                    return count > 0
                            ? SignUseResult.success("service.sign.repair-success", Map.of("count", count))
                            : SignUseResult.failure("service.sign.repair-nothing");
                }
        ));
        handlers.add(new FunctionalSignHandler(
                "GameMode",
                context -> GAME_MODES.contains(context.line(1).toLowerCase(Locale.ROOT))
                        ? SignUseResult.success("service.sign.valid")
                        : SignUseResult.failure("service.sign.gamemode-format"),
                context -> platform.setGameMode(context.player(), context.line(1))
                        ? SignUseResult.success("service.sign.gamemode-success", Map.of("mode", context.line(1)))
                        : SignUseResult.failure("service.sign.gamemode-failed")
        ));
        handlers.add(simple("Heal", context -> admin(playerStates.heal(context.player()))));
        handlers.add(new FunctionalSignHandler(
                "Info",
                context -> textPage(texts, context).isPresent()
                        ? SignUseResult.success("service.sign.valid")
                        : SignUseResult.failure("service.sign.info-format"),
                context -> textPage(texts, context)
                        .map(value -> SignUseResult.success("service.sign.info", Map.of("text", value)))
                        .orElseGet(() -> SignUseResult.failure("service.sign.info-format"))
        ));
        handlers.add(simple("Mail", context -> SignUseResult.success(
                "service.sign.mail",
                Map.of("unread", mail.unreadCount(context.player().uuid()).join())
        )));
        handlers.add(new FunctionalSignHandler(
                "RandomTeleport",
                context -> context.line(1).isBlank() || platform.worlds().contains(context.line(1))
                        ? SignUseResult.success("service.sign.valid")
                        : SignUseResult.failure("service.sign.random-teleport-world"),
                context -> randomTeleport(platform, randomTeleports, randomSettings, teleports, context)
        ));
        workstation(handlers, platform, "Anvil", "anvil");
        workstation(handlers, platform, "Cartography", "cartography");
        workstation(handlers, platform, "Disposal", "disposal");
        workstation(handlers, platform, "Grindstone", "grindstone");
        workstation(handlers, platform, "Loom", "loom");
        workstation(handlers, platform, "Smithing", "smithing");
        workstation(handlers, platform, "Workbench", "workbench");
        handlers.add(new FunctionalSignHandler(
                "SpawnMob",
                context -> validateSpawnMob(platform, context),
                context -> {
                    var count = count(context.line(2), 1, 64).orElse(0);
                    if (count == 0) return SignUseResult.failure("service.sign.spawnmob-format");
                    var spawned = platform.spawnMob(context.player(), context.line(1), count);
                    return spawned == count
                            ? SignUseResult.success("service.sign.spawnmob-success", Map.of("count", count, "entity", context.line(1)))
                            : SignUseResult.failure("service.sign.spawnmob-failed", Map.of("spawned", spawned, "count", count));
                }
        ));
        handlers.add(new FunctionalSignHandler(
                "Time",
                context -> validateTime(platform, context),
                context -> {
                    var time = parseTime(context.line(1)).orElseThrow();
                    var world = context.line(2).isBlank() ? context.location().world : context.line(2);
                    return admin(worlds.setTime(world, time));
                }
        ));
        handlers.add(new FunctionalSignHandler(
                "Weather",
                context -> validateWeather(platform, context),
                context -> {
                    var type = WeatherType.valueOf(context.line(1).toUpperCase(Locale.ROOT));
                    var seconds = count(context.line(2), 1, 86400).orElse(300);
                    var world = context.line(3).isBlank() ? context.location().world : context.line(3);
                    return admin(worlds.setWeather(world, type, seconds));
                }
        ));
        return List.copyOf(handlers);
    }

    private static CellSignHandler simple(
            String id,
            Function<SignUseContext, SignUseResult> action
    ) {
        return new FunctionalSignHandler(id, _ -> SignUseResult.success("service.sign.valid"), action);
    }

    private static SignUseResult validateItem(
            ItemService items,
            Optional<ItemDescriptor> item,
            String formatKey
    ) {
        if (item.isEmpty() || !items.valid(item.orElseThrow())) return SignUseResult.failure(formatKey);
        if (items.blacklisted(item.orElseThrow())) {
            return SignUseResult.failure("service.sign.item-blacklisted", Map.of("item", item.orElseThrow()
                    .normalizedItem()));
        }
        return SignUseResult.success("service.sign.valid");
    }

    private static Optional<ItemDescriptor> freeItem(ItemService items, SignUseContext context) {
        var count = count(context.line(1), 1, 64);
        if (count.isEmpty() || context.line(2).isBlank()) return Optional.empty();
        return items.parse(context.line(2) + " " + count.orElseThrow());
    }

    private static Map<String, Object> itemPlaceholders(ItemDescriptor item) {
        return Map.of("count", item.count, "item", item.normalizedItem());
    }

    private static SignUseResult trade(ItemService items, SignUseContext context) {
        var cost = items.parse(context.line(1));
        var reward = items.parse(context.line(2));
        if (cost.isEmpty() || reward.isEmpty()) return SignUseResult.failure("service.sign.trade-format");
        if (items.count(context.player(), cost.orElseThrow()) < cost.orElseThrow().count) {
            return SignUseResult.failure("service.sign.trade-not-enough");
        }
        if (!items.take(context.player(), cost.orElseThrow()))
            return SignUseResult.failure("service.sign.trade-take-failed");
        if (items.give(context.player(), reward.orElseThrow())) {
            return SignUseResult.success("service.sign.trade-success", Map.of(
                    "cost", items.commandArgument(cost.orElseThrow()),
                    "reward", items.commandArgument(reward.orElseThrow())
            ));
        }
        if (!items.give(context.player(), cost.orElseThrow())) {
            return SignUseResult.failure("service.sign.trade-rollback-failed");
        }
        return SignUseResult.failure("service.sign.trade-inventory-full");
    }

    private static Optional<EnchantParameters> enchantParameters(SignUseContext context) {
        if (context.line(1).isBlank()) return Optional.empty();
        if (!context.line(2).isBlank() && count(context.line(2), 1, 255).isEmpty()) return Optional.empty();
        var level = count(context.line(2), 1, 255).orElse(1);
        return Optional.of(new EnchantParameters(context.line(1), level));
    }

    private static SignUseResult admin(top.likoslupus.cellulosesz.api.admin.AdminResult result) {
        return result.success() ? SignUseResult.success(result.message()) : SignUseResult.failure(result.message());
    }

    private static Optional<String> textPage(TextService texts, SignUseContext context) {
        var section = context.line(1).isBlank() ? "info" : context.line(1).toLowerCase(Locale.ROOT);
        var lines = switch (section) {
            case "info" -> texts.info();
            case "motd" -> texts.motd();
            case "rules" -> texts.rules();
            default -> texts.custom(section);
        };
        if (lines.isEmpty()) return Optional.empty();
        var page = count(context.line(2), 1, Integer.MAX_VALUE).orElse(1);
        var pageSize = Math.max(1, texts.pageSize());
        var from = (page - 1) * pageSize;
        if (from >= lines.size()) return Optional.empty();
        return Optional.of(String.join("\n", lines.subList(from, Math.min(lines.size(), from + pageSize))));
    }

    private static SignUseResult randomTeleport(
            PlatformService platform,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService settings,
            TeleportService teleports,
            SignUseContext context
    ) {
        var world = context.line(1).isBlank() ? context.location().world : context.line(1);
        if (!platform.worlds().contains(world)) return SignUseResult.failure("service.sign.random-teleport-world");
        var destination = randomTeleports.randomLocation(world, settings.settings(world));
        if (destination.isEmpty()) return SignUseResult.failure("service.sign.random-teleport-failed");
        var result = teleports.teleport(
                context.player(),
                destination.orElseThrow(),
                new TeleportOptions().safe(true).warmupSeconds(0)
        ).join();
        return result.success() ? SignUseResult.success(result.message()) : SignUseResult.failure(result.message());
    }

    private static void workstation(
            List<CellSignHandler> handlers,
            PlatformService platform,
            String id,
            String workstation
    ) {
        handlers.add(simple(id, context -> platform.openWorkstation(context.player(), workstation)
                ? SignUseResult.success("service.sign.workstation-opened", Map.of("workstation", id))
                : SignUseResult.failure("service.sign.workstation-failed", Map.of("workstation", id))));
    }

    private static SignUseResult validateSpawnMob(PlatformService platform, SignUseContext context) {
        if (!platform.validEntityType(context.line(1))) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }
        if (!context.line(2).isBlank() && count(context.line(2), 1, 64).isEmpty()) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }
        return SignUseResult.success("service.sign.valid");
    }

    private static Optional<Integer> count(String input, int minimum, int maximum) {
        if (input.isBlank()) return Optional.empty();
        try {
            var value = Integer.parseInt(input);
            return value >= minimum && value <= maximum ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static SignUseResult validateTime(PlatformService platform, SignUseContext context) {
        if (parseTime(context.line(1)).isEmpty()) {
            return SignUseResult.failure("service.sign.time-format");
        }
        if (!context.line(2).isBlank() && !platform.worlds().contains(context.line(2))) {
            return SignUseResult.failure("service.sign.time-world");
        }
        return SignUseResult.success("service.sign.valid");
    }

    private static Optional<Long> parseTime(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "day" -> Optional.of(1000L);
            case "noon" -> Optional.of(6000L);
            case "night" -> Optional.of(13000L);
            case "midnight" -> Optional.of(18000L);
            default -> {
                try {
                    var value = Long.parseLong(input);
                    yield value >= 0 ? Optional.of(value) : Optional.empty();
                } catch (NumberFormatException exception) {
                    yield Optional.empty();
                }
            }
        };
    }

    private static SignUseResult validateWeather(PlatformService platform, SignUseContext context) {
        if (!WEATHER.contains(context.line(1).toLowerCase(Locale.ROOT))) {
            return SignUseResult.failure("service.sign.weather-format");
        }
        if (!context.line(2).isBlank() && count(context.line(2), 1, 86400).isEmpty()) {
            return SignUseResult.failure("service.sign.weather-format");
        }
        if (!context.line(3).isBlank() && !platform.worlds().contains(context.line(3))) {
            return SignUseResult.failure("service.sign.weather-world");
        }
        return SignUseResult.success("service.sign.valid");
    }

    private record EnchantParameters(
            String name,
            int level
    ) {

    }

}

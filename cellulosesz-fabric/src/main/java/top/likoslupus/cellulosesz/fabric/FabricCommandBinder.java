package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionContext;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
import top.likoslupus.cellulosesz.api.command.service.CommandTreeService;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameter;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameterType;
import top.likoslupus.cellulosesz.api.command.spec.CommandRoute;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.command.spec.DefaultCommandSpecFactory;
import top.likoslupus.cellulosesz.fabric.mixin.CommandNodeAccessor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class FabricCommandBinder implements CommandTreeService {

    private final CellulosesZBootstrap bootstrap;
    private final FabricVanillaCommandBridge vanillaCommands;
    private final DefaultCommandSpecFactory specs = new DefaultCommandSpecFactory();
    private final CommandSuggestionRegistry suggestions;
    private final CommandAliasRegistry aliases;
    private final PlatformService platform;
    private final PlayerResolver players;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final Set<String> ownedLabels = new LinkedHashSet<>();
    private volatile @Nullable CommandDispatcher<CommandSourceStack> boundDispatcher;

    public FabricCommandBinder(
            CellulosesZBootstrap bootstrap,
            FabricVanillaCommandBridge vanillaCommands
    ) {
        this.bootstrap = bootstrap;
        this.vanillaCommands = vanillaCommands;
        this.suggestions = bootstrap.serviceRegistry().require(CommandSuggestionRegistry.class);
        this.aliases = bootstrap.serviceRegistry().require(CommandAliasRegistry.class);
        this.platform = bootstrap.serviceRegistry().require(PlatformService.class);
        this.players = bootstrap.serviceRegistry().require(PlayerResolver.class);
        this.renderer = bootstrap.serviceRegistry().require(MessageRenderer.class);
        this.locales = bootstrap.serviceRegistry().require(LocaleResolver.class);
    }

    public synchronized void bind(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        vanillaCommands.capture(dispatcher);
        boundDispatcher = dispatcher;
        ownedLabels.clear();
        registerAll(dispatcher);
    }

    @Override
    public synchronized void refresh() {
        var dispatcher = boundDispatcher;
        if (dispatcher == null) return;

        List.copyOf(ownedLabels).forEach(label -> {
            removeRootCommand(dispatcher, label);
            vanillaCommands.restore(dispatcher, label);
        });
        ownedLabels.clear();
        registerAll(dispatcher);
        platform.refreshCommandTree();
    }

    private void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        var claimed = new LinkedHashMap<String, String>();
        bootstrap.commandRegistry().commands().forEach(command -> {
            var labels = new LinkedHashSet<String>();
            labels.add(command.name());
            labels.addAll(command.aliases());
            labels.addAll(aliases.aliases(command.name()));
            labels.forEach(label -> {
                var normalized = label.toLowerCase(Locale.ROOT);
                var previous = claimed.putIfAbsent(normalized, command.name());
                if (previous != null && !previous.equals(command.name())) {
                    bootstrap.logger().warn(
                            "Skipping command label conflict: " + label + " is claimed by " + previous + " and " + command.name()
                    );
                    return;
                }
                register(dispatcher, command, normalized);
                ownedLabels.add(normalized);
            });
        });
    }

    private void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CellCommand command,
            String label
    ) {
        if (dispatcher.getRoot().getChild(label) != null) removeRootCommand(dispatcher, label);

        var root = Commands.literal(label)
                .requires(source -> permitted(source, command));
        var spec = specs.spec(command);
        spec.routes().forEach(route -> addRoute(root, route, command, label));
        dispatcher.register(root);
    }

    private void addRoute(
            LiteralArgumentBuilder<CommandSourceStack> root,
            CommandRoute route,
            CellCommand command,
            String label
    ) {
        addParameter(root, route.parameters(), 0, new ArrayList<>(), command, label);
    }

    private void addParameter(
            ArgumentBuilder<CommandSourceStack, ?> parent,
            List<CommandParameter> parameters,
            int index,
            List<ValueReader> readers,
            CellCommand command,
            String label
    ) {
        if (index >= parameters.size()) {
            parent.executes(context ->
                    execute(context.getSource(), command, label, values(context, readers))
            );
            return;
        }

        var parameter = parameters.get(index);
        if (parameter.optional()) {
            parent.executes(context ->
                    execute(context.getSource(), command, label, values(context, readers))
            );
        }

        if (!parameter.choices().isEmpty()) {
            parameter.choices().forEach(choice -> {
                var child = Commands.literal(choice);
                var childReaders = new ArrayList<>(readers);

                childReaders.add(_ -> List.of(choice));
                addParameter(child, parameters, index + 1, childReaders, command, label);
                parent.then(child);
            });
            return;
        }

        var child = argument(command, parameter, readers);
        var childReaders = new ArrayList<>(readers);
        childReaders.add(reader(parameter));
        addParameter(child, parameters, index + 1, childReaders, command, label);
        parent.then(child);
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> argument(
            CellCommand command,
            CommandParameter parameter,
            List<ValueReader> parsedReaders
    ) {
        RequiredArgumentBuilder<CommandSourceStack, ?> builder = switch (parameter.type()) {
            case INTEGER -> Commands.argument(parameter.name(), IntegerArgumentType.integer());
            case LONG -> Commands.argument(parameter.name(), LongArgumentType.longArg());
            case DOUBLE -> Commands.argument(parameter.name(), DoubleArgumentType.doubleArg());
            case BOOLEAN -> Commands.argument(parameter.name(), BoolArgumentType.bool());
            case PLAYER -> Commands.argument(parameter.name(), EntityArgument.player());
            case KNOWN_PLAYER -> Commands.argument(parameter.name(), StringArgumentType.word());
            case PLAYERS -> Commands.argument(parameter.name(), EntityArgument.players());
            case POSITION -> Commands.argument(parameter.name(), Vec3Argument.vec3());
            case STRING -> Commands.argument(parameter.name(), StringArgumentType.string());
            case GREEDY_STRING, ITEM -> Commands.argument(parameter.name(), StringArgumentType.greedyString());
            case WORD, WORLD -> Commands.argument(parameter.name(), StringArgumentType.word());
        };

        if (parameter.type() == CommandParameterType.WORLD
                || parameter.type() == CommandParameterType.PLAYER
                || parameter.type() == CommandParameterType.PLAYERS
                || parameter.type() == CommandParameterType.KNOWN_PLAYER
                || parameter.type() == CommandParameterType.ITEM
                || parameter.type() == CommandParameterType.WORD
                || parameter.type() == CommandParameterType.STRING
        ) {
            builder.suggests((context, suggestionsBuilder) ->
                    suggest(command, parameter, parsedReaders, context, suggestionsBuilder)
            );
        }
        return builder;
    }

    private CompletableFuture<Suggestions> suggest(
            CellCommand command,
            CommandParameter parameter,
            List<ValueReader> parsedReaders,
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        var values = new LinkedHashSet<String>();

        if (parameter.type() == CommandParameterType.WORLD) {
            values.addAll(platform.worlds());
        }

        if (parameter.type() == CommandParameterType.ITEM) {
            BuiltInRegistries.ITEM.keySet().stream()
                    .map(Object::toString)
                    .forEach(values::add);
        }

        var viewer = context.getSource().getEntity() instanceof ServerPlayer player
                ? platform.player(player).orElse(null)
                : null;
        if (parameter.type() == CommandParameterType.PLAYER
                || parameter.type() == CommandParameterType.PLAYERS
                || parameter.type() == CommandParameterType.KNOWN_PLAYER
                || Set.of("player", "players", "target", "user").contains(parameter.name().toLowerCase(Locale.ROOT))
        ) {
            platform.onlinePlayers().stream()
                    .map(player -> players.resolveKnown(player.uuid(), viewer))
                    .flatMap(resolved -> resolved.online().stream())
                    .map(CellPlayer::name)
                    .forEach(values::add);
            if (parameter.type() == CommandParameterType.PLAYER
                    || parameter.type() == CommandParameterType.PLAYERS
            ) {
                values.add("@s");
                values.add("@p");
                values.add("@a");
            }
        }

        var playerName = viewer == null
                ? Optional.<String>empty()
                : Optional.of(viewer.name());
        values.addAll(suggestions.suggest(new CommandSuggestionContext(
                command.name(),
                parameter.name(),
                builder.getRemaining(),
                parsedValues(context, parsedReaders),
                playerName
        )));

        var remaining = builder.getRemainingLowerCase();
        values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private ValueReader reader(CommandParameter parameter) {
        return switch (parameter.type()) {
            case INTEGER -> context ->
                    List.of(Integer.toString(IntegerArgumentType.getInteger(context, parameter.name())));
            case LONG -> context ->
                    List.of(Long.toString(LongArgumentType.getLong(context, parameter.name())));
            case DOUBLE -> context ->
                    List.of(Double.toString(DoubleArgumentType.getDouble(context, parameter.name())));
            case BOOLEAN -> context ->
                    List.of(Boolean.toString(BoolArgumentType.getBool(context, parameter.name())));
            case PLAYER -> context ->
                    List.of(EntityArgument.getPlayer(context, parameter.name()).getGameProfile().name());
            case KNOWN_PLAYER -> context ->
                    List.of(StringArgumentType.getString(context, parameter.name()));
            case PLAYERS -> context ->
                    EntityArgument.getPlayers(context, parameter.name()).stream()
                            .map(player -> player.getGameProfile().name())
                            .toList();
            case POSITION -> context -> {
                var value = Vec3Argument.getVec3(context, parameter.name());
                return List.of(Double.toString(value.x), Double.toString(value.y), Double.toString(value.z));
            };
            case GREEDY_STRING -> context ->
                    List.of(normalizeGreedy(StringArgumentType.getString(context, parameter.name())));
            case WORD, STRING, WORLD, ITEM -> context ->
                    List.of(StringArgumentType.getString(context, parameter.name()));
        };
    }

    private List<String> parsedValues(CommandContext<CommandSourceStack> context, List<ValueReader> readers) {
        var values = new ArrayList<String>();
        try {
            for (var reader : readers) values.addAll(reader.read(context));
        } catch (CommandSyntaxException ignored) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private String normalizeGreedy(String value) {
        if (value.length() < 2) return value;

        var first = value.charAt(0);
        var last = value.charAt(value.length() - 1);
        if ((first != '\'' && first != '"') || last != first) return value;

        var body = value.substring(1, value.length() - 1);
        return body
                .replace("\\" + first, Character.toString(first))
                .replace("\\\\", "\\");
    }

    private String[] values(
            CommandContext<CommandSourceStack> context,
            List<ValueReader> readers
    ) throws CommandSyntaxException {
        var values = new ArrayList<String>();
        for (var reader : readers) values.addAll(reader.read(context));
        return values.toArray(String[]::new);
    }

    private void removeRootCommand(CommandDispatcher<CommandSourceStack> dispatcher, String label) {
        @SuppressWarnings("unchecked")
        var root = (CommandNodeAccessor<CommandSourceStack>) dispatcher.getRoot();
        root.cellulosesz$children().remove(label);
        root.cellulosesz$literals().remove(label);
        root.cellulosesz$arguments().remove(label);
    }

    private boolean permitted(CommandSourceStack source, CellCommand command) {
        if (!bootstrap.permissionService().has(source, command.permission())) return false;
        if (command.sourceKind() == CommandSourceKind.PLAYER_ONLY && !(source.getEntity() instanceof ServerPlayer))
            return false;
        return command.sourceKind() != CommandSourceKind.CONSOLE_ONLY || !(source.getEntity() instanceof ServerPlayer);
    }

    private int execute(
            CommandSourceStack source,
            CellCommand command,
            String label,
            String[] args
    ) {
        var viewer = source.getEntity() instanceof ServerPlayer player
                ? platform.player(player).orElse(null)
                : null;
        return bootstrap.commandRegistry().execute(command, new FabricCommandInvocation(
                source,
                bootstrap.permissionService(),
                players,
                viewer,
                renderer,
                locales,
                label,
                args
        ));
    }

    @FunctionalInterface
    private interface ValueReader {

        List<String> read(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

    }

}

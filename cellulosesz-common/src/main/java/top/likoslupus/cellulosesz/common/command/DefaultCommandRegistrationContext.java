package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.command.catalog.CommandCatalogEntry;
import top.likoslupus.cellulosesz.core.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.core.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.core.command.service.CommandAvailabilityService;
import top.likoslupus.cellulosesz.core.command.service.PermissionCatalog;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandRegistrationContext implements CommandRegistrationContext {

    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext buildContext;
    private final Commands.CommandSelection environment;
    private final CellulosesZBootstrap bootstrap;
    private final CommandRootLeaseManager leases;
    private final MinecraftCommandResponder responder;
    private final CommandExecutionPipeline pipeline;
    private final PlayerDirectory players;
    private final CommandAvailabilityService availability;
    private final Map<String, CommandCatalogEntry> entries = new LinkedHashMap<>();

    DefaultCommandRegistrationContext(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection environment,
            CellulosesZBootstrap bootstrap,
            CommandRootLeaseManager leases,
            MinecraftCommandResponder responder,
            CommandExecutionPipeline pipeline,
            PlayerDirectory players,
            CommandAvailabilityService availability
    ) {
        this.dispatcher = requireNonNull(dispatcher, "dispatcher");
        this.buildContext = requireNonNull(buildContext, "buildContext");
        this.environment = requireNonNull(environment, "environment");
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
        this.leases = requireNonNull(leases, "leases");
        this.responder = requireNonNull(responder, "responder");
        this.pipeline = requireNonNull(pipeline, "pipeline");
        this.players = requireNonNull(players, "players");
        this.availability = requireNonNull(availability, "availability");
    }

    public CommandDispatcher<CommandSourceStack> dispatcher() {
        return dispatcher;
    }

    public CommandBuildContext buildContext() {
        return buildContext;
    }

    @Override
    public ServiceRegistry services() {
        return bootstrap.serviceRegistry();
    }

    @Override
    public boolean hasPermission(
            CommandSourceStack source,
            String permission
    ) {
        requireNonNull(source, "source");
        requireNonNull(permission, "permission");
        if (permission.isBlank()) {
            return true;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            return !player.hasDisconnected()
                    && permissions().has(MinecraftPlayers.wrap(player), permission);
        }

        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(4))
        );
    }

    @Override
    public boolean moduleEnabled(String moduleId) {
        return bootstrap.moduleEnabled(moduleId);
    }

    @Override
    public Optional<CellPlayer> player(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)
                || player.hasDisconnected()
        ) {
            return Optional.empty();
        }
        return Optional.ofNullable(players.onlinePlayer(player.getUUID()));
    }

    @Override
    public List<String> onlinePlayerNames() {
        return players.onlinePlayerNames();
    }

    @Override
    public CommandNode<CommandSourceStack> registerDirect(
            String owner,
            CommandDescriptor descriptor,
            List<String> semanticRoots,
            String description,
            String usage,
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        if (availability.disabled(descriptor.canonicalName())
                || !moduleEnabled(descriptor.moduleId())
        ) {
            return root.build();
        }

        var canonical = descriptor.canonicalName().toLowerCase(Locale.ROOT);
        root.requires(requirement(descriptor));

        var node = leases.claimCanonical(
                canonical,
                canonical,
                owner,
                CommandRootLeaseManager.LabelKind.CANONICAL,
                root
        );

        if (!descriptor.permission().isBlank()) {
            services().require(PermissionCatalog.class)
                    .register(
                            descriptor.permission(),
                            description.isBlank()
                                    ? usage
                                    : description
                    );
        }
        entries.put(
                canonical,
                new CommandCatalogEntry(descriptor, semanticRoots, description, usage)
        );

        return node;
    }

    public Predicate<CommandSourceStack> requirement(CommandDescriptor descriptor) {
        return source -> canUse(source, descriptor);
    }

    public boolean canUse(
            CommandSourceStack source,
            CommandDescriptor descriptor
    ) {
        if (availability.disabled(descriptor.canonicalName())
                || !moduleEnabled(descriptor.moduleId())
                || !descriptor.permission().isBlank()
                && !hasPermission(source, descriptor.permission())
                || descriptor.requiredSourceKind() == CommandSourceKind.PLAYER_ONLY
                && !(source.getEntity() instanceof ServerPlayer)
        ) {
            return false;
        }

        return descriptor.requiredSourceKind() != CommandSourceKind.CONSOLE_ONLY
                || !(source.getEntity() instanceof ServerPlayer);
    }

    private PermissionService permissions() {
        return bootstrap.permissionService();
    }

    @Override
    public CommandNode<CommandSourceStack> registerSemantic(
            String owner,
            CommandDescriptor descriptor,
            String label,
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        if (availability.disabled(descriptor.canonicalName())
                || !moduleEnabled(descriptor.moduleId())
        ) {
            return root.build();
        }

        root.requires(requirement(descriptor));
        return leases.claimCanonical(
                label,
                descriptor.canonicalName(),
                owner,
                CommandRootLeaseManager.LabelKind.SEMANTIC_ROOT,
                root
        );
    }

    @Override
    public void registerAlias(
            String owner,
            CommandDescriptor descriptor,
            String label,
            CommandNode<CommandSourceStack> target
    ) {
        if (availability.disabled(descriptor.canonicalName())
                || !moduleEnabled(descriptor.moduleId())
        ) {
            return;
        }

        leases.claimAlias(
                label.toLowerCase(Locale.ROOT),
                descriptor.canonicalName().toLowerCase(Locale.ROOT),
                owner,
                Commands.literal(label.toLowerCase(Locale.ROOT))
                        .requires(target::canUse)
                        .redirect(target)
        );
    }

    @Override
    public int execute(
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<
                    MinecraftCommandPolicyContext,
                    ? extends CompletionStage<CommandOutcome>
                    > terminal
    ) {
        final MinecraftCommandPolicyContext policy;
        try {
            policy = new MinecraftCommandPolicyContext(
                    command.getSource(),
                    descriptor,
                    permissions(),
                    players,
                    responder,
                    invokedLabel(command.getInput()),
                    auditSummary
            );

            pipeline
                    .execute(
                            descriptor,
                            policy,
                            () -> terminal.apply(policy)
                    )
                    .whenComplete((_, failure) -> {
                        if (failure != null) {
                            bootstrap.logger().error(
                                    "Command /%s pipeline completion callback failed".formatted(
                                            policy.canonicalRoot()
                                    ),
                                    failure
                            );
                        }
                    });

            return 1;
        } catch (RuntimeException failure) {
            bootstrap.logger().error(
                    "Failed to start command /" + descriptor.canonicalName(),
                    failure
            );
            throw failure;
        }
    }

    private String invokedLabel(String input) {
        var trimmed = requireNonNull(input, "input").trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        var split = trimmed.indexOf(' ');
        return (
                split < 0
                        ? trimmed
                        : trimmed.substring(0, split)
        ).toLowerCase(Locale.ROOT);
    }

    public Commands.CommandSelection environment() {
        return environment;
    }

    public CommandAliasRegistry aliases() {
        return services().require(CommandAliasRegistry.class);
    }

    public MinecraftCommandResponder responder() {
        return responder;
    }

    public CommandExecutionPipeline pipeline() {
        return pipeline;
    }

    public CommandRootLeaseManager leases() {
        return leases;
    }

    List<CommandCatalogEntry> entries() {
        return List.copyOf(entries.values());
    }

    public Map<String, List<String>> configuredAliases() {
        var result = new LinkedHashMap<String, List<String>>();
        bootstrap.coreConfig().commands.aliases.forEach((command, aliases) ->
                result.put(
                        command.toLowerCase(Locale.ROOT),
                        List.copyOf(aliases)
                )
        );
        return Map.copyOf(result);
    }

}

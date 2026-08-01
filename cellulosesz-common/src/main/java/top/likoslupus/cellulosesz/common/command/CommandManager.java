package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.api.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandAvailabilityService;
import top.likoslupus.cellulosesz.api.command.service.CommandTreeService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Loader-neutral coordinator for the single module-owned Brigadier command catalog.
 */
public final class CommandManager implements CommandTreeService {

    private final CellulosesZBootstrap bootstrap;
    private final CommandRegistry registry;
    private final CommandRootLeaseManager leases;
    private final MinecraftCommandResponder responder;
    private final CommandExecutionPipeline pipeline;
    private final PlayerDirectory players;
    private final CommandAvailabilityService availability;
    private final CommandTreeRefreshService refreshService;
    private volatile @Nullable CommandDispatcher<CommandSourceStack> dispatcher;
    private volatile @Nullable CommandBuildContext buildContext;
    private volatile Commands.@Nullable CommandSelection environment;

    public CommandManager(
            CellulosesZBootstrap bootstrap,
            CommandRegistry registry,
            CommandRootMutator mutator,
            CommandTreeRefreshService refreshService
    ) {
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
        this.registry = requireNonNull(registry, "registry");
        this.leases = new CommandRootLeaseManager(bootstrap.logger(), mutator);
        this.pipeline = bootstrap.serviceRegistry().require(CommandExecutionPipeline.class);
        this.players = bootstrap.serviceRegistry().require(PlayerDirectory.class);
        this.availability = bootstrap.serviceRegistry().require(CommandAvailabilityService.class);
        this.responder = new MinecraftCommandResponder(
                players,
                bootstrap.serviceRegistry().require(PlayerAudienceService.class),
                bootstrap.serviceRegistry().require(MessageRenderer.class),
                bootstrap.serviceRegistry().require(LocaleResolver.class),
                bootstrap.serviceRegistry().require(ServerThreadExecutor.class),
                bootstrap.logger()
        );
        this.refreshService = requireNonNull(refreshService, "refreshService");
    }

    public synchronized void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection environment
    ) {
        var changedDispatcher = this.dispatcher != dispatcher;
        this.dispatcher = requireNonNull(dispatcher, "dispatcher");
        this.buildContext = requireNonNull(buildContext, "buildContext");
        this.environment = requireNonNull(environment, "environment");

        if (changedDispatcher) {
            leases.capture(dispatcher);
        }
        rebuild();
    }

    private void rebuild() {
        try (var transaction = leases.beginBuild()) {
            var context = new DefaultCommandRegistrationContext(
                    requireNonNull(dispatcher, "dispatcher"),
                    requireNonNull(buildContext, "buildContext"),
                    requireNonNull(environment, "environment"),
                    bootstrap,
                    leases,
                    responder,
                    pipeline,
                    players,
                    availability
            );
            registry.freezeAndSnapshot().forEach(contributor -> contributor.register(context));
            registerConfiguredAliases(context);
            bootstrap.serviceRegistry().require(CommandCatalog.class).replace(context.entries());
            transaction.commit();
        }
    }

    private void registerConfiguredAliases(DefaultCommandRegistrationContext context) {
        context.configuredAliases().forEach((canonical, aliases) -> {
            var normalizedCanonical = canonical.toLowerCase(Locale.ROOT);
            var target = leases.ownedNode(normalizedCanonical);
            if (target.isEmpty()) {
                bootstrap
                        .logger()
                        .warn("Skipping configured aliases for unavailable command /" + canonical);
                return;
            }

            aliases.forEach(alias -> {
                var normalized = alias.trim().toLowerCase(Locale.ROOT);
                if (normalized.isEmpty() || normalized.equals(normalizedCanonical)) {
                    return;
                }

                var node = target.orElseThrow();
                leases.claimAlias(
                        normalized,
                        normalizedCanonical,
                        "config",
                        Commands.literal(normalized)
                                .requires(node::canUse)
                                .redirect(node)
                );
            });
        });
    }

    @Override
    public synchronized void refresh() {
        if (dispatcher == null || buildContext == null || environment == null) {
            return;
        }
        rebuild();
        refreshService.refreshOnlinePlayers();
    }

    public CommandRootLeaseManager leases() {
        return leases;
    }

}

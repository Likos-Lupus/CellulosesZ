package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalogEntry;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.api.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.*;
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
    private final PlatformService platform;

    private final Map<String, CommandCatalogEntry> directEntries = new LinkedHashMap<>();
    private final Set<String> directCanonical = new LinkedHashSet<>();

    DefaultCommandRegistrationContext(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection environment,
            CellulosesZBootstrap bootstrap,
            CommandRootLeaseManager leases,
            MinecraftCommandResponder responder,
            CommandExecutionPipeline pipeline,
            PlatformService platform
    ) {
        this.dispatcher = requireNonNull(dispatcher, "dispatcher");
        this.buildContext = requireNonNull(buildContext, "buildContext");
        this.environment = requireNonNull(environment, "environment");
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
        this.leases = requireNonNull(leases, "leases");
        this.responder = requireNonNull(responder, "responder");
        this.pipeline = requireNonNull(pipeline, "pipeline");
        this.platform = requireNonNull(platform, "platform");
    }

    public CommandDispatcher<CommandSourceStack> dispatcher() {
        return dispatcher;
    }

    public CommandBuildContext buildContext() {
        return buildContext;
    }

    public Commands.CommandSelection environment() {
        return environment;
    }

    public CommandAliasRegistry aliases() {
        return bootstrap.serviceRegistry().require(CommandAliasRegistry.class);
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

    public CommandNode<CommandSourceStack> registerLegacy(
            CellCommand command,
            String label,
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        var descriptor = descriptor(command);
        root.requires(requirement(descriptor));

        return leases.claimCanonical(
                label,
                command.name(),
                "legacy",
                CommandRootLeaseManager.RegistrationMode.LEGACY,
                label.equalsIgnoreCase(command.name())
                        ? CommandRootLeaseManager.LabelKind.CANONICAL
                        : CommandRootLeaseManager.LabelKind.DECLARED_ALIAS,
                root
        );
    }

    public CommandDescriptor descriptor(CellCommand command) {
        return new CommandDescriptor(
                bootstrap.commandRegistry()
                        .moduleId(command)
                        .orElse("unknown"),
                command.name(),
                command.permission(),
                command.sourceKind()
        );
    }

    public Predicate<CommandSourceStack> requirement(CommandDescriptor descriptor) {
        return source -> canUse(source, descriptor);
    }

    public boolean canUse(
            CommandSourceStack source,
            CommandDescriptor descriptor
    ) {
        if (disabled(descriptor.canonicalName())
                || !moduleEnabled(descriptor.moduleId())
                || !descriptor.permission().isBlank()
                && !permissions().has(source, descriptor.permission())
                || descriptor.requiredSourceKind() == CommandSourceKind.PLAYER_ONLY
                && !(source.getEntity() instanceof ServerPlayer)
        ) {
            return false;
        }

        return descriptor.requiredSourceKind() != CommandSourceKind.CONSOLE_ONLY
                || !(source.getEntity() instanceof ServerPlayer);
    }

    public List<String> onlinePlayerNames() {
        return platform.onlinePlayers()
                .stream()
                .map(CellPlayer::name)
                .sorted()
                .toList();
    }

    public boolean disabled(String canonical) {
        return bootstrap.serviceRegistry()
                .require(DefaultCommandRegistry.class)
                .disabled(canonical);
    }

    public Set<String> directCanonical() {
        return Set.copyOf(directCanonical);
    }

    public List<CommandCatalogEntry> directEntries() {
        return List.copyOf(directEntries.values());
    }

    public Map<String, List<String>> configuredAliases() {
        var result = new LinkedHashMap<String, List<String>>();

        bootstrap.coreConfig().commands.aliases.forEach(
                (command, aliases) -> result.put(
                        command.toLowerCase(Locale.ROOT),
                        List.copyOf(aliases)
                )
        );

        return Map.copyOf(result);
    }

    public Optional<CellPlayer> player(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || player.hasDisconnected()) {
            return Optional.empty();
        }
        return platform.player(player);
    }


    public CommandNode<CommandSourceStack> registerDirect(
            String owner,
            CommandDescriptor descriptor,
            List<String> semanticRoots,
            String description,
            String usage,
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        if (disabled(descriptor.canonicalName()) || !moduleEnabled(descriptor.moduleId())) {
            return root.build();
        }

        var canonical = descriptor.canonicalName().toLowerCase(Locale.ROOT);
        root.requires(requirement(descriptor));

        var node = leases.claimCanonical(
                canonical,
                canonical,
                owner,
                CommandRootLeaseManager.RegistrationMode.DIRECT,
                CommandRootLeaseManager.LabelKind.CANONICAL,
                root
        );
        directCanonical.add(canonical);
        if (!descriptor.permission().isBlank()) {
            services().require(PermissionCatalog.class)
                    .register(
                            descriptor.permission(),
                            description.isBlank()
                                    ? usage
                                    : description
                    );
        }

        directEntries.put(
                canonical,
                new CommandCatalogEntry(
                        descriptor,
                        semanticRoots,
                        description,
                        usage
                )
        );

        return node;
    }

    public boolean moduleEnabled(String moduleId) {
        return bootstrap.moduleEnabled(moduleId);
    }

    public ServiceRegistry services() {
        return bootstrap.serviceRegistry();
    }

    public PermissionService permissions() {
        return bootstrap.permissionService();
    }

    public CommandNode<CommandSourceStack> registerSemantic(
            String owner,
            CommandDescriptor descriptor,
            String label,
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        if (disabled(descriptor.canonicalName()) || !moduleEnabled(descriptor.moduleId())) {
            return root.build();
        }

        root.requires(requirement(descriptor));
        return leases.claimCanonical(
                label,
                descriptor.canonicalName(),
                owner,
                CommandRootLeaseManager.RegistrationMode.DIRECT,
                CommandRootLeaseManager.LabelKind.SEMANTIC_ROOT,
                root
        );
    }

    public void internalFailure(
            MinecraftCommandPolicyContext policy,
            Throwable failure
    ) {
        bootstrap.logger().error(
                "Command /"
                        + policy.canonicalRoot()
                        + " failed after asynchronous acceptance",
                failure
        );

        policy.error(
                LocalizedMessage.of(
                        GeneratedMessageKeys
                                .COMMANDS_COMMON_PLATFORM_INTERNAL_ERROR
                )
        );
    }

    public int execute(
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String auditSummary,
            Function<MinecraftCommandPolicyContext, Integer> terminal
    ) {
        var policy = new MinecraftCommandPolicyContext(
                command.getSource(),
                descriptor,
                permissions(),
                platform,
                responder,
                invokedLabel(command.getInput()),
                auditSummary
        );
        return pipeline.execute(
                descriptor,
                policy,
                () -> terminal.apply(policy)
        );
    }

    private String invokedLabel(String input) {
        var trimmed = requireNonNull(input, "input").trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        var split = trimmed.indexOf(' ');
        return (split < 0 ? trimmed : trimmed.substring(0, split))
                .toLowerCase(Locale.ROOT);
    }

}

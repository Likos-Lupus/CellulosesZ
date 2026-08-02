package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandAvailabilityService;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.command.middleware.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "command",
        name = "Command",
        description = "Registers the CellulosesZ root command and command infrastructure.",
        phase = ModulePhase.CORE,
        priority = 0
)
public final class CommandModule implements CellulosesZModule {

    private @Nullable CommandConfig config;
    private @Nullable Registration auditRegistration;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.command",
                CommandConfig.class,
                "modules/command.yml",
                CommandConfig::new
        );
        config = validate(context.configs().require("module.command", CommandConfig.class));
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "CommandConfig has not been initialized");
        context.services()
                .require(CommandAvailabilityService.class)
                .replaceDisabledCommands(current.disabledCommands);
        var middlewares = context.middlewares();

        context.scope().own(middlewares.addMiddleware(
                new SourceKindCommandMiddleware(),
                context.moduleId()
        ));
        context.scope().own(middlewares.addMiddleware(
                new ModuleEnabledCommandMiddleware(context),
                context.moduleId()
        ));
        context.scope().own(middlewares.addMiddleware(
                new PermissionCommandMiddleware(),
                context.moduleId()
        ));
        context.scope().own(middlewares.addMiddleware(
                new CommandCostMiddleware(context.services().require(CommandCostService.class)),
                context.moduleId()
        ));
        setAuditEnabled(context, current.auditCommands);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        context.scope().own(registry.register(
                "cellulosesz-command",
                new CellulosesZCommand(
                        context.services().require(RuntimeService.class),
                        context.services().require(ServerThreadExecutor.class)
                )
        ));
        context.scope().own(registry.register("help-command", new HelpCommand()));
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(config, "CommandConfig has not been initialized");
        var candidate = validate(reload.configs().require(
                "module.command",
                CommandConfig.class
        ));
        var availability = context.services().require(CommandAvailabilityService.class);

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    availability.replaceDisabledCommands(candidate.disabledCommands);
                    setAuditEnabled(context, candidate.auditCommands);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previous;
                    availability.replaceDisabledCommands(previous.disabledCommands);
                    setAuditEnabled(context, previous.auditCommands);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private void setAuditEnabled(ModuleContext context, boolean enabled) {
        var current = auditRegistration;
        if (!enabled) {
            if (current != null) {
                current.close();
                auditRegistration = null;
            }
            return;
        }

        if (current != null && !current.closed()) {
            return;
        }

        auditRegistration = context.scope().own(context.middlewares().addMiddleware(
                new AuditCommandMiddleware(context.logger()),
                context.moduleId()
        ));
    }

    private static CommandConfig validate(CommandConfig config) {
        requirePositive(
                config.helpPageSize,
                "module.command.helpPageSize"
        );
        return config;
    }

}

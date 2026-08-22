package top.likoslupus.cellulosesz.modules.text;

import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.text.*;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.text.application.DefaultTextCommandService;
import top.likoslupus.cellulosesz.modules.text.application.TextCommandService;
import top.likoslupus.cellulosesz.modules.text.command.TextCommand;
import top.likoslupus.cellulosesz.modules.text.config.TextConfig;
import top.likoslupus.cellulosesz.modules.text.service.ConfigTextService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class TextModule implements CellulosesZModule {

    private @Nullable TextConfig config;
    private @Nullable ConfigTextService texts;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.text",
                TextConfig.class,
                "modules/text.yml",
                TextConfig::new
        );
        config = context.configs().require("module.text", TextConfig.class);
    }

    @Override
    public void registerServices(ModuleContext context) {
        texts = new ConfigTextService(requireNonNull(
                config,
                "TextConfig has not been initialized"
        ));
        context.services().register(TextService.class, texts);
        context.services().register(ConfigTextService.class, texts);
        context.services().register(TextCommandService.class, new DefaultTextCommandService(texts));
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var audience = context.services().require(PlayerAudienceService.class);
        var locales = context.services().require(LocaleResolver.class);
        var renderer = context.services().require(MessageRenderer.class);

        context.events().listen(
                PlayerJoinEvent.class,
                event -> {
                    var current = requireNonNull(
                            config,
                            "TextConfig has not been initialized"
                    );
                    if (!current.showMotdOnJoin) {
                        return;
                    }

                    var service = requireNonNull(
                            texts,
                            "TextService has not been initialized"
                    );
                    service.motd().forEach(line -> audience.send(
                            event.player(),
                            renderer.render(
                                    locales.locale(event.player()),
                                    "commands.text.line",
                                    MessageArguments.builder().add(line).build()
                            )
                    ));
                }
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var service = context.services().require(TextCommandService.class);
        context.scope().own(registry.register("text-commands", new TextCommand(service)));
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var previous = requireNonNull(config, "TextConfig has not been initialized");
        var candidate = reload.configs().require("module.text", TextConfig.class);
        var service = requireNonNull(texts, "TextService has not been initialized");

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    service.configure(candidate);
                    config = candidate;
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    service.configure(previous);
                    config = previous;
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

}

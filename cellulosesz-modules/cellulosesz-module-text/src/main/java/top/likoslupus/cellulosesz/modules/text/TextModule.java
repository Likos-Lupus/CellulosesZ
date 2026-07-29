package top.likoslupus.cellulosesz.modules.text;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;
import top.likoslupus.cellulosesz.modules.text.application.DefaultTextCommandService;
import top.likoslupus.cellulosesz.modules.text.application.TextCommandService;
import top.likoslupus.cellulosesz.modules.text.command.TextCommand;
import top.likoslupus.cellulosesz.modules.text.config.TextConfig;
import top.likoslupus.cellulosesz.modules.text.service.ConfigTextService;

import java.util.Map;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "text",
        name = "Text",
        description = "Info, MOTD, rules, and custom paged text commands.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class TextModule implements CellulosesZModule {

    private @Nullable TextConfig config;
    private @Nullable ConfigTextService texts;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.text", TextConfig.class, "modules/text.yml", TextConfig::new
        );
    }

    @Override
    @SuppressWarnings("resource")
    public void registerServices(ModuleContext context) {
        texts = new ConfigTextService(requireNonNull(config, "TextConfig has not been initialized"));
        context.services().register(TextService.class, texts);
        context.services().register(ConfigTextService.class, texts);
        context.services().register(TextCommandService.class, new DefaultTextCommandService(texts));
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);

        context.events().listen(PlayerJoinEvent.class, event -> {
            var current = requireNonNull(config, "TextConfig has not been initialized");
            if (!current.showMotdOnJoin) return;

            var service = requireNonNull(texts, "TextService has not been initialized");
            service.motd().forEach(line -> platform.sendMessage(
                    event.player(),
                    renderer.render(
                            platform.locale(event.player()),
                            GeneratedMessageKeys.COMMANDS_TEXT_LINE,
                            Map.of("line", line)
                    )
            ));
        });
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var service = context.services().require(TextCommandService.class);
        context.track(registry.register("text-commands", new TextCommand(service)));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.text", TextConfig.class);
        requireNonNull(texts, "TextService has not been initialized")
                .configure(requireNonNull(config, "TextConfig has not been initialized"));
    }

}

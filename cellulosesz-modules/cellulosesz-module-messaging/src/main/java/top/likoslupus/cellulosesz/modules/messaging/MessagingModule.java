package top.likoslupus.cellulosesz.modules.messaging;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.command.*;
import top.likoslupus.cellulosesz.modules.messaging.service.DefaultPrivateMessageService;
import top.likoslupus.cellulosesz.modules.messaging.service.JsonMailService;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "messaging",
        name = "Messaging",
        description = "Private messages, replies, ignore, mail, social spy, helpop, broadcast, and list commands.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class MessagingModule implements CellulosesZModule {

    private @Nullable MessagingConfig config;
    private @Nullable PrivateMessageService privateMessages;
    private @Nullable MailService mail;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.messaging",
                MessagingConfig.class,
                "modules/messaging.yml",
                MessagingConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent().resolve("mails");
        var displayNames = context.services().require(DisplayNameService.class);
        var renderer = context.services().require(MessageRenderer.class);

        requireNonNull(config, "MessagingConfig has not been initialized");

        privateMessages = new DefaultPrivateMessageService(platform, users, displayNames, renderer);
        mail = new JsonMailService(storage, config, root);

        context.services().register(PrivateMessageService.class, privateMessages);
        context.services().register(DefaultPrivateMessageService.class, (DefaultPrivateMessageService) privateMessages);
        context.services().register(MailService.class, mail);
        context.services().register(JsonMailService.class, (JsonMailService) mail);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var permissions = context.services().require(PermissionService.class);
        var displayNames = context.services().require(DisplayNameService.class);
        var renderer = context.services().require(MessageRenderer.class);

        requireNonNull(config, "MessagingConfig has not been initialized");
        requireNonNull(privateMessages, "PrivateMessageService has not been initialized");
        requireNonNull(mail, "MailService has not been initialized");

        context.commands().register(new MsgCommand(platform, users, config, privateMessages));
        context.commands().register(new ReplyCommand(platform, users, config, privateMessages));
        context.commands().register(new MsgToggleCommand(platform, users, config));
        context.commands().register(new ReplyToggleCommand(platform, users, config));
        context.commands().register(new IgnoreCommand(platform, users, config, privateMessages));
        context.commands().register(new MailCommand(platform, users, config, mail, displayNames, renderer));
        context.commands().register(new SocialSpyCommand(platform, users, config, privateMessages));
        context.commands().register(new HelpOpCommand(platform, users, config, permissions, displayNames, renderer));
        context.commands().register(new BroadcastCommand(platform, users, config, renderer));
        context.commands().register(new BroadcastWorldCommand(platform, users, config, renderer));
        context.commands().register(new MeCommand(platform, users, config, displayNames, renderer));
        context.commands().register(new ListCommand(platform, users, config, displayNames, renderer));
    }

}

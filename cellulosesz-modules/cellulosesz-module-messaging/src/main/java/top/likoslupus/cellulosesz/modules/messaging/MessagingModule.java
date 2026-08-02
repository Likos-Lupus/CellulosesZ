package top.likoslupus.cellulosesz.modules.messaging;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.messaging.application.ChatCommandService;
import top.likoslupus.cellulosesz.modules.messaging.application.MailCommandService;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;
import top.likoslupus.cellulosesz.modules.messaging.command.*;
import top.likoslupus.cellulosesz.modules.messaging.service.DefaultPrivateMessageService;
import top.likoslupus.cellulosesz.modules.messaging.service.JsonMailService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "messaging",
        name = "Messaging",
        description = "Private messages, replies, ignore, mail, social spy, helpop, broadcast, and list commands.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
@SuppressWarnings("resource")
public final class MessagingModule implements CellulosesZModule {

    private @Nullable MessagingConfig config;
    private @Nullable PrivateMessageService privateMessages;
    private @Nullable MailService mail;
    private @Nullable ChatCommandService chatCommands;
    private @Nullable PrivateMessageCommandService privateMessageCommands;
    private @Nullable MailCommandService mailCommands;
    private @Nullable TaskHandle mailSweep;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.messaging",
                MessagingConfig.class,
                "modules/messaging.yml",
                MessagingConfig::new
        );
        config = context
                .configs()
                .require("module.messaging", MessagingConfig.class)
                .validatedCopy();
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "MessagingConfig has not been initialized");
        var users = context.services().require(UserService.class);
        var permissions = context.services().require(PermissionService.class);
        var storage = context.services().require(StorageService.class);
        var players = context.services().require(PlayerDirectory.class);
        var audiences = context.services().require(PlayerAudienceService.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var displayNames = context.services().require(DisplayNameService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var resolver = context.services().require(PlayerResolver.class);
        var names = context.services().require(NameCacheService.class);

        privateMessages = new DefaultPrivateMessageService(
                players,
                audiences,
                serverThread,
                users,
                permissions,
                displayNames,
                renderer
        );
        mail = new JsonMailService(
                storage,
                current,
                context.dataDirectory().resolve("mail.json")
        );
        chatCommands = new ChatCommandService(
                context.services(),
                players,
                audiences,
                context.services().require(PlayerLocationPlatformService.class),
                context.services().require(WorldDirectory.class),
                serverThread,
                permissions,
                displayNames,
                renderer,
                current
        );
        privateMessageCommands = new PrivateMessageCommandService(
                resolver,
                players,
                names,
                privateMessages,
                users,
                serverThread,
                current
        );
        mailCommands = new MailCommandService(
                mail,
                users,
                resolver,
                players,
                audiences,
                serverThread,
                displayNames,
                renderer,
                privateMessages,
                current
        );

        context.services().register(PrivateMessageService.class, privateMessages);
        context.services().register(
                DefaultPrivateMessageService.class,
                (DefaultPrivateMessageService) privateMessages
        );
        context.services().register(MailService.class, mail);
        context.services().register(JsonMailService.class, (JsonMailService) mail);
        context.services().register(ChatCommandService.class, chatCommands);
        context.services().register(PrivateMessageCommandService.class, privateMessageCommands);
        context.services().register(MailCommandService.class, mailCommands);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var players = context.services().require(PlayerDirectory.class);
        var chat = requireNonNull(
                chatCommands,
                "ChatCommandService has not been initialized"
        );
        var privateService = requireNonNull(
                privateMessageCommands,
                "PrivateMessageCommandService has not been initialized"
        );
        var mailService = requireNonNull(
                mailCommands,
                "MailCommandService has not been initialized"
        );

        track(
                context,
                registry,
                "broadcast-command",
                new BroadcastCommand(chat)
        );
        track(
                context,
                registry,
                "broadcastworld-command",
                new BroadcastWorldCommand(chat)
        );
        track(
                context,
                registry,
                "helpop-command",
                new HelpOpCommand(chat, players)
        );
        track(
                context,
                registry,
                "ignore-command",
                new IgnoreCommand(privateService, players)
        );
        track(
                context,
                registry,
                "list-command",
                new ListCommand(chat, players)
        );
        track(
                context,
                registry,
                "mail-command",
                new MailCommand(mailService, players, privateService::knownNames)
        );
        track(
                context,
                registry,
                "me-command",
                new MeCommand(chat, players)
        );
        track(
                context,
                registry,
                "msg-command",
                new MsgCommand(privateService, players)
        );
        track(
                context,
                registry,
                "msgtoggle-command",
                new MsgToggleCommand(privateService, players)
        );
        track(
                context,
                registry,
                "reply-command",
                new ReplyCommand(privateService, players)
        );
        track(
                context,
                registry,
                "rtoggle-command",
                new ReplyToggleCommand(privateService, players)
        );
        track(
                context,
                registry,
                "socialspy-command",
                new SocialSpyCommand(privateService, players)
        );
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        scheduleMailSweep(context);
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(
                config,
                "MessagingConfig has not been initialized"
        );
        var candidate = reload.configs().require(
                "module.messaging",
                MessagingConfig.class
        ).validatedCopy();
        var service = (JsonMailService) requireNonNull(
                mail,
                "MailService has not been initialized"
        );
        var chat = requireNonNull(
                chatCommands,
                "ChatCommandService has not been initialized"
        );
        var privateMessages = requireNonNull(
                privateMessageCommands,
                "PrivateMessageCommandService has not been initialized"
        );
        var mailCommandsService = requireNonNull(
                mailCommands,
                "MailCommandService has not been initialized"
        );

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    service.configure(candidate);
                    chat.configure(candidate);
                    privateMessages.configure(candidate);
                    mailCommandsService.configure(candidate);
                    scheduleMailSweep(context);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previous;
                    service.configure(previous);
                    chat.configure(previous);
                    privateMessages.configure(previous);
                    mailCommandsService.configure(previous);
                    scheduleMailSweep(context);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private void scheduleMailSweep(ModuleContext context) {
        var service = requireNonNull(
                mail,
                "MailService has not been initialized"
        );
        var current = requireNonNull(
                config,
                "MessagingConfig has not been initialized"
        );
        if (mailSweep != null) {
            mailSweep.close();
        }

        final long period = Math.multiplyExact(current.expiredMailSweepSeconds, 20L);
        mailSweep = context.scope().own(context.scheduler().syncRepeating(
                () -> service
                        .purgeExpired(System.currentTimeMillis())
                        .whenComplete((_, failure) -> {
                            if (failure != null) {
                                context.logger().error(
                                        "Failed to persist expired mail cleanup",
                                        failure
                                );
                            }
                        }),
                20L,
                period
        ));
    }

}

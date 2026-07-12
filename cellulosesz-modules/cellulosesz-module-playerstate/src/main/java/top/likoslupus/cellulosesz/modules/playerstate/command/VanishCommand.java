package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.List;
import java.util.Map;

public final class VanishCommand extends AbstractPlayerStateCommand {

    private final VanishService vanish;
    private final MessageRenderer messages;
    private final LocaleResolver locales;

    public VanishCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states,
            VanishService vanish,
            MessageRenderer messages,
            LocaleResolver locales
    ) {
        super(platform, users, states);
        this.vanish = vanish;
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public List<String> aliases() {
        return List.of("v");
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.vanish";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/vanish [player] [on|off]";
    }

    @Override
    public String name() {
        return "vanish";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        var self = self(invocation);
        if (self.isEmpty()) return 0;

        var target = self.get();
        var stateIndex = 0;

        if (args.length > 0 && !toggleWord(args[0])) {
            if (!invocation.hasPermission("cellulosesz.playerstate.vanish.other")) {
                invocation.errorKey("commands.playerstate.vanish-command.error.1");
                return 0;
            }

            var online = invocation.resolvePlayer(args[0]).online();
            if (online.isEmpty()) {
                invocation.errorKey(
                        "commands.playerstate.vanish-command.error.2",
                        Map.of("value0", args[0])
                );
                return 0;
            }

            target = online.get();
            stateIndex = 1;
        }

        if (args.length > stateIndex + 1) {
            invocation.errorKey(
                    "commands.playerstate.vanish-command.error.3",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var enabled = args.length == stateIndex
                ? !vanish.vanished(target.uuid())
                : enabled(args[stateIndex]);
        var result = vanish.setVanished(target, enabled);
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }

        if (!target.uuid().equals(self.get().uuid())) {
            platform.sendMessage(target, messages.render(locales.locale(target), result.message()));
        }
        return result.success() ? 1 : 0;
    }

    private boolean toggleWord(String value) {
        return switch (value.toLowerCase()) {
            case "on", "off", "true", "false", "enable", "disable" -> true;
            default -> false;
        };
    }

    private boolean enabled(String value) {
        return switch (value.toLowerCase()) {
            case "on", "true", "enable" -> true;
            default -> false;
        };
    }

}

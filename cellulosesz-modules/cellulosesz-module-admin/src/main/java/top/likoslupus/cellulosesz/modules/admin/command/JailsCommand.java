package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class JailsCommand extends AbstractAdminCommand {

    private final JailService jails;

    public JailsCommand(
            PlatformService platform,
            UserService users,
            JailService jails
    ) {
        super(platform, users);
        this.jails = jails;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.jail.list";
    }

    @Override
    public String name() {
        return "jails";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var names = jails.jails().stream()
                .map(jail -> jail.name)
                .sorted()
                .toList();
        invocation.reply(names.isEmpty() ? "没有已设置的监狱。" : "监狱: " + String.join(", ", names));
        return 1;
    }

}

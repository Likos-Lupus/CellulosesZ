package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;

import java.util.List;
import java.util.Map;

public final class PingCommand implements CellCommand {

    @Override
    public List<String> aliases() {
        return List.of("pong");
    }

    @Override
    public String permission() {
        return "cellulosesz.command.ping";
    }

    @Override
    public String usage() {
        return "/ping [message]";
    }

    @Override
    public String name() {
        return "ping";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length == 0) {
            invocation.replyKey("commands.playerstate.ping.pong");
            return 1;
        }
        var message = String.join(" ", invocation.args()).strip();
        if (message.isEmpty() || message.codePoints().anyMatch(Character::isISOControl)) {
            invocation.errorKey("commands.playerstate.ping.invalid-message");
            return 0;
        }
        invocation.replyKey("commands.playerstate.ping.echo", Map.of("message", message));
        return 1;
    }

}

package top.likoslupus.cellulosesz.modules.sign.command;

import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.Map;

final class SignCommandSupport {

    static final String MODULE = "sign";

    private SignCommandSupport() {
    }

    static CommandDescriptor descriptor() {
        return new CommandDescriptor(
                MODULE,
                "editsign",
                "cellulosesz.command.editsign",
                CommandSourceKind.PLAYER_ONLY
        );
    }

    static int respond(
            MinecraftCommandPolicyContext policy,
            PlatformResult<?> result
    ) {
        var key = result.successful()
                ? "commands.sign.editsign.success"
                : "commands.sign.editsign.failed";

        policy.respond(
                result.successful(),
                LocalizedMessage.of(
                        key,
                        Map.of(
                                "status", result.status().name().toLowerCase(),
                                "reason", result.detail().isBlank() ? "-" : result.detail()
                        )
                )
        );
        return result.successful() ? 1 : 0;
    }

}

package top.likoslupus.cellulosesz.modules.item.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.WorkstationKind;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.application.WorkstationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class WorkstationCommand implements CommandContributor {

    private final WorkstationCommandService service;
    private final String root;
    private final List<String> aliases;
    private final WorkstationKind kind;

    public WorkstationCommand(
            WorkstationCommandService service,
            String root,
            List<String> aliases,
            WorkstationKind kind
    ) {
        this.service = requireNonNull(service, "service");
        this.root = requireNonNull(root, "root");
        this.aliases = List.copyOf(aliases);
        this.kind = requireNonNull(kind, "kind");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                root,
                "cellulosesz.item.workstation." + kind.permissionSegment(),
                CommandSourceKind.PLAYER_ONLY
        );

        var command = Commands.literal(root)
                .executes(commandContext -> ItemCommandSupport.sync(
                        context,
                        commandContext,
                        descriptor,
                        "open workstation",
                        policy -> {
                            var player = ItemCommandSupport.current(policy);

                            return player
                                    .<PlatformResult<?>>map(value -> service.open(value, kind))
                                    .orElseGet(() -> PlatformResult.failure(
                                            PlatformOperationStatus.INVALID_SOURCE,
                                            "player-only"
                                    ));
                        }
                ));

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                aliases,
                "commands.description." + root,
                "/" + root,
                command
        );

        aliases.forEach(alias -> context.registerAlias(
                moduleId(),
                descriptor,
                alias,
                node
        ));
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

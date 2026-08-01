package top.likoslupus.cellulosesz.modules.world.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class GcCommand implements CommandContributor {

    private final WorldPlatformService worlds;

    public GcCommand(WorldPlatformService worlds) {
        this.worlds = requireNonNull(worlds, "worlds");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "gc",
                "cellulosesz.command.gc",
                CommandSourceKind.ANY
        );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.gc",
                "/gc",
                Commands.literal("gc")
                        .executes(command -> WorldCommandSupport.sync(
                                context,
                                command,
                                descriptor,
                                "diagnostics",
                                _ -> worlds.diagnostics()
                        ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

package top.likoslupus.cellulosesz.modules.world.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.world.BackupService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class BackupCommand implements CommandContributor {

    private final BackupService backups;

    public BackupCommand(BackupService backups) {
        this.backups = requireNonNull(backups, "backups");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "backup",
                "cellulosesz.world.backup",
                CommandSourceKind.ANY
        );
        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.backup",
                "/backup",
                Commands.literal("backup")
                        .executes(command -> WorldCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "backup",
                        policy -> {
                            if (backups.running()) {
                                return CompletableFuture.completedFuture(
                                        PlatformResult.failure(
                                                PlatformOperationStatus.CONFLICT,
                                                "backup-already-running"
                                        )
                                );
                            }

                            policy.respond(
                                    true,
                                    LocalizedMessage.of("commands.world.backup-started")
                            );

                            return backups.createBackup()
                                    .thenApply(PlatformResult::success)
                                    .exceptionally(failure -> PlatformResult.failure(
                                            PlatformOperationStatus.INTERNAL_ERROR,
                                            failure.getClass().getSimpleName()
                                    ));
                        }
                ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

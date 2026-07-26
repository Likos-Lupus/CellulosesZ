package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.world.BackupService;

import java.util.Map;

public final class BackupCommand implements CellCommand {

    private final BackupService backups;

    public BackupCommand(BackupService backups) {
        this.backups = backups;
    }

    @Override
    public String permission() {
        return "cellulosesz.world.backup";
    }

    @Override
    public String name() {
        return "backup";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (backups.running()) {
            invocation.errorKey("commands.world.backup-running");
            return 0;
        }
        invocation.replyKey("commands.world.backup-started");
        backups.createBackup().whenComplete((path, failure) -> {
            if (failure != null) invocation.errorKey("commands.world.backup-failed");
            else invocation.replyKey("commands.world.backup-complete", Map.of("file", path.getFileName()));
        });
        return 1;
    }

}

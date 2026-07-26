package top.likoslupus.cellulosesz.api.world;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface BackupService {

    CompletableFuture<Path> createBackup();

    boolean running();

}

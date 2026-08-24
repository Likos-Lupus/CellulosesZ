package top.likoslupus.cellulosesz.common.world;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface BackupPlatformService extends AutoCloseable {

    CompletableFuture<BackupResult> create(Path destinationDirectory);

    @Override
    void close();

}

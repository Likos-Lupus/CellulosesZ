package top.likoslupus.cellulosesz.common.world;

import java.nio.file.Path;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

import static java.util.Objects.requireNonNull;

public record BackupResult(
        Path archive,
        long files,
        long bytes
) {

    public BackupResult {
        archive = requireNonNull(archive, "archive").toAbsolutePath().normalize();
        requireNonNegative(files, "files");
        requireNonNegative(bytes, "bytes");
    }

}

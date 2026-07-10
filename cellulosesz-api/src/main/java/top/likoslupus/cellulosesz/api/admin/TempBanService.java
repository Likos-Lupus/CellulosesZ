package top.likoslupus.cellulosesz.api.admin;

import java.util.Optional;
import java.util.UUID;

public interface TempBanService {

    AdminResult tempBan(
            String target,
            String actor,
            long durationMillis,
            String reason
    );

    AdminResult tempBanIp(
            String target,
            String actor,
            long durationMillis,
            String reason
    );

    Optional<BanRecord> active(UUID uuid, String name);

    Optional<BanRecord> activeIp(String address);

    void purgeExpired();

}

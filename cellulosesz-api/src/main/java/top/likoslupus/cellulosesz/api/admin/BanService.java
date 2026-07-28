package top.likoslupus.cellulosesz.api.admin;

import java.util.UUID;

public interface BanService {

    AdminResult ban(
            UUID targetId,
            String targetName,
            String actor,
            String reason
    );

    AdminResult unban(
            UUID targetId,
            String targetName,
            String actor
    );

    AdminResult banIp(
            String target,
            String actor,
            String reason
    );

    AdminResult unbanIp(
            String target,
            String actor
    );

    AdminResult kick(
            String target,
            String actor,
            String reason
    );

    AdminResult kickAll(
            String actor,
            String reason
    );

}

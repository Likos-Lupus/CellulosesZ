package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;

import java.net.InetAddress;
import java.util.UUID;

public interface BanService {

    AdminResult ban(
            UUID targetId,
            String targetName,
            AdminActor actor,
            String reason
    );

    AdminResult unban(
            UUID targetId,
            String targetName,
            AdminActor actor
    );

    AdminResult banIp(
            InetAddress target,
            AdminActor actor,
            String reason
    );

    AdminResult unbanIp(
            InetAddress target,
            AdminActor actor
    );

    AdminResult kick(
            CellPlayer target,
            String reason
    );

}

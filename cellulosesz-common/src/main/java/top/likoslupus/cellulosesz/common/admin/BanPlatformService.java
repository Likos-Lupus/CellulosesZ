package top.likoslupus.cellulosesz.common.admin;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.net.InetAddress;

public interface BanPlatformService {

    BanPlatformResult banUser(BanUserRequest request);

    BanPlatformResult pardonUser(PlayerProfileId target);

    BanPlatformResult banIp(BanIpRequest request);

    BanPlatformResult pardonIp(InetAddress address);

    PlatformResult<Boolean> isUserBanned(PlayerProfileId target);

    PlatformResult<Boolean> isIpBanned(InetAddress address);

    BanPlatformResult disconnectMatchingPlayers(BanDisconnectRequest request);

}

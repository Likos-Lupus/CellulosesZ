package top.likoslupus.cellulosesz.api.platform.admin;

import java.net.InetAddress;

public interface BanPlatformService {

    BanPlatformResult banUser(BanUserRequest request);

    BanPlatformResult pardonUser(PlayerProfileId target);

    BanPlatformResult banIp(BanIpRequest request);

    BanPlatformResult pardonIp(InetAddress address);

    boolean isUserBanned(PlayerProfileId target);

    boolean isIpBanned(InetAddress address);

    BanPlatformResult disconnectMatchingPlayers(BanDisconnectRequest request);

}

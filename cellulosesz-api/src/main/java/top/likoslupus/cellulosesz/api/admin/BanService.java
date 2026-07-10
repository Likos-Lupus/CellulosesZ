package top.likoslupus.cellulosesz.api.admin;

public interface BanService {

    AdminResult ban(String target, String actor, String reason);

    AdminResult unban(String target, String actor);

    AdminResult banIp(String target, String actor, String reason);

    AdminResult unbanIp(String target, String actor);

    AdminResult kick(String target, String actor, String reason);

    AdminResult kickAll(String actor, String reason);

}

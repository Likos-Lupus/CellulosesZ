package top.likoslupus.cellulosesz.modules.admin.data;

import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailedPlayer;

import java.util.ArrayList;
import java.util.List;

public final class JailDocument {

    public int schema = 1;
    public List<Jail> jails = new ArrayList<>();
    public List<JailedPlayer> jailed = new ArrayList<>();

}

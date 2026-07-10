package top.likoslupus.cellulosesz.modules.admin.data;

import top.likoslupus.cellulosesz.api.admin.BanRecord;

import java.util.ArrayList;
import java.util.List;

public final class MuteDocument {

    public int schema = 1;
    public List<BanRecord> records = new ArrayList<>();

}

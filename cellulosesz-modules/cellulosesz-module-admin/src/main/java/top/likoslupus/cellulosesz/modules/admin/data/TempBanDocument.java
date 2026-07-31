package top.likoslupus.cellulosesz.modules.admin.data;

import java.util.ArrayList;
import java.util.List;

public final class TempBanDocument {

    public List<Record> records = new ArrayList<>();

    public static final class Record {

        public boolean ip;
        public String uuid = "";
        public String name = "";
        public String address = "";
        public String reason = "";
        public String actorUuid = "";
        public String actorName = "";
        public long createdAt;
        public long expiresAt;

    }

}

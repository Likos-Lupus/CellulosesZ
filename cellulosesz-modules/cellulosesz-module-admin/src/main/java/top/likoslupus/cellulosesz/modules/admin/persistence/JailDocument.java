package top.likoslupus.cellulosesz.modules.admin.persistence;

import java.util.ArrayList;
import java.util.List;

public final class JailDocument {

    public List<JailEntry> jails = new ArrayList<>();
    public List<JailedEntry> jailed = new ArrayList<>();

    public static final class JailEntry {

        public String name = "";
        public LocationDocument location = new LocationDocument();
        public String createdBy = "";
        public long createdAt;

    }

    public static final class JailedEntry {

        public String uuid = "";
        public String name = "";
        public String jail = "";
        public String reason = "";
        public String actorUuid = "";
        public String actorName = "";
        public long createdAt;
        public boolean permanent;
        public long expiresAt;
        public LocationDocument returnLocation = new LocationDocument();
        public boolean hasReturnLocation;
        public String state = "ACTIVE";

    }

}

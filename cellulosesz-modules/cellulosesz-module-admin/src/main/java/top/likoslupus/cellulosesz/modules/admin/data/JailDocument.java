package top.likoslupus.cellulosesz.modules.admin.data;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.ArrayList;
import java.util.List;

public final class JailDocument {

    public List<JailEntry> jails = new ArrayList<>();
    public List<JailedEntry> jailed = new ArrayList<>();

    public static final class JailEntry {

        public String name = "";
        public CellLocation location = new CellLocation(
                "minecraft:overworld",
                0, 64, 0,
                0, 0
        );
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
        public CellLocation returnLocation = new CellLocation(
                "minecraft:overworld",
                0, 64, 0,
                0, 0
        );
        public boolean hasReturnLocation;
        public String state = "ACTIVE";

    }

}

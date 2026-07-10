package top.likoslupus.cellulosesz.api.teleport;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class TeleportOptions {

    private boolean safe = true;
    private boolean rememberBack = true;
    private boolean allowCrossWorld = true;
    private boolean keepVehicle;
    private int warmupSeconds;

}

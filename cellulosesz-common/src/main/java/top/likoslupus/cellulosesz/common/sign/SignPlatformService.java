package top.likoslupus.cellulosesz.common.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface SignPlatformService {

    PlatformResult<SignSnapshot> target(CellPlayer player, int maximumDistance);

    PlatformResult<SignSnapshot> compareAndReplace(SignWriteRequest request);

    PlatformResult<Void> compareAndBreak(SignBreakRequest request);

}

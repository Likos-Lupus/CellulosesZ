package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface EntityRemovalPlatformService {

    PlatformResult<EntityRemovalResult> remove(EntityRemovalRequest request);

}

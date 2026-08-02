package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

/**
 * Prepared exact-slot inventory mutation with explicit conflict and rollback outcomes.
 */
public interface InventoryMutation {

    PlatformResult<Void> commit();

    PlatformResult<Void> rollback();

}

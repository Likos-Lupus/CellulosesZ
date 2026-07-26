package top.likoslupus.cellulosesz.api.item;

/**
 * Prepared, all-or-nothing inventory mutation.
 *
 * <p>A grant may be committed once. A committed grant can be rolled back while the affected stacks are unchanged.
 * Implementations must not partially modify the inventory when {@link #commit()} returns {@code false}.</p>
 */
public interface InventoryGrant {

    boolean commit();

    boolean rollback();

}

package top.likoslupus.cellulosesz.api.item;

/**
 * Prepared all-or-nothing inventory mutation.
 *
 * <p>The platform must compare the current slots with the prepared snapshot before commit. Rollback must restore the
 * exact pre-commit stacks and may fail only when a caller or another operation changed an affected slot after
 * commit.</p>
 */
public interface InventoryMutation {

    boolean commit();

    boolean rollback();

}

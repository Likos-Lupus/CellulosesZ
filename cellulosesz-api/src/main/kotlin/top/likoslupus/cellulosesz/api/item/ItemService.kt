package top.likoslupus.cellulosesz.api.item

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult

/** Item application service that preserves validation and platform failure states. */
public interface ItemService {

    public fun itemNames(): Set<String>

    public fun parse(input: String): PlatformResult<ItemDescriptor>

    public fun valid(item: ItemDescriptor): PlatformResult<Boolean>

    public fun blacklisted(item: ItemDescriptor): Boolean

    public fun maxStackSize(item: ItemDescriptor): PlatformResult<Int>

    public fun give(
        player: CellPlayer,
        item: ItemDescriptor
    ): PlatformResult<ItemGrantResult>

    public fun count(
        player: CellPlayer,
        item: ItemDescriptor
    ): PlatformResult<Int>

    public fun take(
        player: CellPlayer,
        item: ItemDescriptor
    ): PlatformResult<Void>

    public fun heldItemId(player: CellPlayer): PlatformResult<String>

}

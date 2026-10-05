package top.likoslupus.cellulosesz.utility

import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.utility.item.*
import top.likoslupus.cellulosesz.utility.kit.*
import java.time.*
import java.util.*

internal object ImmediateServerThreadRunner : ServerThreadRunner {

    override suspend fun <T> run(block: () -> T): T =
        block()

}

internal class FakeKnownPlayerResolver : KnownPlayerResolver {

    private val online = HashMap<UUID, KnownPlayerIdentity>()

    fun add(identity: KnownPlayerIdentity) {
        online[identity.id] = identity
    }

    fun remove(id: UUID) {
        online.remove(id)
    }

    override fun onlineByName(name: String): KnownPlayerIdentity? =
        online.values.firstOrNull {
            it.name.equals(
                name,
                ignoreCase = true
            )
        }

    override fun onlineById(id: UUID): KnownPlayerIdentity? =
        online[id]

    override fun knownByName(name: String): KnownPlayerIdentity? =
        onlineByName(name)

    override fun knownById(id: UUID): KnownPlayerIdentity? =
        onlineById(id)

}

internal class MutableClock(private var current: Instant) : Clock() {

    override fun getZone(): ZoneOffset =
        ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock =
        this

    override fun instant(): Instant =
        current

    fun advance(by: Duration) {
        current = current.plus(by)
    }

}

internal class FakeKitRepository : KitRepository {

    val definitions = HashMap<KitName, KitDefinition>()

    override suspend fun loadAll(): Map<KitName, KitDefinition> =
        definitions.toMap()

    override suspend fun create(definition: KitDefinition): Boolean =
        when {
            definitions.containsKey(definition.name) -> false
            else -> {
                definitions[definition.name] = definition
                true
            }
        }

    override suspend fun replace(definition: KitDefinition): Boolean =
        when {
            !definitions.containsKey(definition.name) -> false
            else -> {
                definitions[definition.name] = definition
                true
            }
        }

    override suspend fun remove(name: KitName): Boolean =
        definitions.remove(name) != null

}

internal class FakeKitClaimRepository : KitClaimRepository {

    private val claims = HashMap<UUID, MutableMap<KitName, KitClaim>>()

    var failReserve = false
    var failFinalize = false

    fun statusOf(playerId: UUID, name: KitName): KitClaimStatus? =
        claims[playerId]?.get(name)?.status

    override suspend fun load(playerId: UUID): Map<KitName, KitClaim> =
        claims[playerId]?.toMap() ?: emptyMap()

    override suspend fun reserve(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    ) {
        if (failReserve) {
            throw KitClaimDataException("reserve failed")
        }
        claims.getOrPut(player.id) { HashMap() }[kit.name] = KitClaim(
            kit.id.value,
            at,
            KitClaimStatus.RESERVED
        )
    }

    override suspend fun markDelivered(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    ) {
        if (failFinalize) {
            throw KitClaimDataException("finalize failed")
        }
        claims.getOrPut(player.id) { HashMap() }[kit.name] = KitClaim(
            kit.id.value,
            at,
            KitClaimStatus.DELIVERED
        )
    }

    override suspend fun reset(playerId: UUID, name: KitName): Boolean =
        claims[playerId]?.remove(name) != null

}

internal class FakeKitInventoryBackend : KitInventoryBackend {

    var captureResult: KitCaptureResult = KitCaptureResult.Captured(emptyList())
    var preflightResult: KitPreflightResult = KitPreflightResult.Fits
    var deliveryResult: KitDeliveryResult = KitDeliveryResult.Delivered(0)

    var preflightCalls = 0
    var deliverCalls = 0

    override fun capture(playerId: UUID): KitCaptureResult = captureResult

    override fun preflight(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
    ): KitPreflightResult {
        preflightCalls++
        return preflightResult
    }

    override fun deliver(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
    ): KitDeliveryResult {
        deliverCalls++
        return deliveryResult
    }

}

internal class FakeItemUtilityBackend : ItemUtilityBackend {

    var repairResult: RepairBackendResult = RepairBackendResult.NothingToRepair
    var moreResult: MoreBackendResult = MoreBackendResult.NothingToFill
    var condenseResult: CondenseBackendResult = CondenseBackendResult.NothingToCondense

    override fun repair(
        playerId: UUID,
        scope: RepairScope,
        allowEnchanted: Boolean,
        includeArmor: Boolean,
    ): RepairBackendResult = repairResult

    override fun more(playerId: UUID, amount: Int?): MoreBackendResult =
        moreResult

    override fun condense(playerId: UUID): CondenseBackendResult =
        condenseResult

}

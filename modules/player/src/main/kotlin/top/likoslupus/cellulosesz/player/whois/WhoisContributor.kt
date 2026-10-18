package top.likoslupus.cellulosesz.player.whois

import top.likoslupus.cellulosesz.core.text.model.DetailField
import java.util.*

/**
 * A narrow port so another feature (economy, administration, an addon) can append fields to
 * `/whois`. The composition root registers contributors on the player feature; the player module
 * never depends on those features.
 *
 * [fields] is suspended so a contributor may read durable data through its own feature port (for
 * example an economy balance). An implementation that needs server-thread state hops there itself;
 * the contributor list is invoked outside the live snapshot.
 */
public fun interface WhoisContributor {

    public suspend fun fields(playerId: UUID): List<DetailField>

}

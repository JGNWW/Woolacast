package nl.woolacast.data.maker

import java.time.LocalDate
import nl.woolacast.data.local.FollowedMaker
import nl.woolacast.domain.Channel
import nl.woolacast.domain.ChannelShow

/**
 * Wanneer een show van een gevolgd kanaal een melding waard is: hij was er
 * niet toen je keek, er ging nog geen melding over, en Apple kent hem pas
 * kort. Alleen kanalen: zonder kanaal weten we niet zeker of een show nieuw is,
 * en een melding die niet klopt is erger dan geen melding.
 */
object MakerNews {

    const val NEW_SHOW_DAYS = 14L

    fun toNotify(maker: FollowedMaker, channel: Channel?, today: LocalDate = LocalDate.now()): List<ChannelShow> {
        if (channel == null || maker.knownShowIds.isEmpty()) return emptyList()
        val skip = (maker.knownShowIds + maker.notifiedShowIds).toSet()
        val since = today.minusDays(NEW_SHOW_DAYS)
        return channel.newShows.filter { show ->
            show.id !in skip && runCatching { LocalDate.parse(show.createdDate?.take(10)) }.getOrNull()?.isBefore(since) == false
        }
    }
}

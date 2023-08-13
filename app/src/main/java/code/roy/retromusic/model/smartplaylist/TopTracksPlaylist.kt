package code.roy.retromusic.model.smartplaylist

import androidx.annotation.Keep
import code.roy.retromusic.App
import code.roy.retromusic.R
import code.roy.retromusic.model.Song
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
class TopTracksPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.my_top_tracks),
    iconRes = R.drawable.ic_trending_up
) {
    override fun songs(): List<Song> {
        return topPlayedRepository.topTracks()
    }
}

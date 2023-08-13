package code.roy.retromusic.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import code.roy.retromusic.repository.RealPlaylistRepository
import code.roy.retromusic.util.MusicUtil
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Keep
@Parcelize
open class Playlist(
    val id: Long,
    val name: String,
) : Parcelable, KoinComponent {

    companion object {
        val empty = Playlist(id = -1, name = "")
    }

    // this default implementation covers static playlists
    fun getSongs(): List<Song> {
        return RealPlaylistRepository(get()).playlistSongs(id)
    }

    open fun getInfoString(context: Context): String {
        val songCount = getSongs().size
        val songCountString = MusicUtil.getSongCountString(context, songCount)
        return MusicUtil.buildInfoString(
            string1 = songCountString,
            string2 = ""
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Playlist

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

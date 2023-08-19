package code.roy.retromusic.adapter.playlist

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentActivity
import code.roy.appthemehelper.util.ATHUtil
import code.roy.retromusic.R
import code.roy.retromusic.adapter.base.AbsMultiSelectAdapter
import code.roy.retromusic.adapter.base.MediaEntryViewHolder
import code.roy.retromusic.db.PlaylistEntity
import code.roy.retromusic.db.PlaylistWithSongs
import code.roy.retromusic.db.toSongs
import code.roy.retromusic.extensions.dipToPix
import code.roy.retromusic.glide.RetroGlideExtension.playlistOptions
import code.roy.retromusic.glide.playlistPreview.PlaylistPreview
import code.roy.retromusic.helper.SortOrder
import code.roy.retromusic.helper.menu.PlaylistMenuHelper
import code.roy.retromusic.helper.menu.SongsMenuHelper
import code.roy.retromusic.itf.IPlaylistClickListener
import code.roy.retromusic.model.Song
import code.roy.retromusic.util.MusicUtil
import code.roy.retromusic.util.PreferenceUtil
import com.bumptech.glide.Glide
import me.zhanghai.android.fastscroll.PopupTextProvider

class PlaylistAdapter(
    override val activity: FragmentActivity,
    var dataSet: List<PlaylistWithSongs>,
    private var itemLayoutRes: Int,
    private val listener: IPlaylistClickListener,
) : AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, PlaylistWithSongs>(
    activity,
    R.menu.menu_playlists_selection
), PopupTextProvider {

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapDataSet(dataSet: List<PlaylistWithSongs>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].playlistEntity.playListId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view)
    }

    private fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    private fun getPlaylistTitle(playlist: PlaylistEntity): String {
        return playlist.playlistName.ifEmpty { "-" }
    }

    private fun getPlaylistText(playlist: PlaylistWithSongs): String {
        return MusicUtil.getPlaylistInfoString(activity, playlist.songs.toSongs())
    }

    override fun getPopupText(position: Int): String {
        val sectionName: String = when (PreferenceUtil.playlistSortOrder) {
            SortOrder.PlaylistSortOrder.PLAYLIST_A_Z, SortOrder.PlaylistSortOrder.PLAYLIST_Z_A -> dataSet[position].playlistEntity.playlistName
            SortOrder.PlaylistSortOrder.PLAYLIST_SONG_COUNT, SortOrder.PlaylistSortOrder.PLAYLIST_SONG_COUNT_DESC -> dataSet[position].songs.size.toString()
            else -> {
                return ""
            }
        }
        return MusicUtil.getSectionName(sectionName)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = dataSet[position]
        holder.itemView.isActivated = isChecked(playlist)
        holder.title?.text = getPlaylistTitle(playlist.playlistEntity)
        holder.text?.text = getPlaylistText(playlist)
        holder.menu?.isGone = isChecked(playlist)
        if (itemLayoutRes == R.layout.item_list) {
            holder.image?.setPadding(activity.dipToPix(8F).toInt())
            holder.image?.setImageDrawable(getIconRes())
        } else {
            Glide.with(activity)
                .load(PlaylistPreview(playlist))
                .playlistOptions()
                .into(holder.image!!)
        }
    }

    private fun getIconRes(): Drawable =
        code.roy.appthemehelper.util.TintHelper.createTintedDrawable(
            /* context = */ activity,
            /* res = */ R.drawable.ic_playlist_play,
            /* color = */ ATHUtil.resolveColor(activity, android.R.attr.colorControlNormal)
        )

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): PlaylistWithSongs {
        return dataSet[position]
    }

    override fun getName(model: PlaylistWithSongs): String {
        return model.playlistEntity.playlistName
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<PlaylistWithSongs>) {
        when (menuItem.itemId) {
            else -> SongsMenuHelper.handleMenuClick(
                activity = activity,
                songs = getSongList(selection),
                menuItemId = menuItem.itemId
            )
        }
    }

    private fun getSongList(playlists: List<PlaylistWithSongs>): List<Song> {
        val songs = mutableListOf<Song>()
        playlists.forEach {
            songs.addAll(it.songs.toSongs())
        }
        return songs
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        init {
            menu?.setOnClickListener { view ->
                val popupMenu = PopupMenu(activity, view)
                popupMenu.inflate(R.menu.menu_item_playlist)
                popupMenu.setOnMenuItemClickListener { item ->
                    PlaylistMenuHelper.handleMenuClick(
                        activity = activity,
                        playlistWithSongs = dataSet[layoutPosition],
                        item = item
                    )
                }
                popupMenu.show()
            }

            imageTextContainer?.apply {
                cardElevation = 0f
                setCardBackgroundColor(Color.TRANSPARENT)
            }
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                itemView.transitionName = "playlist"
                listener.onPlaylistClick(dataSet[layoutPosition], itemView)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            toggleChecked(layoutPosition)
            return true
        }
    }

    companion object {
        val TAG: String = PlaylistAdapter::class.java.simpleName
    }
}

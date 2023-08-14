package code.roy.retromusic.fragments.songs

import android.os.Bundle
import android.view.*
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import code.roy.monkey.retromusic.extensions.setUpMediaRouteButton
import code.roy.retromusic.R
import code.roy.retromusic.adapter.song.SongAdapter
import code.roy.retromusic.fragments.GridStyle
import code.roy.retromusic.fragments.ReloadType
import code.roy.retromusic.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import code.roy.retromusic.helper.SortOrder
import code.roy.retromusic.util.PreferenceUtil
import code.roy.retromusic.util.RetroUtil

class SongsFragment : AbsRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getSongs().observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        }
    }

    override val titleRes: Int
        get() = R.string.songs

    override val emptyMessage: Int
        get() = R.string.no_songs

    override val isShuffleVisible: Boolean
        get() = true

    override fun onShuffleClicked() {
        libraryViewModel.shuffleSongs()
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), getGridSize())
    }

    override fun createAdapter(): SongAdapter {
        val dataSet = if (adapter == null) mutableListOf() else adapter!!.dataSet
        return SongAdapter(
            activity = requireActivity(),
            dataSet = dataSet,
            itemLayoutRes = itemLayoutRes()
        )
    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.songGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.songGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.songGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.songGridSizeLand = gridColumns
    }

    override fun setGridSize(gridSize: Int) {
        adapter?.notifyDataSetChanged()
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.songSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.songSortOrder = sortOrder
    }

    @LayoutRes
    override fun loadLayoutRes(): Int {
        return PreferenceUtil.songGridStyle.layoutResId
    }

    override fun saveLayoutRes(@LayoutRes layoutRes: Int) {
        PreferenceUtil.songGridStyle = GridStyle.values().first { gridStyle ->
            gridStyle.layoutResId == layoutRes
        }
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
        if (RetroUtil.isLandscape) {
            gridSizeItem.setTitle(R.string.action_grid_size_land)
        }
        setUpGridSizeMenu(gridSizeItem.subMenu!!)
        val layoutItem = menu.findItem(R.id.action_layout_type)
        setupLayoutMenu(layoutItem.subMenu!!)
        setUpSortOrderMenu(menu.findItem(R.id.action_sort_order).subMenu!!)
        //Setting up cast button
        requireContext().setUpMediaRouteButton(menu)
    }

    private fun setUpSortOrderMenu(sortOrderMenu: SubMenu) {
        val currentSortOrder: String? = getSortOrder()
        sortOrderMenu.clear()
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_default_sort_order,
            /* p2 = */ 0,
            /* p3 = */ R.string.sort_order_default
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_DEFAULT
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_asc,
            /* p2 = */ 0,
            /* p3 = */ R.string.sort_order_a_z
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_A_Z
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_desc,
            /* p2 = */ 1,
            /* p3 = */ R.string.sort_order_z_a
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_Z_A
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_artist,
            /* p2 = */ 2,
            /* p3 = */ R.string.sort_order_artist
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_ARTIST
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_album,
            /* p2 = */ 3,
            /* p3 = */ R.string.sort_order_album
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_ALBUM
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_year,
            /* p2 = */ 4,
            /* p3 = */ R.string.sort_order_year
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_YEAR
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_date,
            /* p2 = */ 5,
            /* p3 = */ R.string.sort_order_date
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_DATE
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_date_modified,
            /* p2 = */ 6,
            /* p3 = */ R.string.sort_order_date_modified
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_DATE_MODIFIED
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_composer,
            /* p2 = */ 7,
            /* p3 = */ R.string.sort_order_composer
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.COMPOSER
        sortOrderMenu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_song_sort_order_album_artist,
            /* p2 = */ 8,
            /* p3 = */ R.string.album_artist
        ).isChecked =
            currentSortOrder == SortOrder.SongSortOrder.SONG_ALBUM_ARTIST

        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    private fun setupLayoutMenu(subMenu: SubMenu) {
        when (itemLayoutRes()) {
            R.layout.v_item_card -> subMenu.findItem(R.id.action_layout_card).isChecked = true
            R.layout.v_item_grid -> subMenu.findItem(R.id.action_layout_normal).isChecked = true
            R.layout.v_item_card_color ->
                subMenu.findItem(R.id.action_layout_colored_card).isChecked = true

            R.layout.v_item_grid_circle ->
                subMenu.findItem(R.id.action_layout_circular).isChecked = true

            R.layout.v_image -> subMenu.findItem(R.id.action_layout_image).isChecked = true
            R.layout.v_item_image_gradient ->
                subMenu.findItem(R.id.action_layout_gradient_image).isChecked = true
        }
    }

    private fun setUpGridSizeMenu(gridSizeMenu: SubMenu) {
        when (getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked = true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val gridSize: Int = maxGridSize
        if (gridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        }
        if (gridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        }
        if (gridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        }
        if (gridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        }
        if (gridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        }
        if (gridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (handleGridSizeMenuItem(item)) {
            return true
        }
        if (handleLayoutResType(item)) {
            return true
        }
        if (handleSortOrderMenuItem(item)) {
            return true
        }
        return super.onMenuItemSelected(item)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        val sortOrder: String = when (item.itemId) {
            R.id.action_song_default_sort_order -> SortOrder.SongSortOrder.SONG_DEFAULT
            R.id.action_song_sort_order_asc -> SortOrder.SongSortOrder.SONG_A_Z
            R.id.action_song_sort_order_desc -> SortOrder.SongSortOrder.SONG_Z_A
            R.id.action_song_sort_order_artist -> SortOrder.SongSortOrder.SONG_ARTIST
            R.id.action_song_sort_order_album_artist -> SortOrder.SongSortOrder.SONG_ALBUM_ARTIST
            R.id.action_song_sort_order_album -> SortOrder.SongSortOrder.SONG_ALBUM
            R.id.action_song_sort_order_year -> SortOrder.SongSortOrder.SONG_YEAR
            R.id.action_song_sort_order_date -> SortOrder.SongSortOrder.SONG_DATE
            R.id.action_song_sort_order_composer -> SortOrder.SongSortOrder.COMPOSER
            R.id.action_song_sort_order_date_modified -> SortOrder.SongSortOrder.SONG_DATE_MODIFIED
            else -> PreferenceUtil.songSortOrder
        }
        if (sortOrder != PreferenceUtil.songSortOrder) {
            item.isChecked = true
            setAndSaveSortOrder(sortOrder)
            return true
        }
        return false
    }

    private fun handleLayoutResType(item: MenuItem): Boolean {
        val layoutRes = when (item.itemId) {
            R.id.action_layout_normal -> R.layout.v_item_grid
            R.id.action_layout_card -> R.layout.v_item_card
            R.id.action_layout_colored_card -> R.layout.v_item_card_color
            R.id.action_layout_circular -> R.layout.v_item_grid_circle
            R.id.action_layout_image -> R.layout.v_image
            R.id.action_layout_gradient_image -> R.layout.v_item_image_gradient
            else -> PreferenceUtil.songGridStyle.layoutResId
        }
        if (layoutRes != PreferenceUtil.songGridStyle.layoutResId) {
            item.isChecked = true
            setAndSaveLayoutRes(layoutRes)
            return true
        }
        return false
    }

    private fun handleGridSizeMenuItem(item: MenuItem): Boolean {
        val gridSize = when (item.itemId) {
            R.id.action_grid_size_1 -> 1
            R.id.action_grid_size_2 -> 2
            R.id.action_grid_size_3 -> 3
            R.id.action_grid_size_4 -> 4
            R.id.action_grid_size_5 -> 5
            R.id.action_grid_size_6 -> 6
            R.id.action_grid_size_7 -> 7
            R.id.action_grid_size_8 -> 8
            else -> 0
        }
        if (gridSize > 0) {
            item.isChecked = true
            setAndSaveGridSize(gridSize)
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    companion object {
        @JvmField
        var TAG: String = SongsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): SongsFragment {
            return SongsFragment()
        }
    }
}

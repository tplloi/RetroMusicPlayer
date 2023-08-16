package code.roy.retromusic.fragments.player.classic

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.roy.retromusic.util.color.MediaNotificationProcessor
import code.roy.appthemehelper.util.ColorUtil
import code.roy.retromusic.R
import code.roy.retromusic.adapter.song.PlayingQueueAdapter
import code.roy.retromusic.databinding.FragmentClassicPlayerBinding
import code.roy.retromusic.extensions.getSongInfo
import code.roy.retromusic.extensions.hide
import code.roy.retromusic.extensions.setNavigationBarColor
import code.roy.retromusic.extensions.show
import code.roy.retromusic.extensions.surfaceColor
import code.roy.retromusic.extensions.whichFragment
import code.roy.retromusic.fragments.MusicSeekSkipTouchListener
import code.roy.retromusic.fragments.base.AbsPlayerControlsFragment
import code.roy.retromusic.fragments.base.AbsPlayerFragment
import code.roy.retromusic.fragments.base.goToAlbum
import code.roy.retromusic.fragments.base.goToArtist
import code.roy.retromusic.fragments.other.VolumeFragment
import code.roy.retromusic.fragments.player.PlayerAlbumCoverFragment
import code.roy.retromusic.helper.MusicPlayerRemote
import code.roy.retromusic.helper.MusicProgressViewUpdateHelper
import code.roy.retromusic.helper.PlayPauseButtonOnClickHandler
import code.roy.retromusic.misc.SimpleOnSeekbarChangeListener
import code.roy.retromusic.model.Song
import code.roy.retromusic.service.MusicService
import code.roy.retromusic.util.MusicUtil
import code.roy.retromusic.util.PreferenceUtil
import code.roy.retromusic.util.ViewUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils

class ClassicPlayerFragment : AbsPlayerFragment(R.layout.fragment_classic_player),
    View.OnLayoutChangeListener,
    MusicProgressViewUpdateHelper.Callback {

    private var _binding: FragmentClassicPlayerBinding? = null
    private val binding get() = _binding!!

    private var lastColor: Int = 0
    private var lastPlaybackControlsColor: Int = 0
    private var lastDisabledPlaybackControlsColor: Int = 0
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper
    private var volumeFragment: VolumeFragment? = null
    private lateinit var shapeDrawable: MaterialShapeDrawable
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var recyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private var recyclerViewTouchActionGuardManager: RecyclerViewTouchActionGuardManager? = null
    private var playingQueueAdapter: PlayingQueueAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager

    private val bottomSheetCallbackList = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            mainActivity.getBottomSheetBehavior().isDraggable = false
            binding.playerQueueSheet.setContentPadding(
                /* left = */ binding.playerQueueSheet.contentPaddingLeft,
                /* top = */ (slideOffset * binding.statusBar.height).toInt(),
                /* right = */ binding.playerQueueSheet.contentPaddingRight,
                /* bottom = */ binding.playerQueueSheet.contentPaddingBottom
            )

            shapeDrawable.interpolation = 1 - slideOffset
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED,
                BottomSheetBehavior.STATE_DRAGGING,
                -> {
                    mainActivity.getBottomSheetBehavior().isDraggable = false
                }

                BottomSheetBehavior.STATE_COLLAPSED -> {
                    resetToCurrentPosition()
                    mainActivity.getBottomSheetBehavior().isDraggable = true
                }

                else -> {
                    mainActivity.getBottomSheetBehavior().isDraggable = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentClassicPlayerBinding.bind(view)
        setupPanel()
        setUpMusicControllers()
        setUpPlayerToolbar()
        hideVolumeIfAvailable()
        setupRecyclerView()

        // Check if the device is in landscape mode
        if (isLandscapeMode()) {
            resizePlayingQueue()
        }

        val coverFragment: PlayerAlbumCoverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        coverFragment.setCallbacks(this)

        getQueuePanel().addBottomSheetCallback(bottomSheetCallbackList)

        shapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel.builder(
                /* context = */ requireContext(),
                /* shapeAppearanceResId = */ R.style.ClassicThemeOverLay,
                /* shapeAppearanceOverlayResId = */ 0
            ).build()
        )
        shapeDrawable.fillColor =
            ColorStateList.valueOf(surfaceColor())
        binding.playerQueueSheet.background = shapeDrawable

        binding.playerQueueSheet.setOnTouchListener { _, _ ->
            mainActivity.getBottomSheetBehavior().isDraggable = false
            getQueuePanel().isDraggable = true
            return@setOnTouchListener false
        }

        code.roy.appthemehelper.util.ToolbarContentTintHelper.colorizeToolbar(
            /* toolbarView = */ binding.playerToolbar,
            /* toolbarIconsColor = */ Color.WHITE,
            /* activity = */ requireActivity()
        )
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (getQueuePanel().state == BottomSheetBehavior.STATE_EXPANDED) {
                    getQueuePanel().state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    mainActivity.getBottomSheetBehavior().state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        })
    }


    private fun resizePlayingQueue() {
        val layoutParams =
            binding.playerQueueSheet.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.width = (resources.displayMetrics.widthPixels * 0.5).toInt()
        layoutParams.height = resources.displayMetrics.heightPixels
        binding.playerQueueSheet.layoutParams = layoutParams
    }

    private fun hideVolumeIfAvailable() {
        if (PreferenceUtil.isVolumeVisibilityMode) {
            childFragmentManager.commit {
                replace(
                    /* containerViewId = */ R.id.volumeFragmentContainer,
                    /* fragment = */ VolumeFragment.newInstance()
                )
            }
            childFragmentManager.executePendingTransactions()
            volumeFragment =
                whichFragment(R.id.volumeFragmentContainer) as VolumeFragment?
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        getQueuePanel().removeBottomSheetCallback(bottomSheetCallbackList)
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager?.release()
            recyclerViewDragDropManager = null
        }

        if (recyclerViewSwipeManager != null) {
            recyclerViewSwipeManager?.release()
            recyclerViewSwipeManager = null
        }

        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        _binding = null
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = song.artistName

        if (PreferenceUtil.isSongInfo) {
            binding.playerControlsContainer.songInfo.text = getSongInfo(song)
            binding.playerControlsContainer.songInfo.show()
        } else {
            binding.playerControlsContainer.songInfo.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateSong()
        updatePlayPauseDrawableState()
        updateQueue()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onRepeatModeChanged() {
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        updateShuffleState()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
        updateQueuePosition()
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        updateQueue()
    }

    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    override fun onShow() {
    }

    override fun onHide() {
    }

    override fun toolbarIconColor(): Int {
        return Color.WHITE
    }

    override val paletteColor: Int
        get() = lastColor

    override fun onColorChanged(color: MediaNotificationProcessor) {
        lastColor = color.backgroundColor
        libraryViewModel.updateColor(color.backgroundColor)

        lastPlaybackControlsColor = color.primaryTextColor
        lastDisabledPlaybackControlsColor = ColorUtil.withAlpha(
            baseColor = color.primaryTextColor,
            alpha = 0.3f
        )

        binding.playerContainer.setBackgroundColor(color.backgroundColor)
        binding.playerControlsContainer.songInfo.setTextColor(color.primaryTextColor)
        binding.playerQueueSubHeader.setTextColor(color.primaryTextColor)

        binding.playerControlsContainer.songCurrentProgress.setTextColor(lastPlaybackControlsColor)
        binding.playerControlsContainer.songTotalTime.setTextColor(lastPlaybackControlsColor)

        if (isLandscapeMode()) {
            mainActivity.setNavigationBarColor(color.backgroundColor)
        }

        ViewUtil.setProgressDrawable(
            progressSlider = binding.playerControlsContainer.progressSlider,
            newColor = color.primaryTextColor,
            thumbTint = true
        )
        volumeFragment?.setTintableColor(color.primaryTextColor)

        code.roy.appthemehelper.util.TintHelper.setTintAuto(
            /* view = */ binding.playerControlsContainer.playPauseButton,
            /* color = */ color.primaryTextColor,
            /* background = */ true
        )
        code.roy.appthemehelper.util.TintHelper.setTintAuto(
            /* view = */ binding.playerControlsContainer.playPauseButton,
            /* color = */ color.backgroundColor,
            /* background = */ false
        )

        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()

        code.roy.appthemehelper.util.ToolbarContentTintHelper.colorizeToolbar(
            /* toolbarView = */ binding.playerToolbar,
            /* toolbarIconsColor = */ Color.WHITE,
            /* activity = */ requireActivity()
        )
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.playerControlsContainer.progressSlider.max = total

        val animator = ObjectAnimator.ofInt(
            /* target = */ binding.playerControlsContainer.progressSlider,
            /* propertyName = */ "progress",
            /* ...values = */ progress
        )
        animator.duration = AbsPlayerControlsFragment.SLIDER_ANIMATION_TIME
        animator.interpolator = LinearInterpolator()
        animator.start()

        binding.playerControlsContainer.songTotalTime.text =
            MusicUtil.getReadableDurationString(total.toLong())
        binding.playerControlsContainer.songCurrentProgress.text =
            MusicUtil.getReadableDurationString(progress.toLong())
    }

    private fun updateQueuePosition() {
        playingQueueAdapter?.setCurrent(MusicPlayerRemote.position)
        resetToCurrentPosition()
    }

    private fun updateQueue() {
        playingQueueAdapter?.swapDataSet(
            dataSet = MusicPlayerRemote.playingQueue,
            position = MusicPlayerRemote.position
        )
        resetToCurrentPosition()
    }

    private fun resetToCurrentPosition() {
        binding.recyclerView.stopScroll()
        linearLayoutManager.scrollToPositionWithOffset(
            /* position = */ MusicPlayerRemote.position + 1,
            /* offset = */ 0
        )
    }

    private fun getQueuePanel(): BottomSheetBehavior<MaterialCardView> {
        return from(binding.playerQueueSheet)
    }

    private fun setupPanel() {
        if (!binding.playerContainer.isLaidOut || binding.playerContainer.isLayoutRequested) {
            binding.playerContainer.addOnLayoutChangeListener(this)
            return
        }

        // Check if the device is in landscape mode
        if (isLandscapeMode()) {
            calculateLandScapePeekHeight()
        } else {
            val height = binding.playerContainer.height
            val width = binding.playerContainer.width
            val finalHeight = height - width
            val panel = getQueuePanel()
            panel.peekHeight = finalHeight
        }
    }


    /**
     * What am doing here is getting the controls  height, and adding the toolbar and statusbar height to itm
     * then i subtract it from the screen height to get a peek height
     */
    private fun calculateLandScapePeekHeight() {
        val height = binding.playerControlsContainer.root.height
        val appbarHeight = binding.playerToolbar.height
        val statusBarHeight = binding.statusBar.height
        val screenHeight = resources.displayMetrics.heightPixels
        val peekHeight = screenHeight - (height + appbarHeight + statusBarHeight)
        val panel = getQueuePanel()
        if (peekHeight > 10) {
            panel.peekHeight = peekHeight
        } else {
            panel.peekHeight = 10
        }
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.inflateMenu(R.menu.menu_player)
        binding.playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.playerToolbar.setOnMenuItemClickListener(this)

        code.roy.appthemehelper.util.ToolbarContentTintHelper.colorizeToolbar(
            /* toolbarView = */ binding.playerToolbar,
            /* toolbarIconsColor = */ Color.WHITE,
            /* activity = */ requireActivity()
        )
    }

    private fun setupRecyclerView() {
        playingQueueAdapter = PlayingQueueAdapter(
            activity = requireActivity() as AppCompatActivity,
            dataSet = MusicPlayerRemote.playingQueue.toMutableList(),
            current = MusicPlayerRemote.position,
            itemLayoutRes = R.layout.item_queue
        )
        linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerViewTouchActionGuardManager = RecyclerViewTouchActionGuardManager()
        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        recyclerViewSwipeManager = RecyclerViewSwipeManager()

        val animator = DraggableItemAnimator()
        animator.supportsChangeAnimations = false
        wrappedAdapter =
            recyclerViewDragDropManager?.createWrappedAdapter(playingQueueAdapter!!) as RecyclerView.Adapter<*>
        wrappedAdapter =
            recyclerViewSwipeManager?.createWrappedAdapter(wrappedAdapter) as RecyclerView.Adapter<*>
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = animator
        recyclerViewTouchActionGuardManager?.attachRecyclerView(binding.recyclerView)
        recyclerViewDragDropManager?.attachRecyclerView(binding.recyclerView)
        recyclerViewSwipeManager?.attachRecyclerView(binding.recyclerView)

        linearLayoutManager.scrollToPositionWithOffset(
            /* position = */ MusicPlayerRemote.position + 1,
            /* offset = */ 0
        )
    }

    private fun setUpProgressSlider() {
        binding.playerControlsContainer.progressSlider.setOnSeekBarChangeListener(object :
            SimpleOnSeekbarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress)
                    onUpdateProgressViews(
                        progress = MusicPlayerRemote.songProgressMillis,
                        total = MusicPlayerRemote.songDurationMillis
                    )
                }
            }
        })
    }

    private fun setUpPlayPauseFab() {
        binding.playerControlsContainer.playPauseButton.setOnClickListener(
            PlayPauseButtonOnClickHandler()
        )
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playerControlsContainer.playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playerControlsContainer.playPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    private fun setUpMusicControllers() {
        setUpPlayPauseFab()
        setUpPrevNext()
        setUpRepeatButton()
        setUpShuffleButton()
        setUpProgressSlider()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPrevNext() {
        updatePrevNextColor()
        binding.playerControlsContainer.nextButton.setOnTouchListener(
            MusicSeekSkipTouchListener(
                activity = requireActivity(),
                next = true
            )
        )
        binding.playerControlsContainer.previousButton.setOnTouchListener(
            MusicSeekSkipTouchListener(
                activity = requireActivity(),
                next = false
            )
        )
    }

    private fun updatePrevNextColor() {
        binding.playerControlsContainer.nextButton.setColorFilter(
            lastPlaybackControlsColor,
            PorterDuff.Mode.SRC_IN
        )
        binding.playerControlsContainer.previousButton.setColorFilter(
            lastPlaybackControlsColor,
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun setUpShuffleButton() {
        binding.playerControlsContainer.shuffleButton.setOnClickListener {
            MusicPlayerRemote.toggleShuffleMode()
        }
    }

    fun updateShuffleState() {
        when (MusicPlayerRemote.shuffleMode) {
            MusicService.SHUFFLE_MODE_SHUFFLE ->
                binding.playerControlsContainer.shuffleButton.setColorFilter(
                    /* color = */ lastPlaybackControlsColor,
                    /* mode = */ PorterDuff.Mode.SRC_IN
                )

            else -> binding.playerControlsContainer.shuffleButton.setColorFilter(
                lastDisabledPlaybackControlsColor,
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun setUpRepeatButton() {
        binding.playerControlsContainer.repeatButton.setOnClickListener {
            MusicPlayerRemote.cycleRepeatMode()
        }
    }

    fun updateRepeatState() {
        when (MusicPlayerRemote.repeatMode) {
            MusicService.REPEAT_MODE_NONE -> {
                binding.playerControlsContainer.repeatButton.setImageResource(R.drawable.ic_repeat)
                binding.playerControlsContainer.repeatButton.setColorFilter(
                    /* color = */ lastDisabledPlaybackControlsColor,
                    /* mode = */ PorterDuff.Mode.SRC_IN
                )
            }

            MusicService.REPEAT_MODE_ALL -> {
                binding.playerControlsContainer.repeatButton.setImageResource(R.drawable.ic_repeat)
                binding.playerControlsContainer.repeatButton.setColorFilter(
                    /* color = */ lastPlaybackControlsColor,
                    /* mode = */ PorterDuff.Mode.SRC_IN
                )
            }

            MusicService.REPEAT_MODE_THIS -> {
                binding.playerControlsContainer.repeatButton.setImageResource(R.drawable.ic_repeat_one)
                binding.playerControlsContainer.repeatButton.setColorFilter(
                    /* color = */ lastPlaybackControlsColor,
                    /* mode = */ PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int,
    ) {

        // Check if the device is in landscape mode
        if (isLandscapeMode()) {
            calculateLandScapePeekHeight()

            //get background color from viewModel
            val backgroundColor = libraryViewModel.paletteColor.value

            //check if color is already applied, if not applied then update navigationBarColor
            backgroundColor?.let { color ->
                if (isLandscapeMode()) {
                    val window = requireActivity().window
                    window?.navigationBarColor.let { navBarColor ->
                        if (navBarColor == null || navBarColor != color) {
                            mainActivity.setNavigationBarColor(color)
                        }
                    }
                }
            }
        } else {

            val height = binding.playerContainer.height
            val width = binding.playerContainer.width
            val finalHeight = height - (binding.playerControlsContainer.root.height + width)
            val panel = getQueuePanel()
            panel.peekHeight = finalHeight
        }
    }

    private fun isLandscapeMode(): Boolean {
        val config = resources.configuration

        // Check if the device is in landscape mode
        return config.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}

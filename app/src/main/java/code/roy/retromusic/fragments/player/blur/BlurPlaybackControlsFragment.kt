/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.roy.retromusic.fragments.player.blur

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import code.roy.retromusic.util.color.MediaNotificationProcessor
import code.roy.appthemehelper.util.ColorUtil
import code.roy.appthemehelper.util.MaterialValueHelper
import code.roy.retromusic.R
import code.roy.retromusic.databinding.FBlurPlayerPlaybackControlsBinding
import code.roy.retromusic.extensions.applyColor
import code.roy.retromusic.extensions.getSongInfo
import code.roy.retromusic.extensions.hide
import code.roy.retromusic.extensions.show
import code.roy.retromusic.fragments.base.AbsPlayerControlsFragment
import code.roy.retromusic.fragments.base.goToAlbum
import code.roy.retromusic.fragments.base.goToArtist
import code.roy.retromusic.helper.MusicPlayerRemote
import code.roy.retromusic.util.PreferenceUtil
import com.google.android.material.slider.Slider

class BlurPlaybackControlsFragment :
    AbsPlayerControlsFragment(R.layout.f_blur_player_playback_controls) {
    private var _binding: FBlurPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val progressSlider: Slider
        get() = binding.progressSlider

    override val shuffleButton: ImageButton
        get() = binding.shuffleButton

    override val repeatButton: ImageButton
        get() = binding.repeatButton

    override val nextButton: ImageButton
        get() = binding.nextButton

    override val previousButton: ImageButton
        get() = binding.previousButton

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FBlurPlayerPlaybackControlsBinding.bind(view)
        setUpPlayPauseFab()
        binding.title.isSelected = true
        binding.text.isSelected = true
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = String.format("%s • %s", song.artistName, song.albumName)

        if (PreferenceUtil.isSongInfo) {
            binding.songInfo.show()
            binding.songInfo.text = getSongInfo(song)
        } else {
            binding.songInfo.hide()
        }
    }

    override fun onServiceConnected() {
        updatePlayPauseDrawableState()
        updateRepeatState()
        updateShuffleState()
        updateSong()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
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

    override fun setColor(color: MediaNotificationProcessor) {
        lastPlaybackControlsColor = Color.WHITE
        lastDisabledPlaybackControlsColor =
            ContextCompat.getColor(requireContext(), code.roy.appthemehelper.R.color.md_grey_500)

        binding.title.setTextColor(lastPlaybackControlsColor)

        binding.songCurrentProgress.setTextColor(lastPlaybackControlsColor)
        binding.songTotalTime.setTextColor(lastPlaybackControlsColor)

        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()

        binding.text.setTextColor(lastPlaybackControlsColor)
        binding.songInfo.setTextColor(lastDisabledPlaybackControlsColor)

        binding.progressSlider.applyColor(lastPlaybackControlsColor)
        volumeFragment?.setTintableColor(lastPlaybackControlsColor)
        setFabColor(lastPlaybackControlsColor)
    }

    private fun setFabColor(i: Int) {
        code.roy.appthemehelper.util.TintHelper.setTintAuto(
            binding.playPauseButton,
            MaterialValueHelper.getPrimaryTextColor(context, ColorUtil.isColorLight(i)),
            false
        )
        code.roy.appthemehelper.util.TintHelper.setTintAuto(binding.playPauseButton, i, true)
    }

    private fun setUpPlayPauseFab() {
        binding.playPauseButton.setOnClickListener {
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
            it.showBounceAnimation()
        }
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_32dp)
        }
    }

    public override fun show() {
        binding.playPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    public override fun hide() {
        binding.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

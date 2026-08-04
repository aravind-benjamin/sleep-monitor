package com.example.sleepmonitor.ui.playback

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sleepmonitor.data.SleepRepository
import com.example.sleepmonitor.data.SoundEvent
import com.example.sleepmonitor.databinding.FragmentPlaybackBinding
import kotlinx.coroutines.launch

class PlaybackFragment : Fragment() {

    private var _binding: FragmentPlaybackBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: SleepRepository
    private lateinit var adapter: EventAdapter
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaybackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SleepRepository(requireContext())

        adapter = EventAdapter(
            onPlay = { event -> togglePlay(event) },
            onDelete = { event ->
                stopPlayback()
                lifecycleScope.launch { repository.deleteEvent(event) }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        repository.observeAllEvents().observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            binding.emptyView.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun togglePlay(event: SoundEvent) {
        if (adapter.currentlyPlayingId == event.id) {
            stopPlayback()
            return
        }
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(event.filePath)
                prepare()
                setOnCompletionListener { stopPlayback() }
                start()
            }
            adapter.currentlyPlayingId = event.id
        } catch (e: Exception) {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        adapter.currentlyPlayingId = null
    }

    override fun onDestroyView() {
        stopPlayback()
        super.onDestroyView()
        _binding = null
    }
}

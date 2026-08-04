package com.example.sleepmonitor.ui.playback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sleepmonitor.R
import com.example.sleepmonitor.data.SoundEvent
import com.example.sleepmonitor.data.SoundType
import com.example.sleepmonitor.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val onPlay: (SoundEvent) -> Unit,
    private val onDelete: (SoundEvent) -> Unit
) : ListAdapter<SoundEvent, EventAdapter.VH>(DIFF) {

    private val timeFmt = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
    var currentlyPlayingId: Long? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val event = getItem(position)
        val label = when (event.type) {
            SoundType.SNORE -> "Snoring"
            SoundType.TALK -> "Sleep talk"
            SoundType.NOISE -> "Other sound"
        }
        val color = when (event.type) {
            SoundType.SNORE -> R.color.snore_color
            SoundType.TALK -> R.color.talk_color
            SoundType.NOISE -> R.color.noise_color
        }
        holder.binding.tvType.text = label
        holder.binding.typeIndicator.setBackgroundColor(holder.itemView.context.getColor(color))
        val confidencePct = (event.confidence * 100).toInt()
        holder.binding.tvMeta.text =
            "${timeFmt.format(Date(event.timestamp))} \u00b7 ${event.durationMs / 1000}s \u00b7 $confidencePct% confidence"

        val isPlaying = currentlyPlayingId == event.id
        holder.binding.btnPlay.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        holder.binding.btnPlay.setOnClickListener { onPlay(event) }
        holder.binding.btnDelete.setOnClickListener { onDelete(event) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SoundEvent>() {
            override fun areItemsTheSame(old: SoundEvent, new: SoundEvent) = old.id == new.id
            override fun areContentsTheSame(old: SoundEvent, new: SoundEvent) = old == new
        }
    }
}

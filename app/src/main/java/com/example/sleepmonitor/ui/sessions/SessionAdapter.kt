package com.example.sleepmonitor.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sleepmonitor.data.SleepSession
import com.example.sleepmonitor.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class SessionAdapter(
    private val onEdit: (SleepSession) -> Unit,
    private val onDelete: (SleepSession) -> Unit,
    private val onClick: (SleepSession) -> Unit
) : ListAdapter<SleepSession, SessionAdapter.VH>(DIFF) {

    private val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class VH(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val session = getItem(position)
        holder.binding.tvDate.text = dateFmt.format(Date(session.startTime))
        val endText = session.endTime?.let { timeFmt.format(Date(it)) } ?: "in progress"
        holder.binding.tvTimeRange.text = "${timeFmt.format(Date(session.startTime))} \u2192 $endText"
        val duration = if (session.endTime != null) "${session.durationMinutes / 60}h ${session.durationMinutes % 60}m tracked" else "Tracking now"
        holder.binding.tvEventSummary.text = duration

        holder.binding.btnEdit.setOnClickListener { onEdit(session) }
        holder.binding.btnDelete.setOnClickListener { onDelete(session) }
        holder.itemView.setOnClickListener { onClick(session) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SleepSession>() {
            override fun areItemsTheSame(old: SleepSession, new: SleepSession) = old.id == new.id
            override fun areContentsTheSame(old: SleepSession, new: SleepSession) = old == new
        }
    }
}

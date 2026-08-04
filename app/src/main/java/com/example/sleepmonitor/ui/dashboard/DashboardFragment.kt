package com.example.sleepmonitor.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.sleepmonitor.data.SleepRepository
import com.example.sleepmonitor.data.SoundType
import com.example.sleepmonitor.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: SleepRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SleepRepository(requireContext())

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.tvSnoreCount.text = stats.snoreEvents.toString()
            binding.tvTalkCount.text = stats.talkEvents.toString()
            val h = stats.totalTrackedMinutes / 60
            val m = stats.totalTrackedMinutes % 60
            binding.tvTotalMinutes.text = "${h}h ${m}m"
        }

        repository.observeSessions().observe(viewLifecycleOwner) { sessions ->
            renderChart(sessions.take(7).reversed())
        }
    }

    private fun renderChart(sessions: List<com.example.sleepmonitor.data.SleepSession>) {
        if (sessions.isEmpty()) return

        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val labels = sessions.map { dateFormat.format(Date(it.startTime)) }

        // Pull events per session and build bar entries (snore counts).
        val entries = mutableListOf<BarEntry>()
        sessions.forEachIndexed { index, session ->
            repository.observeEventsForSession(session.id).observe(viewLifecycleOwner) { events ->
                val snoreCount = events.count { it.type == SoundType.SNORE }.toFloat()
                entries.removeAll { it.x == index.toFloat() }
                entries.add(BarEntry(index.toFloat(), snoreCount))
                entries.sortBy { it.x }

                val dataSet = BarDataSet(entries, "Snore events")
                dataSet.color = resources.getColor(com.example.sleepmonitor.R.color.snore_color, null)
                binding.barChart.data = BarData(dataSet)
                binding.barChart.xAxis.valueFormatter =
                    com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                binding.barChart.description.isEnabled = false
                binding.barChart.invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

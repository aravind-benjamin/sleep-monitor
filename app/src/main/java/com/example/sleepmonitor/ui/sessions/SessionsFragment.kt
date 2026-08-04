package com.example.sleepmonitor.ui.sessions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sleepmonitor.data.SleepRepository
import com.example.sleepmonitor.data.SleepSession
import com.example.sleepmonitor.databinding.DialogManualSessionBinding
import com.example.sleepmonitor.databinding.FragmentSessionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: SleepRepository
    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SleepRepository(requireContext())

        adapter = SessionAdapter(
            onEdit = { showManualEntryDialog(it) },
            onDelete = { session ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete this sleep entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch { repository.deleteSession(session) }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onClick = { /* could navigate to a filtered playback view */ }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        repository.observeSessions().observe(viewLifecycleOwner) { sessions ->
            adapter.submitList(sessions)
            binding.emptyView.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddManual.setOnClickListener { showManualEntryDialog(null) }
    }

    private fun showManualEntryDialog(existing: SleepSession?) {
        val dialogBinding = DialogManualSessionBinding.inflate(layoutInflater)
        val cal = Calendar.getInstance()

        var startMillis = existing?.startTime ?: (System.currentTimeMillis() - 8 * 3600_000)
        var endMillis = existing?.endTime ?: System.currentTimeMillis()

        val fmt = SimpleDateFormat("EEE MMM d, h:mm a", Locale.getDefault())
        fun refreshLabels() {
            dialogBinding.btnPickStart.text = fmt.format(Date(startMillis))
            dialogBinding.btnPickEnd.text = fmt.format(Date(endMillis))
        }
        refreshLabels()

        dialogBinding.btnPickStart.setOnClickListener {
            pickDateTime(startMillis) { picked -> startMillis = picked; refreshLabels() }
        }
        dialogBinding.btnPickEnd.setOnClickListener {
            pickDateTime(endMillis) { picked -> endMillis = picked; refreshLabels() }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add sleep entry" else "Edit sleep entry")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                lifecycleScope.launch {
                    if (existing == null) {
                        repository.addManualSession(
                            SleepSession(
                                startTime = startMillis,
                                endTime = endMillis,
                                isManualEntry = true
                            )
                        )
                    } else {
                        repository.updateSession(
                            existing.copy(startTime = startMillis, endTime = endMillis)
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickDateTime(initialMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day)
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

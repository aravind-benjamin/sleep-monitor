package com.example.sleepmonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sleepmonitor.databinding.ActivityMainBinding
import com.example.sleepmonitor.service.SleepTrackingService
import com.example.sleepmonitor.ui.dashboard.DashboardFragment
import com.example.sleepmonitor.ui.playback.PlaybackFragment
import com.example.sleepmonitor.ui.sessions.SessionsFragment
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            startTrackingService()
        } else {
            Snackbar.make(binding.root, R.string.mic_permission_required, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(DashboardFragment())
                R.id.nav_sessions -> showFragment(SessionsFragment())
                R.id.nav_playback -> showFragment(PlaybackFragment())
            }
            true
        }

        binding.fabTrack.setOnClickListener {
            if (hasRecordPermission()) {
                startTrackingService()
            } else {
                requestPermissions()
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun startTrackingService() {
        val intent = Intent(this, SleepTrackingService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Snackbar.make(binding.root, R.string.tracking_started, Snackbar.LENGTH_SHORT).show()
    }
}

package com.pixelbot.messenger.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pixelbot.messenger.PixelBotApplication
import com.pixelbot.messenger.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PixelBotApplication.instance.prefs

        binding.etCooldownSec.setText(prefs.cooldownSec.toString())
        binding.etSendDelayMs.setText(prefs.sendDelayMs.toString())
        binding.etTargetPackage.setText(prefs.targetPackage)

        binding.btnIgnoreBatteryOpt.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }

        binding.btnSaveSettings.setOnClickListener {
            val cooldown = binding.etCooldownSec.text.toString().toIntOrNull() ?: 3
            val delay = binding.etSendDelayMs.text.toString().toLongOrNull() ?: 300L
            val pkg = binding.etTargetPackage.text.toString().trim()

            prefs.cooldownSec = cooldown
            prefs.sendDelayMs = delay
            if (pkg.isNotEmpty()) {
                prefs.targetPackage = pkg
            }

            Toast.makeText(requireContext(), "설정이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val pkg = requireContext().packageName
            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$pkg")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(requireContext(), "이미 배터리 최적화에서 제외되어 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

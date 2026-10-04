package com.pixelbot.messenger

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.pixelbot.messenger.databinding.ActivityMainBinding
import com.pixelbot.messenger.service.KakaoAccessibilityService
import com.pixelbot.messenger.service.PixelBotForegroundService
import com.pixelbot.messenger.ui.LogsFragment
import com.pixelbot.messenger.ui.RulesFragment
import com.pixelbot.messenger.ui.ScriptFragment
import com.pixelbot.messenger.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPagerAndTabs()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatusUI()
    }

    private fun setupViewPagerAndTabs() {
        val fragments: List<Fragment> = listOf(
            RulesFragment(),
            ScriptFragment(),
            LogsFragment(),
            SettingsFragment()
        )
        val tabTitles = listOf("노코드 매크로", "자바스크립트", "실시간 로그", "설정/배터리")

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun setupListeners() {
        binding.btnToggleService.setOnClickListener {
            openAccessibilitySettings()
        }

        binding.tvBannerWarning.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun updateServiceStatusUI() {
        val isEnabled = isAccessibilityServiceEnabled()
        if (isEnabled) {
            binding.tvServiceStatus.text = "🟢 화면 감시 서비스 가동 중"
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_success))
            binding.btnToggleService.text = "서비스 설정"
            binding.tvBannerWarning.visibility = View.GONE

            // Ensure background service is running
            try {
                val serviceIntent = Intent(this, PixelBotForegroundService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            binding.tvServiceStatus.text = "🔴 접근성 권한 필요 (꺼짐)"
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_danger))
            binding.btnToggleService.text = "권한 켜기"
            binding.tvBannerWarning.visibility = View.VISIBLE
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (KakaoAccessibilityService.isServiceRunning) return true

        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName &&
                service.resolveInfo.serviceInfo.name.contains("KakaoAccessibilityService")
            ) {
                return true
            }
        }
        return false
    }
}

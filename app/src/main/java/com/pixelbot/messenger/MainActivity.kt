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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !isAccessibilityServiceEnabled()) {
            androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_PixelMessengerBot)
                .setTitle("⚠️ 안드로이드 13/14+ 권한 안내")
                .setMessage("스마트폰 보안 정책상 외부 설치 앱은 접근성이 '제한된 설정'으로 막힐 수 있습니다.\n\n" +
                            "만약 접근성 스위치가 회색으로 눌리지 않는다면:\n" +
                            "1. 스마트폰 [설정] ➡️ [애플리케이션] ➡️ [PixelBot]\n" +
                            "2. 우측 상단 [⋮ 점 3개] ➡️ [제한된 설정 허용] 터치\n" +
                            "3. 그 후 접근성을 켜주시면 됩니다!")
                .setPositiveButton("접근성 설정 열기") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNeutralButton("앱 정보 열기") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
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

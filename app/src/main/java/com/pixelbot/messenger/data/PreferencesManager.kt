package com.pixelbot.messenger.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pixelbot.messenger.model.MacroRule
import com.pixelbot.messenger.model.MatchType

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pixel_bot_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_MACRO_RULES = "macro_rules"
        private const val KEY_JS_SCRIPT = "js_script"
        private const val KEY_IS_BOT_ENABLED = "is_bot_enabled"
        private const val KEY_COOLDOWN_SEC = "cooldown_sec"
        private const val KEY_SEND_DELAY_MS = "send_delay_ms"
        private const val KEY_TARGET_PACKAGE = "target_package"

        val DEFAULT_JS_SCRIPT = """
/**
 * 메신저봇R 호환 JavaScript 스크립트
 * 화면에서 감지된 텍스트가 전달됩니다.
 *
 * @param room 채팅방 이름 (또는 대화 상대)
 * @param msg 화면에 감지된 메시지 본문
 * @param sender 보낸 사람
 * @param isGroupChat 단체 채팅방 여부
 * @param replier replier.reply("응답내용") 으로 카카오톡 자동 전송
 */
function response(room, msg, sender, isGroupChat, replier, imageDB, packageName) {
    // 예시 1: !고승현 명령어
    if (msg === "!고승현") {
        replier.reply("나는 고승현이다 (JS 모드)");
        return;
    }

    // 예시 2: !도움말 명령어
    if (msg === "!도움말") {
        var help = "✨ [PixelBot 명령어 목록]\n";
        help += "1. !고승현 : 나는 고승현이다 출력\n";
        help += "2. !주사위 : 1~6 주사위 굴리기\n";
        help += "3. !시간 : 현재 시각 안내\n";
        help += "4. !핑 : 퐁 응답";
        replier.reply(help);
        return;
    }

    // 예시 3: !핑 명령어
    if (msg === "!핑") {
        replier.reply("🏓 퐁! (화면 감시 정상 가동 중)");
        return;
    }
}
""".trimIndent()
    }

    fun initDefaultPresetsIfEmpty() {
        if (!prefs.contains(KEY_MACRO_RULES)) {
            val defaultRules = listOf(
                MacroRule(
                    name = "고승현 n일차 트리거",
                    trigger = "!고승현",
                    matchType = MatchType.EXACT,
                    response = "고승현 되기 {count}일차",
                    isEnabled = true
                ),
                MacroRule(
                    name = "도움말",
                    trigger = "!도움말",
                    matchType = MatchType.EXACT,
                    response = "🤖 화면 감시 봇 작동 중!\n- !고승현 : 나는 고승현이다\n- !주사위 : 주사위 굴리기\n- !시간 : 현재 시각 확인",
                    isEnabled = true
                ),
                MacroRule(
                    name = "주사위",
                    trigger = "!주사위",
                    matchType = MatchType.EXACT,
                    response = "🎲 주사위를 굴렸습니다: [{random:1..6}]",
                    isEnabled = true
                ),
                MacroRule(
                    name = "현재 시간",
                    trigger = "!시간",
                    matchType = MatchType.EXACT,
                    response = "⏰ 현재 시각: {time:yyyy-MM-dd HH:mm:ss}",
                    isEnabled = true
                )
            )
            saveMacroRules(defaultRules)
        }

        if (!prefs.contains(KEY_JS_SCRIPT)) {
            saveJsScript(DEFAULT_JS_SCRIPT)
        }
    }

    fun getMacroRules(): MutableList<MacroRule> {
        val json = prefs.getString(KEY_MACRO_RULES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<MacroRule>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveMacroRules(rules: List<MacroRule>) {
        val json = gson.toJson(rules)
        prefs.edit().putString(KEY_MACRO_RULES, json).apply()
    }

    fun getJsScript(): String {
        return prefs.getString(KEY_JS_SCRIPT, DEFAULT_JS_SCRIPT) ?: DEFAULT_JS_SCRIPT
    }

    fun saveJsScript(script: String) {
        prefs.edit().putString(KEY_JS_SCRIPT, script).apply()
    }

    var isBotEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_BOT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_BOT_ENABLED, value).apply()

    var cooldownSec: Int
        get() = prefs.getInt(KEY_COOLDOWN_SEC, 3)
        set(value) = prefs.edit().putInt(KEY_COOLDOWN_SEC, value).apply()

    var sendDelayMs: Long
        get() = prefs.getLong(KEY_SEND_DELAY_MS, 300L)
        set(value) = prefs.edit().putLong(KEY_SEND_DELAY_MS, value).apply()

    var targetPackage: String
        get() = prefs.getString(KEY_TARGET_PACKAGE, "com.kakao.talk") ?: "com.kakao.talk"
        set(value) = prefs.edit().putString(KEY_TARGET_PACKAGE, value).apply()

    @Synchronized
    fun getNextCounter(counterName: String, initial: Int = 1): Int {
        val key = "counter_$counterName"
        val current = prefs.getInt(key, initial - 1)
        val next = current + 1
        prefs.edit().putInt(key, next).apply()
        return next
    }

    fun resetCounter(counterName: String, value: Int = 0) {
        val key = "counter_$counterName"
        prefs.edit().putInt(key, value).apply()
    }
}

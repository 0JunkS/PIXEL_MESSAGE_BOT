package com.pixelbot.messenger.engine

import com.pixelbot.messenger.PixelBotApplication
import com.pixelbot.messenger.model.BotLog
import java.util.concurrent.CopyOnWriteArrayList

object BotController {

    private val jsEngine = JsEngine()
    private val recentMessages = LinkedHashMap<String, Long>()
    private val maxRecentCache = 50

    val logList = CopyOnWriteArrayList<BotLog>()
    var logUpdateListener: (() -> Unit)? = null

    fun addLog(tag: String, message: String) {
        val log = BotLog(tag = tag, message = message)
        logList.add(0, log)
        if (logList.size > 200) {
            logList.removeAt(logList.size - 1)
        }
        logUpdateListener?.invoke()
    }

    /**
     * 화면에서 텍스트 노드가 감지되었을 때 호출됨
     * @return 전송할 최종 텍스트가 있으면 문자열 반환, 없으면 null
     */
    @Synchronized
    fun processDetectedScreenText(
        room: String,
        msg: String,
        sender: String = "상대방"
    ): String? {
        val prefs = PixelBotApplication.instance.prefs
        if (!prefs.isBotEnabled) {
            return null
        }

        val cleanMsg = msg.trim()
        if (cleanMsg.isEmpty()) return null

        val now = System.currentTimeMillis()
        val cooldownMs = prefs.cooldownSec * 1000L

        // 중복 방지 검사 (방이름 + 메시지 본문 기준)
        val cacheKey = "$room::$cleanMsg"
        val lastSeen = recentMessages[cacheKey]
        if (lastSeen != null && (now - lastSeen) < cooldownMs) {
            // 쿨타임 내 동일 메시지는 무시
            return null
        }
        recentMessages[cacheKey] = now
        if (recentMessages.size > maxRecentCache) {
            val firstKey = recentMessages.keys.firstOrNull()
            if (firstKey != null) recentMessages.remove(firstKey)
        }

        addLog("화면감지", "[$room] $cleanMsg")

        // 1. 노코드 매크로 규칙 우선 검사
        val rules = prefs.getMacroRules()
        val macroReply = MacroEngine.evaluate(rules, room, cleanMsg, sender)
        if (macroReply != null) {
            addLog("매크로실행", "응답 -> $macroReply")
            return macroReply
        }

        // 2. JavaScript 엔진 검사
        val script = prefs.getJsScript()
        if (script.isNotBlank()) {
            var jsReply: String? = null
            jsEngine.execute(
                script = script,
                room = room,
                msg = cleanMsg,
                sender = sender,
                isGroupChat = false,
                packageName = prefs.targetPackage
            ) { replyText ->
                jsReply = replyText
            }

            if (!jsReply.isNullOrBlank()) {
                addLog("JS실행", "응답 -> $jsReply")
                return jsReply
            }
        }

        return null
    }
}

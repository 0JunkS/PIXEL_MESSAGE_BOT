package com.pixelbot.messenger.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.pixelbot.messenger.PixelBotApplication
import com.pixelbot.messenger.engine.BotController

class KakaoAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastHandledTrigger: String = ""
    private var lastHandledTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        BotController.addLog("서비스가동", "카카오톡 화면 감시 서비스가 성공적으로 연결되었습니다.")

        try {
            val serviceIntent = Intent(this, PixelBotForegroundService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        val targetPackage = PixelBotApplication.instance.prefs.targetPackage

        // Check if event is from target package (e.g. com.kakao.talk)
        if (targetPackage.isNotBlank() && targetPackage != "ALL" && pkgName != targetPackage) {
            return
        }

        // Only scan when active window is available
        val rootNode = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return

        try {
            scanScreenForTriggers(rootNode)
        } catch (e: Exception) {
            BotController.addLog("감시오류", "화면 분석 중 예외: ${e.message}")
        } finally {
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                // Ignore recycle exceptions
            }
        }
    }

    private fun scanScreenForTriggers(rootNode: AccessibilityNodeInfo) {
        val screenTexts = mutableListOf<String>()
        extractAllTexts(rootNode, screenTexts)

        for (rawText in screenTexts) {
            val text = rawText.trim()
            if (text.isEmpty()) continue

            // Check if contains trigger prefix '!'
            val exclamationIndex = text.indexOf("!")
            if (exclamationIndex >= 0) {
                val candidateTrigger = text.substring(exclamationIndex).trim()
                val now = System.currentTimeMillis()
                val cooldown = PixelBotApplication.instance.prefs.cooldownSec * 1000L

                if (candidateTrigger == lastHandledTrigger && (now - lastHandledTime) < cooldown) {
                    continue
                }

                val reply = BotController.processDetectedScreenText("카카오톡", candidateTrigger)
                if (!reply.isNullOrBlank()) {
                    lastHandledTrigger = candidateTrigger
                    lastHandledTime = now
                    BotController.addLog("트리거감지", "화면 감지: $candidateTrigger ➡️ 응답 예약: $reply")

                    // Schedule automatic typing and sending on fresh window state
                    val delay = PixelBotApplication.instance.prefs.sendDelayMs
                    handler.postDelayed({
                        performAutoTypingAndSend(reply)
                    }, delay)
                    break
                }
            }
        }
    }

    private fun extractAllTexts(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return

        // Exclude EditText itself from screen reading to avoid self-triggering
        val isInput = node.className == "android.widget.EditText" || node.isEditable
        if (!isInput) {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { textList.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { textList.add(it) }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                null
            }
            if (child != null) {
                extractAllTexts(child, textList)
                try {
                    child.recycle()
                } catch (e: Exception) {}
            }
        }
    }

    private fun performAutoTypingAndSend(replyText: String) {
        // ALWAYS obtain a fresh rootInActiveWindow right before interacting
        val freshRoot = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        }

        if (freshRoot == null) {
            BotController.addLog("전송실패", "화면 활성 윈도우를 가져오지 못했습니다.")
            return
        }

        try {
            val editNode = findInputNode(freshRoot)
            if (editNode == null) {
                BotController.addLog("전송실패", "카카오톡 입력창(EditText)을 찾지 못했습니다.")
                return
            }

            // Step 1: Focus input field
            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Step 2: Set Text directly
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    replyText
                )
            }
            var textSet = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            // Fallback: If ACTION_SET_TEXT failed, use Clipboard + PASTE
            if (!textSet) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("PixelBotReply", replyText)
                clipboard.setPrimaryClip(clip)
                textSet = editNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }

            try {
                editNode.recycle()
            } catch (e: Exception) {}

            if (textSet) {
                BotController.addLog("입력완료", "입력창에 텍스트 입력 성공: $replyText")

                // Step 3: Wait 200ms for KakaoTalk UI to reveal the Send button, then click it
                handler.postDelayed({
                    clickSendButton()
                }, 200)
            } else {
                BotController.addLog("전송실패", "입력창에 텍스트 대입이 거부되었습니다.")
            }
        } finally {
            try {
                freshRoot.recycle()
            } catch (e: Exception) {}
        }
    }

    private fun clickSendButton() {
        val root = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return

        try {
            val sendNode = findSendButtonNode(root)
            if (sendNode != null) {
                val clicked = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    BotController.addLog("전송완료", "카카오톡 [전송] 버튼 클릭 완료! 🚀")
                } else {
                    BotController.addLog("주의", "전송 버튼 클릭 액션 실패")
                }
                try {
                    sendNode.recycle()
                } catch (e: Exception) {}
            } else {
                BotController.addLog("주의", "전송 버튼을 찾지 못했습니다. (엔터 전송 옵션 필요)")
            }
        } finally {
            try {
                root.recycle()
            } catch (e: Exception) {}
        }
    }

    private fun findInputNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val className = node.className?.toString() ?: ""
        if (className.contains("EditText", ignoreCase = true) || node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            val found = findInputNode(child)
            try { child?.recycle() } catch (e: Exception) {}
            if (found != null) return found
        }
        return null
    }

    private fun findSendButtonNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val viewId = node.viewIdResourceName?.toString() ?: ""

        val isSendKeyword = desc.contains("전송") || desc.contains("보내기") || desc.contains("Send") ||
                            text.contains("전송") || text.contains("보내기") || text.contains("Send") ||
                            viewId.contains("send", ignoreCase = true)

        if (node.isClickable && isSendKeyword) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            val found = findSendButtonNode(child)
            try { child?.recycle() } catch (e: Exception) {}
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        isServiceRunning = false
        BotController.addLog("서비스중단", "접근성 서비스가 중단되었습니다.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}

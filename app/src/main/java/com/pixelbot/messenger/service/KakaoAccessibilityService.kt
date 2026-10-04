package com.pixelbot.messenger.service

import android.accessibilityservice.AccessibilityService
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
    private var lastHandledText: String = ""
    private var lastHandledTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        BotController.addLog("서비스가동", "카카오톡 화면 감시 접근성 서비스가 활성화되었습니다.")
        
        // Start foreground service together for persistent background survival
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

        if (pkgName != targetPackage) {
            return
        }

        // Window content changed or window state changed
        val rootNode = rootInActiveWindow ?: return

        try {
            analyzeScreenAndReact(rootNode)
        } finally {
            rootNode.recycle()
        }
    }

    private fun analyzeScreenAndReact(rootNode: AccessibilityNodeInfo) {
        val screenTexts = mutableListOf<String>()
        var chatTitle = "카카오톡"

        // Recursively find all text nodes and potential input fields
        extractNodes(rootNode, screenTexts)

        // Find recent trigger messages
        for (text in screenTexts) {
            val clean = text.trim()
            if (clean.startsWith("!")) {
                // Check if this was just handled to avoid loop
                val now = System.currentTimeMillis()
                if (clean == lastHandledText && (now - lastHandledTime) < 2000L) {
                    continue
                }

                // Process through BotController
                val reply = BotController.processDetectedScreenText(chatTitle, clean)
                if (!reply.isNullOrBlank()) {
                    lastHandledText = clean
                    lastHandledTime = now
                    executeAutoReply(rootNode, reply)
                    break
                }
            }
        }
    }

    private fun extractNodes(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return

        val text = node.text?.toString()
        if (!text.isNullOrBlank() && node.className != "android.widget.EditText") {
            textList.add(text)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractNodes(child, textList)
                child.recycle()
            }
        }
    }

    private fun executeAutoReply(rootNode: AccessibilityNodeInfo, replyText: String) {
        val delayMs = PixelBotApplication.instance.prefs.sendDelayMs

        handler.postDelayed({
            // 1. 카카오톡 메시지 입력창(EditText) 찾기
            val editNode = findEditTextNode(rootNode)
            if (editNode != null) {
                // 2. 텍스트 설정 (ACTION_SET_TEXT)
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        replyText
                    )
                }
                val textSetSuccess = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                if (textSetSuccess) {
                    BotController.addLog("입력완료", "카카오톡 입력창에 텍스트 대입: $replyText")

                    // 3. 전송 버튼 클릭 (약간의 텀을 두고 클릭)
                    handler.postDelayed({
                        val sendButton = findSendButton(rootNode)
                        if (sendButton != null) {
                            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            BotController.addLog("전송완료", "카카오톡 [전송] 버튼 클릭 성공!")
                            sendButton.recycle()
                        } else {
                            BotController.addLog("주의", "전송 버튼을 찾지 못했습니다. 수동 확인 필요")
                        }
                    }, 150)
                } else {
                    BotController.addLog("오류", "입력창에 텍스트를 대입하지 못했습니다.")
                }
                editNode.recycle()
            } else {
                BotController.addLog("오류", "카카오톡 입력창(EditText)을 화면에서 찾지 못했습니다.")
            }
        }, delayMs)
    }

    private fun findEditTextNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className == "android.widget.EditText" || node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            val found = findEditTextNode(child)
            child?.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val resId = node.viewIdResourceName ?: ""

        // Check button conditions in KakaoTalk
        val isSendByDesc = desc.contains("전송") || desc.contains("보내기") || desc.contains("Send")
        val isSendByText = text == "전송" || text == "보내기"
        val isSendById = resId.contains("send") || resId.contains("btn_send")

        if (node.isClickable && (isSendByDesc || isSendByText || isSendById)) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            val found = findSendButton(child)
            child?.recycle()
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

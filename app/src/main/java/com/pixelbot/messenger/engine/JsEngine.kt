package com.pixelbot.messenger.engine

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class JsEngine {

    interface ReplyCallback {
        fun onReply(msg: String)
    }

    class Replier(private val callback: ReplyCallback) {
        fun reply(msg: String) {
            callback.onReply(msg)
        }

        fun reply(room: String, msg: String) {
            callback.onReply(msg)
        }
    }

    fun execute(
        script: String,
        room: String,
        msg: String,
        sender: String,
        isGroupChat: Boolean = false,
        packageName: String = "com.kakao.talk",
        onReply: (String) -> Unit
    ): String? {
        val rhino = Context.enter()
        // Optimization level -1 for interpreted mode on Android Dalvik/ART
        rhino.optimizationLevel = -1

        try {
            val scope: Scriptable = rhino.initStandardObjects()

            // Replier object
            val replier = Replier(object : ReplyCallback {
                override fun onReply(msg: String) {
                    onReply(msg)
                }
            })
            ScriptableObject.putProperty(scope, "replier", Context.javaToJS(replier, scope))

            // Console mock
            val consoleMock = object {
                fun log(vararg args: Any?) {
                    android.util.Log.i("PixelBotJS", args.joinToString(" "))
                }
            }
            ScriptableObject.putProperty(scope, "console", Context.javaToJS(consoleMock, scope))

            // Evaluate script definition
            rhino.evaluateString(scope, script, "PixelBotScript.js", 1, null)

            // Look for response function
            val responseObj = scope.get("response", scope)
            if (responseObj is Function) {
                val args = arrayOf<Any?>(
                    room,
                    msg,
                    sender,
                    isGroupChat,
                    Context.javaToJS(replier, scope),
                    null, // imageDB
                    packageName
                )
                responseObj.call(rhino, scope, scope, args)
                return "성공"
            } else {
                return "response 함수를 찾을 수 없습니다."
            }
        } catch (e: Exception) {
            return "오류: ${e.message}"
        } finally {
            Context.exit()
        }
    }
}

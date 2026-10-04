package com.pixelbot.messenger.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pixelbot.messenger.PixelBotApplication
import com.pixelbot.messenger.data.PreferencesManager
import com.pixelbot.messenger.databinding.FragmentScriptBinding
import com.pixelbot.messenger.engine.JsEngine

class ScriptFragment : Fragment() {

    private var _binding: FragmentScriptBinding? = null
    private val binding get() = _binding!!

    private val jsEngine = JsEngine()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScriptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PixelBotApplication.instance.prefs

        binding.etJsCode.setText(prefs.getJsScript())

        binding.btnSaveScript.setOnClickListener {
            val script = binding.etJsCode.text.toString()
            prefs.saveJsScript(script)
            Toast.makeText(requireContext(), "스크립트가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetTemplate.setOnClickListener {
            binding.etJsCode.setText(PreferencesManager.DEFAULT_JS_SCRIPT)
            Toast.makeText(requireContext(), "기본 템플릿으로 복구되었습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnRunTest.setOnClickListener {
            val script = binding.etJsCode.text.toString()
            val testMsg = binding.etTestMsg.text.toString().trim()

            if (testMsg.isEmpty()) {
                Toast.makeText(requireContext(), "테스트 메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var replied = false
            val outputBuilder = StringBuilder()

            val status = jsEngine.execute(
                script = script,
                room = "테스트 채팅방",
                msg = testMsg,
                sender = "홍길동",
                isGroupChat = true,
                packageName = prefs.targetPackage
            ) { reply ->
                replied = true
                outputBuilder.append("📤 [전송 응답]: ").append(reply).append("\n")
            }

            if (!replied) {
                outputBuilder.append("⚠️ [결과]: 조건에 맞는 응답이 트리거되지 않았습니다.\n(엔진 상태: $status)")
            }

            binding.tvTestOutput.text = outputBuilder.toString().trim()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

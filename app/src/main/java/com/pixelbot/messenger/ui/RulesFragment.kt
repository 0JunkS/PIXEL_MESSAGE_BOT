package com.pixelbot.messenger.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pixelbot.messenger.PixelBotApplication
import com.pixelbot.messenger.R
import com.pixelbot.messenger.adapter.MacroRuleAdapter
import com.pixelbot.messenger.databinding.DialogEditRuleBinding
import com.pixelbot.messenger.databinding.FragmentRulesBinding
import com.pixelbot.messenger.model.MacroRule
import com.pixelbot.messenger.model.MatchType

class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MacroRuleAdapter
    private val rulesList = mutableListOf<MacroRule>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PixelBotApplication.instance.prefs
        rulesList.clear()
        rulesList.addAll(prefs.getMacroRules())

        adapter = MacroRuleAdapter(
            rules = rulesList,
            onToggle = { _, _ ->
                prefs.saveMacroRules(rulesList)
            },
            onEdit = { rule ->
                showEditRuleDialog(rule)
            },
            onDelete = { rule ->
                rulesList.remove(rule)
                prefs.saveMacroRules(rulesList)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "규칙이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvRules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRules.adapter = adapter

        binding.btnAddRule.setOnClickListener {
            showEditRuleDialog(null)
        }
    }

    private fun showEditRuleDialog(existingRule: MacroRule?) {
        val dialogBinding = DialogEditRuleBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_PixelMessengerBot)
            .setView(dialogBinding.root)
            .create()

        if (existingRule != null) {
            dialogBinding.tvDialogTitle.text = "매크로 규칙 수정"
            dialogBinding.etRuleName.setText(existingRule.name)
            dialogBinding.etTrigger.setText(existingRule.trigger)
            dialogBinding.etResponse.setText(existingRule.response)
            when (existingRule.matchType) {
                MatchType.EXACT -> dialogBinding.rbExact.isChecked = true
                MatchType.STARTS_WITH -> dialogBinding.rbStartsWith.isChecked = true
                MatchType.CONTAINS -> dialogBinding.rbContains.isChecked = true
            }
        } else {
            dialogBinding.tvDialogTitle.text = "새 매크로 규칙 추가"
            dialogBinding.etTrigger.setText("!")
        }

        // Tag insertion helper chips
        dialogBinding.chipRandom.setOnClickListener {
            dialogBinding.etResponse.append("{random:1..6}")
        }
        dialogBinding.chipTime.setOnClickListener {
            dialogBinding.etResponse.append("{time:HH:mm:ss}")
        }
        dialogBinding.chipSender.setOnClickListener {
            dialogBinding.etResponse.append("{sender}")
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etRuleName.text.toString().trim()
            val trigger = dialogBinding.etTrigger.text.toString().trim()
            val response = dialogBinding.etResponse.text.toString().trim()

            if (trigger.isEmpty()) {
                Toast.makeText(requireContext(), "트리거 단어를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (response.isEmpty()) {
                Toast.makeText(requireContext(), "응답 문구를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val matchType = when {
                dialogBinding.rbStartsWith.isChecked -> MatchType.STARTS_WITH
                dialogBinding.rbContains.isChecked -> MatchType.CONTAINS
                else -> MatchType.EXACT
            }

            val prefs = PixelBotApplication.instance.prefs
            if (existingRule != null) {
                existingRule.name = if (name.isEmpty()) trigger else name
                existingRule.trigger = trigger
                existingRule.response = response
                existingRule.matchType = matchType
            } else {
                val newRule = MacroRule(
                    name = if (name.isEmpty()) trigger else name,
                    trigger = trigger,
                    matchType = matchType,
                    response = response,
                    isEnabled = true
                )
                rulesList.add(0, newRule)
            }

            prefs.saveMacroRules(rulesList)
            adapter.notifyDataSetChanged()
            dialog.dismiss()
            Toast.makeText(requireContext(), "규칙이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

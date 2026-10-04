package com.pixelbot.messenger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pixelbot.messenger.databinding.ItemMacroRuleBinding
import com.pixelbot.messenger.model.MacroRule

class MacroRuleAdapter(
    private val rules: MutableList<MacroRule>,
    private val onToggle: (MacroRule, Boolean) -> Unit,
    private val onEdit: (MacroRule) -> Unit,
    private val onDelete: (MacroRule) -> Unit
) : RecyclerView.Adapter<MacroRuleAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMacroRuleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMacroRuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = rules[position]
        val binding = holder.binding

        binding.tvRuleName.text = rule.name
        binding.tvMatchType.text = rule.matchType.displayName
        binding.tvTrigger.text = rule.trigger
        binding.tvResponse.text = rule.response

        binding.switchEnabled.setOnCheckedChangeListener(null)
        binding.switchEnabled.isChecked = rule.isEnabled
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            rule.isEnabled = isChecked
            onToggle(rule, isChecked)
        }

        binding.btnEdit.setOnClickListener {
            onEdit(rule)
        }

        binding.btnDelete.setOnClickListener {
            onDelete(rule)
        }
    }

    override fun getItemCount(): Int = rules.size

    fun updateData(newRules: List<MacroRule>) {
        rules.clear()
        rules.addAll(newRules)
        notifyDataSetChanged()
    }
}

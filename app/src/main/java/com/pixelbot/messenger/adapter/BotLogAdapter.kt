package com.pixelbot.messenger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pixelbot.messenger.R
import com.pixelbot.messenger.databinding.ItemBotLogBinding
import com.pixelbot.messenger.model.BotLog

class BotLogAdapter(
    private val logs: List<BotLog>
) : RecyclerView.Adapter<BotLogAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBotLogBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBotLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val binding = holder.binding

        binding.tvLogTime.text = log.formattedTime
        binding.tvLogTag.text = log.tag
        binding.tvLogMessage.text = log.message

        val context = binding.root.context
        val tagColor = when (log.tag) {
            "화면감지" -> ContextCompat.getColor(context, R.color.accent_secondary)
            "매크로실행", "전송완료" -> ContextCompat.getColor(context, R.color.accent_success)
            "JS실행" -> ContextCompat.getColor(context, R.color.accent_primary)
            "오류", "서비스중단" -> ContextCompat.getColor(context, R.color.accent_danger)
            else -> ContextCompat.getColor(context, R.color.text_secondary)
        }
        binding.tvLogTag.setTextColor(tagColor)
    }

    override fun getItemCount(): Int = logs.size
}

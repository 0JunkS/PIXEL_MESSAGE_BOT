package com.pixelbot.messenger.engine

import com.pixelbot.messenger.model.MacroRule
import com.pixelbot.messenger.model.MatchType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object MacroEngine {

    fun evaluate(
        rules: List<MacroRule>,
        room: String,
        msg: String,
        sender: String
    ): String? {
        val trimmedMsg = msg.trim()

        for (rule in rules) {
            if (!rule.isEnabled) continue
            val trigger = rule.trigger.trim()
            if (trigger.isEmpty()) continue

            val isMatch = when (rule.matchType) {
                MatchType.EXACT -> trimmedMsg == trigger
                MatchType.STARTS_WITH -> trimmedMsg.startsWith(trigger)
                MatchType.CONTAINS -> trimmedMsg.contains(trigger)
            }

            if (isMatch) {
                return formatResponse(rule.response, room, msg, sender)
            }
        }
        return null
    }

    private fun formatResponse(
        template: String,
        room: String,
        msg: String,
        sender: String
    ): String {
        var result = template
            .replace("{room}", room)
            .replace("{sender}", sender)
            .replace("{msg}", msg)

        // Process {random:min..max}
        val randomRegex = Regex("""\{random:(\d+)\.\.(\d+)\}""")
        result = randomRegex.replace(result) { matchResult ->
            val min = matchResult.groupValues[1].toIntOrNull() ?: 1
            val max = matchResult.groupValues[2].toIntOrNull() ?: 6
            val randomVal = if (min <= max) Random.nextInt(min, max + 1) else min
            randomVal.toString()
        }

        // Process {time:FORMAT}
        val timeRegex = Regex("""\{time:([^\}]+)\}""")
        result = timeRegex.replace(result) { matchResult ->
            val formatStr = matchResult.groupValues[1]
            try {
                val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
                sdf.format(Date())
            } catch (e: Exception) {
                Date().toString()
            }
        }

        return result
    }
}

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

        // Process {count} or {count:initial} or {count:name:initial}
        // e.g. "고승현 되기 {count}일차" -> 1일차, 2일차, 3일차...
        val countRegex = Regex("""\{count(?::([a-zA-Z0-9_-]+))?(?::(\d+))?\}""")
        result = countRegex.replace(result) { matchResult ->
            val firstArg = matchResult.groupValues.getOrNull(1)?.ifEmpty { null }
            val secondArg = matchResult.groupValues.getOrNull(2)?.ifEmpty { null }

            val (counterName, initialVal) = when {
                firstArg != null && firstArg.toIntOrNull() != null -> {
                    // {count:10}
                    "default" to firstArg.toInt()
                }
                firstArg != null && secondArg != null -> {
                    // {count:myvar:5}
                    firstArg to secondArg.toInt()
                }
                firstArg != null -> {
                    // {count:myvar}
                    firstArg to 1
                }
                else -> {
                    // {count}
                    "default" to 1
                }
            }

            val nextVal = com.pixelbot.messenger.PixelBotApplication.instance.prefs.getNextCounter(counterName, initialVal)
            nextVal.toString()
        }

        // Process {dday:yyyy-MM-dd}
        val ddayRegex = Regex("""\{dday:(\d{4}-\d{2}-\d{2})\}""")
        result = ddayRegex.replace(result) { matchResult ->
            val targetDateStr = matchResult.groupValues[1]
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val targetDate = sdf.parse(targetDateStr)
                if (targetDate != null) {
                    val diff = System.currentTimeMillis() - targetDate.time
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                    days.toString()
                } else "1"
            } catch (e: Exception) {
                "1"
            }
        }

        return result
    }
}

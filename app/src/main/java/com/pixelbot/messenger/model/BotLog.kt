package com.pixelbot.messenger.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BotLog(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

package com.safetap.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 10_000L) {
            return "just now"
        }

        if (diff < 60_000L) {
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            return "${seconds}s ago"
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutes < 60) {
            return "${minutes}m ago"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours < 24) {
            return "${hours}h ago"
        }

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days < 7) {
            return "${days}d ago"
        }

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

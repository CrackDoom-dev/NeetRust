// [KATALOG] shared.core.util
package com.example.neetrust.core.util

import java.util.Locale

object FormatUtils {

    fun formatDuration(seconds: Long): String {
        val totalSecs = if (seconds < 0) 0 else seconds
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }

    fun formatMillis(millis: Long): String {
        return formatDuration(millis / 1000)
    }
}

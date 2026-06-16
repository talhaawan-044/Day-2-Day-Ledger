package com.example.awancoalledger.utils

import java.util.*
import java.text.SimpleDateFormat

object DateUtils {
    fun isToday(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
               now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    fun isTomorrow(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        return tomorrow.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
               tomorrow.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    fun isOverdue(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        return timestamp < System.currentTimeMillis() && !isToday(timestamp)
    }

    fun isThisWeek(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
               now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR)
    }

    fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

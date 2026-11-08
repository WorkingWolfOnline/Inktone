package com.inktone.app.data

import android.content.Context

object FocusStore {
    private const val PREF = "focus_prefs"
    private const val KEY_TOTAL = "total_seconds"

    fun add(context: Context, seconds: Int) {
        if (seconds <= 0) return
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_TOTAL, prefs.getLong(KEY_TOTAL, 0L) + seconds).apply()
    }

    fun totalSeconds(context: Context): Long =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_TOTAL, 0L)
}

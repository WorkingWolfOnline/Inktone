package com.inktone.app.data

import android.content.Context

/** 花朵解锁状态：记录已解锁花朵序号，按累计专注时长跨阈值解锁。 */
object FlowerStore {
    private const val PREF = "flower_prefs"
    private const val KEY_UNLOCKED = "unlocked_orders"

    fun unlocked(context: Context): Set<Int> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_UNLOCKED, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** 依据累计专注时长，解锁所有已达阈值的花朵，返回最新已解锁集合。 */
    fun refresh(context: Context): Set<Int> {
        val totalMinutes = FocusStore.totalSeconds(context) / 60
        val due = FlowerCatalog.all.filter { it.unlockMinutes <= totalMinutes }.map { it.order }
        val current = unlocked(context)
        if (due.isEmpty() || current.containsAll(due)) return current
        val merged = current + due
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_UNLOCKED, merged.sorted().joinToString(","))
            .apply()
        return merged
    }
}

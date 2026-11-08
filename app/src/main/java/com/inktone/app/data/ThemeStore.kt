package com.inktone.app.data

import android.content.Context

object ThemeStore {
    private const val PREF = "theme_prefs"
    private const val KEY_INDEX = "theme_index"
    private const val KEY_DARK_MODE = "dark_mode"

    fun index(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0)

    fun setIndex(context: Context, i: Int) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_INDEX, i).apply()

    /** 0 = 跟随系统，1 = 浅色，2 = 深色 */
    fun darkMode(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_DARK_MODE, 0)

    fun setDarkMode(context: Context, mode: Int) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_DARK_MODE, mode).apply()
}

package com.inktone.app.data

import android.content.Context

object SettingsStore {
    private const val PREF = "settings"
    private const val KEY_SPEED = "reading_speed"
    private const val KEY_AUTO_PAGE = "auto_page_turn"
    private const val KEY_POINTER = "pointer_enabled"
    private const val KEY_FOCUS_MUSIC = "focus_music"
    private const val KEY_METEOR_STYLE = "meteor_style"
    private const val KEY_FOCUS_MODE = "focus_mode_enabled"
    private const val KEY_DRAWER_LOCK = "drawer_locked"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_REMINDER_MINUTES = "reminder_minutes"
    private const val KEY_LAST_SCRIPT = "last_script_index"

    fun readingSpeed(context: Context): Float =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_SPEED, 250f)

    fun setReadingSpeed(context: Context, speed: Float) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putFloat(KEY_SPEED, speed).apply()

    fun autoPageTurn(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_PAGE, true)

    fun setAutoPageTurn(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO_PAGE, v).apply()

    fun pointerEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_POINTER, true)

    fun setPointerEnabled(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_POINTER, v).apply()

    fun focusMusic(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_FOCUS_MUSIC, true)

    fun setFocusMusic(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOCUS_MUSIC, v).apply()

    fun meteorStyleIndex(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_METEOR_STYLE, 0)

    fun setMeteorStyleIndex(context: Context, index: Int) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_METEOR_STYLE, index).apply()

    fun focusModeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_FOCUS_MODE, true)

    fun setFocusModeEnabled(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOCUS_MODE, v).apply()

    fun drawerLocked(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_DRAWER_LOCK, false)

    fun setDrawerLocked(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_DRAWER_LOCK, v).apply()

    fun reminderEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_REMINDER_ENABLED, true)

    fun setReminderEnabled(context: Context, v: Boolean) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_REMINDER_ENABLED, v).apply()

    fun reminderMinutes(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_REMINDER_MINUTES, 25)

    fun setReminderMinutes(context: Context, v: Int) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_REMINDER_MINUTES, v).apply()

    fun lastScriptIndex(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_LAST_SCRIPT, -1)

    fun setLastScriptIndex(context: Context, v: Int) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_LAST_SCRIPT, v).apply()
}

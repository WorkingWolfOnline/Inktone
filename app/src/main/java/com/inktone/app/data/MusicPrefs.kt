package com.inktone.app.data

import android.content.Context

object MusicPrefs {
    private const val PREF = "music_prefs"
    private const val KEY_SELECTED = "selected_track_id"
    private const val KEY_VOLUME = "volume"

    fun selectedId(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_SELECTED, null)

    fun setSelected(context: Context, id: String?) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_SELECTED, id).apply()

    fun volume(context: Context): Float =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_VOLUME, 0.8f)

    fun setVolume(context: Context, v: Float) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putFloat(KEY_VOLUME, v).apply()
}

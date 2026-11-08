package com.inktone.app.data

import android.content.Context

object WallpaperStore {

    enum class Type { NONE, STATIC, DYNAMIC }

    enum class DynamicKind(val label: String) {
        FLOW("流动渐变"),
        PARTICLES("粒子漂浮")
    }

    private const val PREF = "wallpaper_prefs"
    private const val KEY_TYPE = "type"
    private const val KEY_STATIC_PATH = "static_path"
    private const val KEY_DYNAMIC = "dynamic_kind"

    fun type(context: Context): Type =
        Type.valueOf(
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_TYPE, Type.NONE.name) ?: Type.NONE.name
        )

    fun setType(context: Context, t: Type) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_TYPE, t.name).apply()

    fun staticPath(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_STATIC_PATH, null)

    fun setStaticPath(context: Context, path: String?) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_STATIC_PATH, path).apply()

    fun dynamicKind(context: Context): DynamicKind =
        DynamicKind.valueOf(
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_DYNAMIC, DynamicKind.FLOW.name) ?: DynamicKind.FLOW.name
        )

    fun setDynamicKind(context: Context, k: DynamicKind) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_DYNAMIC, k.name).apply()
}

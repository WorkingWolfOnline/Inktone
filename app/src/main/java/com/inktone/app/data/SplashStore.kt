package com.inktone.app.data

import android.content.Context

/** 开屏背景设置：记录选中的花朵序号（null 表示纯色背景）。 */
object SplashStore {
    private const val PREF = "splash_prefs"
    private const val KEY_FLOWER = "flower_order"

    fun flowerOrder(context: Context): Int? {
        val v = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_FLOWER, -1)
        return if (v > 0) v else null
    }

    fun setFlowerOrder(context: Context, order: Int?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_FLOWER, order ?: -1)
            .apply()
    }
}

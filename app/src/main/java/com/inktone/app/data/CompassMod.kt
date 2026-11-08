package com.inktone.app.data

import android.content.Context
import com.inktone.app.R
import java.io.File

object CompassMod {

    private const val ROOT = "墨音"
    private const val DIAL_DIR = "表盘"
    private const val NEEDLE_DIR = "指针"
    private val imageExts = setOf("png", "jpg", "jpeg", "webp")

    private fun root(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, ROOT)

    fun dialDir(context: Context): File = File(root(context), DIAL_DIR)
    fun needleDir(context: Context): File = File(root(context), NEEDLE_DIR)

    /** 首次启动时创建表盘/指针文件夹，并把默认资源复制进去作为默认模组 */
    fun ensureDefaults(context: Context) {
        val dd = dialDir(context).apply { mkdirs() }
        val nd = needleDir(context).apply { mkdirs() }
        if (dd.listFiles().isNullOrEmpty()) {
            copyResource(context, R.drawable.compass_dial, File(dd, "默认表盘.png"))
        }
        if (nd.listFiles().isNullOrEmpty()) {
            copyResource(context, R.drawable.compass_needle, File(nd, "默认指针.png"))
        }
    }

    /** 表盘文件夹里第一张图片 */
    fun dialFile(context: Context): File? = firstImage(dialDir(context))

    /** 指针文件夹里第一张图片 */
    fun needleFile(context: Context): File? = firstImage(needleDir(context))

    private fun firstImage(dir: File): File? =
        dir.listFiles()?.firstOrNull { it.isFile && it.extension.lowercase() in imageExts }

    private fun copyResource(context: Context, resId: Int, target: File) {
        try {
            context.resources.openRawResource(resId).use { ins ->
                target.outputStream().use { out -> ins.copyTo(out) }
            }
        } catch (e: Exception) {
            // 忽略复制失败
        }
    }
}

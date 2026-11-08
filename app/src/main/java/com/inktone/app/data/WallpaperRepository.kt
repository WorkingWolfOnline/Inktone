package com.inktone.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object WallpaperRepository {

    private const val ROOT = "墨音"
    private const val WALL_DIR = "壁纸"

    private fun dir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "$ROOT/$WALL_DIR")

    fun import(context: Context, uri: Uri): String {
        val d = dir(context).apply { mkdirs() }
        val name = queryName(context.contentResolver, uri) ?: "wallpaper.jpg"
        val ext = name.substringAfterLast('.', "").lowercase().ifBlank { "jpg" }
        val target = File(d, "${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        return target.absolutePath
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
}

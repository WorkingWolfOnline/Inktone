package com.inktone.app.data

import android.content.Context
import com.inktone.app.data.model.Direction
import com.inktone.app.data.model.Floor
import com.inktone.app.data.model.Photo
import java.io.File

object CompassRepository {

    private const val ROOT_DIR = "墨音"
    private const val PREFS = "compass_prefs"
    private const val KEY_FLOOR = "current_floor_name"
    private val imageExts = setOf("jpg", "jpeg", "png")

    private fun rootDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, ROOT_DIR)

    // ---- 楼层（每个楼层是一个文件夹） ----

    fun loadFloors(context: Context): List<Floor> {
        val root = rootDir(context)
        if (!root.exists()) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.map { Floor(it.name) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun addFloor(context: Context, name: String): Floor {
        val dir = File(rootDir(context), name).apply { mkdirs() }
        Direction.entries.forEach { File(dir, it.label).mkdirs() }
        setCurrentFloor(context, name)
        return Floor(name)
    }

    // ---- 照片（扫描楼层/方向文件夹，不依赖内部索引） ----

    fun loadPhotos(context: Context, floorName: String): List<Photo> {
        val floorDir = File(rootDir(context), floorName)
        val list = mutableListOf<Photo>()
        Direction.entries.forEach { d ->
            val dir = File(floorDir, d.label)
            dir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in imageExts }
                ?.forEach { f ->
                    list.add(Photo(f.name, floorName, d, f.nameWithoutExtension))
                }
        }
        return list.sortedBy { it.fileName }
    }

    fun photoFile(context: Context, photo: Photo): File =
        File(File(rootDir(context), photo.floorName), "${photo.direction.label}/${photo.fileName}")

    fun addPhoto(context: Context, floorName: String, direction: Direction, source: File): Photo {
        val dir = File(File(rootDir(context), floorName), direction.label).apply { mkdirs() }
        val fileName = "${System.currentTimeMillis()}.jpg"
        source.copyTo(File(dir, fileName), overwrite = true)
        return Photo(fileName, floorName, direction, fileName.substringBeforeLast('.'))
    }

    fun renamePhoto(context: Context, photo: Photo, newName: String): Photo {
        val safe = sanitizeFileName(newName).ifBlank { "photo" }
        val dir = File(File(rootDir(context), photo.floorName), photo.direction.label)
        val old = File(dir, photo.fileName)
        var candidate = "$safe.jpg"
        var n = 1
        while (File(dir, candidate).exists() && candidate != old.name) {
            candidate = "${safe}_$n.jpg"
            n++
        }
        if (old.exists() && candidate != old.name) {
            old.renameTo(File(dir, candidate))
        }
        return Photo(candidate, photo.floorName, photo.direction, candidate.substringBeforeLast('.'))
    }

    private fun sanitizeFileName(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|\\s]"), "_").trim('_').take(40)

    // ---- 当前楼层 ----

    fun currentFloorName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FLOOR, null)

    fun setCurrentFloor(context: Context, name: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_FLOOR, name).apply()
}

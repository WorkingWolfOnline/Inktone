package com.inktone.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.inktone.app.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object MusicRepository {

    private const val MUSIC_DIR = "music"
    private const val INDEX_FILE = "music_index.json"

    fun import(context: Context, uri: Uri): Track {
        val resolver = context.contentResolver
        val fileName = queryName(resolver, uri) ?: "未命名音频"
        val id = UUID.randomUUID().toString()
        val ext = fileName.substringAfterLast('.', "").lowercase().ifBlank { "mp3" }
        val dir = File(context.filesDir, MUSIC_DIR).apply { mkdirs() }
        val target = File(dir, "$id.$ext")
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        val track = Track(
            id = id,
            title = fileName.substringBeforeLast('.', fileName),
            fileName = fileName,
            localPath = "$id.$ext"
        )
        val tracks = loadAll(context).toMutableList()
        tracks.add(track)
        saveAll(context, tracks)
        return track
    }

    fun loadAll(context: Context): List<Track> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                list.add(fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(context: Context, tracks: List<Track>) {
        val arr = JSONArray()
        tracks.forEach { arr.put(toJson(it)) }
        indexFile(context).writeText(arr.toString())
    }

    fun trackFile(context: Context, track: Track): File =
        File(File(context.filesDir, MUSIC_DIR), track.localPath)

    fun delete(context: Context, track: Track) {
        trackFile(context, track).delete()
        saveAll(context, loadAll(context).filterNot { it.id == track.id })
    }

    private fun indexFile(context: Context): File = File(context.filesDir, INDEX_FILE)

    private fun queryName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }

    private fun toJson(track: Track) = JSONObject().apply {
        put("id", track.id)
        put("title", track.title)
        put("fileName", track.fileName)
        put("localPath", track.localPath)
        put("addedAt", track.addedAt)
    }

    private fun fromJson(o: JSONObject) = Track(
        id = o.getString("id"),
        title = o.getString("title"),
        fileName = o.getString("fileName"),
        localPath = o.getString("localPath"),
        addedAt = o.optLong("addedAt", 0L)
    )
}

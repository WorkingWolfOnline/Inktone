package com.inktone.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.inktone.app.data.model.Book
import com.inktone.app.data.model.BookFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object BookRepository {

    private const val BOOKS_DIR = "books"
    private const val INDEX_FILE = "books_index.json"

    fun import(context: Context, uri: Uri): Book {
        val resolver = context.contentResolver
        val fileName = queryName(resolver, uri) ?: "未命名文档"
        val id = UUID.randomUUID().toString()
        val ext = extensionOf(fileName)
        val dir = File(context.filesDir, BOOKS_DIR).apply { mkdirs() }
        val target = File(dir, "$id.$ext")
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        val book = Book(
            id = id,
            title = fileName.substringBeforeLast('.', fileName),
            fileName = fileName,
            localPath = "$id.$ext",
            format = formatOf(fileName)
        )
        val books = loadAll(context).toMutableList()
        books.add(0, book)
        saveAll(context, books)
        return book
    }

    fun loadAll(context: Context): List<Book> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<Book>()
            for (i in 0 until arr.length()) {
                list.add(fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(context: Context, books: List<Book>) {
        val arr = JSONArray()
        books.forEach { arr.put(toJson(it)) }
        indexFile(context).writeText(arr.toString())
    }

    fun bookFile(context: Context, book: Book): File =
        File(File(context.filesDir, BOOKS_DIR), book.localPath)

    fun delete(context: Context, book: Book) {
        bookFile(context, book).delete()
        saveAll(context, loadAll(context).filterNot { it.id == book.id })
    }

    fun updateProgress(context: Context, bookId: String, progress: Float) {
        val books = loadAll(context).map {
            if (it.id == bookId) it.copy(progress = progress, lastReadAt = System.currentTimeMillis())
            else it
        }
        saveAll(context, books)
    }

    fun extractDocument(context: Context, book: Book): ReaderDocument? {
        val file = bookFile(context, book)
        return when (book.format) {
            BookFormat.TXT -> TextParser.parseTxt(readText(file))
            BookFormat.EPUB -> TextParser.parseEpub(file).takeIf { it.total > 0 }
            BookFormat.DOCX -> TextParser.parseDocx(file).takeIf { it.total > 0 }
            BookFormat.PDF -> null
        }
    }

    fun pdfChapters(context: Context, book: Book): List<Chapter> {
        val outlines = PdfOutlineParser.outline(bookFile(context, book))
        if (outlines.isNotEmpty()) {
            return outlines.map { Chapter(it.title, it.page, 1) }
        }
        val count = TextParser.pdfPageCount(bookFile(context, book))
        return (1..count).map { Chapter("第 $it 页", it - 1, 0) }
    }

    private fun readText(file: File): String {
        val bytes = file.readBytes()
        val utf8 = String(bytes, Charsets.UTF_8)
        return if (utf8.contains('\uFFFD')) String(bytes, charset("GBK")) else utf8
    }

    private fun indexFile(context: Context): File = File(context.filesDir, INDEX_FILE)

    private fun queryName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }

    private fun formatOf(name: String): BookFormat = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> BookFormat.PDF
        "epub" -> BookFormat.EPUB
        "doc", "docx" -> BookFormat.DOCX
        else -> BookFormat.TXT
    }

    private fun extensionOf(name: String): String =
        name.substringAfterLast('.', "").lowercase().ifBlank { "txt" }

    private fun toJson(book: Book) = JSONObject().apply {
        put("id", book.id)
        put("title", book.title)
        put("fileName", book.fileName)
        put("localPath", book.localPath)
        put("format", book.format.name)
        put("progress", book.progress.toDouble())
        put("lastReadAt", book.lastReadAt)
    }

    private fun fromJson(o: JSONObject) = Book(
        id = o.getString("id"),
        title = o.getString("title"),
        fileName = o.getString("fileName"),
        localPath = o.getString("localPath"),
        format = BookFormat.valueOf(o.getString("format")),
        progress = o.optDouble("progress", 0.0).toFloat(),
        lastReadAt = o.optLong("lastReadAt", 0L)
    )
}

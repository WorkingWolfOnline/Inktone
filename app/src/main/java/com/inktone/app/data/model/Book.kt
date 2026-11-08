package com.inktone.app.data.model

enum class BookFormat { TXT, EPUB, DOCX, PDF }

data class Book(
    val id: String,
    val title: String,
    val fileName: String,
    val localPath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastReadAt: Long = 0L
)

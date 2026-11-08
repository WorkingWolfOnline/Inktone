package com.inktone.app.data.model

data class Track(
    val id: String,
    val title: String,
    val fileName: String,
    val localPath: String,
    val addedAt: Long = System.currentTimeMillis()
)

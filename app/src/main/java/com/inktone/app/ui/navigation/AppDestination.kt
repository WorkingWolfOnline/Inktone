package com.inktone.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    Bookshelf("书架", Icons.Filled.AutoStories, "你的本地书库"),
    Import("导入文件", Icons.Filled.AddCircle, "支持 PDF / Word / TXT / EPUB"),
    MusicLibrary("音乐库", Icons.Filled.LibraryMusic, "本地专注音乐"),
    FlowerCollection("拾花集", Icons.Filled.LocalFlorist, "阅读时长换取的繁花图鉴"),
    Compass("指南针", Icons.Filled.Explore, "方向拍照与照片管理"),
    Puzzle("拼图记忆", Icons.Filled.Extension, "图片拼图记忆"),
    Knowledge("知识卡片", Icons.Filled.Style, "回顾复习"),
    Wallpaper("壁纸", Icons.Filled.Wallpaper, "静态 / 动态壁纸"),
    Settings("设置", Icons.Filled.Settings, "阅读与软件偏好")
}

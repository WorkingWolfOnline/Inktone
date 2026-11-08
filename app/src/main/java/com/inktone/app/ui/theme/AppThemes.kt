package com.inktone.app.ui.theme

import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color
)

val AppThemes = listOf(
    AppTheme(
        name = "清新蓝绿",
        primary = Color(0xFF3A7CA5),
        secondary = Color(0xFF6FA47A),
        tertiary = Color(0xFFE8C07A),
        background = Color(0xFFF5F0E8),
        surface = Color(0xFFFFFFFF)
    ),
    AppTheme(
        name = "晴空蓝",
        primary = Color(0xFF007BFF),
        secondary = Color(0xFF6C757D),
        tertiary = Color(0xFF17A2B8),
        background = Color(0xFFF0F4F8),
        surface = Color(0xFFFFFFFF)
    ),
    AppTheme(
        name = "蜜桃暖阳",
        primary = Color(0xFFE07A5F),
        secondary = Color(0xFFF2CC8F),
        tertiary = Color(0xFF81B29A),
        background = Color(0xFFFDF6EC),
        surface = Color(0xFFFFFFFF)
    ),
    AppTheme(
        name = "薰衣草",
        primary = Color(0xFF7C6BA8),
        secondary = Color(0xFF8BB8D8),
        tertiary = Color(0xFFE5A0B4),
        background = Color(0xFFF6F2FA),
        surface = Color(0xFFFFFFFF)
    )
)

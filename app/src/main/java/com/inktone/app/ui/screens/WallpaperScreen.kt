package com.inktone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.inktone.app.data.WallpaperStore
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.theme.AppTheme
import com.inktone.app.ui.theme.AppThemes

@Composable
fun WallpaperScreen(
    type: WallpaperStore.Type,
    dynamicKind: WallpaperStore.DynamicKind,
    themeIndex: Int,
    onThemeChange: (Int) -> Unit,
    onSelectType: (WallpaperStore.Type) -> Unit,
    onSelectDynamic: (WallpaperStore.DynamicKind) -> Unit,
    onImportStatic: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(title = "壁纸与主题", subtitle = "静态 / 动态 / 配色", onOpenDrawer = onOpenDrawer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("壁纸", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(8.dp))
            OptionCard(
                title = "默认背景",
                desc = "纯色渐变背景",
                selected = type == WallpaperStore.Type.NONE,
                onClick = { onSelectType(WallpaperStore.Type.NONE) }
            )
            Spacer(Modifier.size(10.dp))
            OptionCard(
                title = "静态壁纸",
                desc = "导入图片 / GIF / 视频作为背景",
                selected = type == WallpaperStore.Type.STATIC,
                onClick = onImportStatic
            )
            Spacer(Modifier.size(10.dp))
            OptionCard(
                title = "动态 · 流动渐变",
                desc = "柔和光晕缓慢流动",
                selected = type == WallpaperStore.Type.DYNAMIC && dynamicKind == WallpaperStore.DynamicKind.FLOW,
                onClick = {
                    onSelectType(WallpaperStore.Type.DYNAMIC)
                    onSelectDynamic(WallpaperStore.DynamicKind.FLOW)
                }
            )
            Spacer(Modifier.size(10.dp))
            OptionCard(
                title = "动态 · 粒子漂浮",
                desc = "淡蓝粒子向上漂浮",
                selected = type == WallpaperStore.Type.DYNAMIC && dynamicKind == WallpaperStore.DynamicKind.PARTICLES,
                onClick = {
                    onSelectType(WallpaperStore.Type.DYNAMIC)
                    onSelectDynamic(WallpaperStore.DynamicKind.PARTICLES)
                }
            )

            Spacer(Modifier.size(22.dp))
            Text("主题配色", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(8.dp))
            AppThemes.forEachIndexed { i, t ->
                ThemeRow(theme = t, selected = i == themeIndex, onClick = { onThemeChange(i) })
                Spacer(Modifier.size(10.dp))
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(theme.primary, theme.secondary, theme.tertiary, theme.background, theme.surface).forEach { c ->
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(theme.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

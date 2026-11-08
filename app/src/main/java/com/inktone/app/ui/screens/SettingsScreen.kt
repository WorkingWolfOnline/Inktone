package com.inktone.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inktone.app.data.SettingsStore
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.theme.AppThemes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeIndex: Int,
    onThemeChange: (Int) -> Unit,
    darkMode: Int,
    onDarkModeChange: (Int) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    var speed by remember { mutableStateOf(SettingsStore.readingSpeed(context)) }
    var autoPageTurn by remember { mutableStateOf(SettingsStore.autoPageTurn(context)) }
    var pointerEnabled by remember { mutableStateOf(SettingsStore.pointerEnabled(context)) }
    var focusMusic by remember { mutableStateOf(SettingsStore.focusMusic(context)) }
    var focusMode by remember { mutableStateOf(SettingsStore.focusModeEnabled(context)) }
    var meteorStyleIndex by remember { mutableStateOf(SettingsStore.meteorStyleIndex(context)) }
    var reminderEnabled by remember { mutableStateOf(SettingsStore.reminderEnabled(context)) }
    var reminderMinutes by remember { mutableStateOf(SettingsStore.reminderMinutes(context)) }
    var reminderUseCustom by remember { mutableStateOf(!listOf(15, 25, 30, 45, 60).contains(reminderMinutes)) }
    var reminderCustom by remember { mutableStateOf(reminderMinutes.toFloat().coerceIn(5f, 120f)) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(title = "设置", onOpenDrawer = onOpenDrawer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        LazyColumn(Modifier.fillMaxSize()) {
            item { GroupHeader("阅读") }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("阅读速度", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${speed.toInt()} 字/分钟", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it; SettingsStore.setReadingSpeed(context, it) },
                    valueRange = 100f..500f,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item { SwitchRow("自动翻页", "指针读到页末自动前进", autoPageTurn) { autoPageTurn = it; SettingsStore.setAutoPageTurn(context, it) } }
            item { SwitchRow("阅读指针", "逐字跟随、当前字高亮", pointerEnabled) { pointerEnabled = it; SettingsStore.setPointerEnabled(context, it) } }

            item { GroupHeader("显示") }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    ChoiceRow("跟随系统", darkMode == 0) { onDarkModeChange(0) }
                    ChoiceRow("浅色模式", darkMode == 1) { onDarkModeChange(1) }
                    ChoiceRow("深色模式", darkMode == 2) { onDarkModeChange(2) }
                }
            }

            item { GroupHeader("外观") }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    AppThemes.forEachIndexed { i, t ->
                        ThemeItem(theme = t, selected = i == themeIndex, onClick = { onThemeChange(i) })
                        Spacer(Modifier.size(8.dp))
                    }
                }
            }

            item { GroupHeader("指针颜色") }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    MeteorStyles.forEachIndexed { i, s ->
                        MeteorStyleItem(style = s, selected = i == meteorStyleIndex, onClick = {
                            meteorStyleIndex = i
                            SettingsStore.setMeteorStyleIndex(context, i)
                        })
                        Spacer(Modifier.size(8.dp))
                    }
                }
            }

            item { GroupHeader("专注") }
            item { SwitchRow("专注模式", "长按屏幕进入专注阅读，滑动仅移动光晕", focusMode) { focusMode = it; SettingsStore.setFocusModeEnabled(context, it) } }
            item { SwitchRow("专注播放音乐", "按住屏幕时自动播放选中音景", focusMusic) { focusMusic = it; SettingsStore.setFocusMusic(context, it) } }
            item { SwitchRow("阅读提醒", "到达目标时长后，萌娘送出慰问", reminderEnabled) { reminderEnabled = it; SettingsStore.setReminderEnabled(context, it) } }
            if (reminderEnabled) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        Text("目标时长", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 25, 30, 45, 60).forEach { m ->
                                val selected = !reminderUseCustom && reminderMinutes == m
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            reminderUseCustom = false
                                            reminderMinutes = m
                                            SettingsStore.setReminderMinutes(context, m)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "$m 分钟",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (reminderUseCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { reminderUseCustom = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "自定义",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (reminderUseCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (reminderUseCustom) {
                            Slider(
                                value = reminderCustom,
                                onValueChange = {
                                    reminderCustom = it
                                    reminderMinutes = it.toInt()
                                    SettingsStore.setReminderMinutes(context, it.toInt())
                                },
                                valueRange = 5f..120f,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                "${reminderCustom.toInt()} 分钟",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { GroupHeader("关于") }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text("墨音 Inktone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("版本 1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Text(
                    "感谢你使用墨音。如果它曾带给你片刻宁静与欢喜，欢迎在爱发电上支持我，或在 GitHub 上点个 Star、反馈问题。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    LinkRow(Icons.Filled.Favorite, "爱发电", "afdian.com/a/Kaiyu_132", "https://afdian.com/a/Kaiyu_132")
                    Spacer(Modifier.size(8.dp))
                    LinkRow(Icons.Filled.Code, "GitHub", "github.com/WorkingWolfOnline", "https://github.com/WorkingWolfOnline")
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun MeteorStyleItem(style: MeteorStyle, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(style.head))
        }
        Spacer(Modifier.width(12.dp))
        Text(style.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun ThemeItem(theme: com.inktone.app.ui.theme.AppTheme, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(theme.primary, theme.secondary, theme.tertiary, theme.background, theme.surface).forEach { c ->
                Box(Modifier.size(18.dp).clip(CircleShape).background(c).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(theme.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

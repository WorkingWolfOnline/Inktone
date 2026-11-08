package com.inktone.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inktone.app.audio.MusicPlayer
import com.inktone.app.data.BookRepository
import com.inktone.app.data.Chapter
import com.inktone.app.data.ReaderDocument
import com.inktone.app.data.FlowerStore
import com.inktone.app.data.FocusStore
import com.inktone.app.data.MusicPrefs
import com.inktone.app.data.MusicRepository
import com.inktone.app.data.SettingsStore
import com.inktone.app.data.model.Book
import com.inktone.app.data.model.BookFormat
import com.inktone.app.ui.focus.FocusGlow
import com.inktone.app.ui.focus.FocusLamp
import com.inktone.app.ui.focus.FocusState
import com.inktone.app.ui.focus.focusPress
import com.inktone.app.ui.focus.rememberFocusState

private val speeds = listOf(150f, 250f, 350f)

@Composable
fun ReaderScreen(book: Book, onBack: () -> Unit, drawerLocked: Boolean = false, onToggleLock: () -> Unit = {}) {
    val context = LocalContext.current
    val isPdf = book.format == BookFormat.PDF
    val document = remember(book.id) {
        if (isPdf) null else BookRepository.extractDocument(context, book)
    }
    val chapters = remember(book.id, document) {
        if (isPdf) BookRepository.pdfChapters(context, book) else document?.chapters ?: emptyList()
    }
    val focus = rememberFocusState()
    var speed by remember { mutableStateOf(SettingsStore.readingSpeed(context)) }
    val meteorStyle = remember(book.id) { MeteorStyles[SettingsStore.meteorStyleIndex(context) % MeteorStyles.size] }
    var showToc by remember { mutableStateOf(false) }
    var jumpTo by remember { mutableStateOf<Int?>(null) }
    var pdfJumpTo by remember { mutableStateOf<Int?>(null) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var reminderTriggered by remember { mutableStateOf(false) }
    val reminderEnabled = SettingsStore.reminderEnabled(context)
    val reminderMinutes = SettingsStore.reminderMinutes(context)

    LaunchedEffect(focus.seconds) {
        if (reminderEnabled && !reminderTriggered && reminderMinutes > 0 &&
            focus.seconds >= reminderMinutes * 60
        ) {
            reminderTriggered = true
            showReminder = true
        }
    }

    LaunchedEffect(focus.isPressing) {
        if (focus.isPressing) {
            if (SettingsStore.focusMusic(context)) {
                val selectedId = MusicPrefs.selectedId(context)
                val track = MusicRepository.loadAll(context).firstOrNull { it.id == selectedId }
                if (track != null) {
                    MusicPlayer.play(MusicRepository.trackFile(context, track).absolutePath, track.id)
                }
            }
        } else {
            MusicPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            FocusStore.add(context, focus.seconds)
            FlowerStore.refresh(context)
            MusicPlayer.pause()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.format.name + " · 阅读中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (chapters.isNotEmpty()) {
                Text(
                    text = "目录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showToc = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "${speed.toInt()}字/分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSpeedDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            IconButton(onClick = onToggleLock) {
                Icon(
                    if (drawerLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (drawerLocked) "解锁侧边栏" else "锁定侧边栏",
                    tint = if (drawerLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FocusLamp(isFocused = focus.isPressing, modifier = Modifier.padding(end = 16.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                isPdf -> PdfReader(
                    file = BookRepository.bookFile(context, book),
                    onProgress = { p -> BookRepository.updateProgress(context, book.id, p) },
                    jumpToPage = pdfJumpTo
                )
                document == null -> UnsupportedFormat(book.format)
                else -> FocusReader(
                    document = document,
                    focus = focus,
                    speed = speed,
                    onProgress = { p -> BookRepository.updateProgress(context, book.id, p) },
                    jumpTo = jumpTo,
                    autoPageTurn = SettingsStore.autoPageTurn(context),
                    initialProgress = book.progress,
                    style = meteorStyle,
                    focusModeEnabled = SettingsStore.focusModeEnabled(context)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "专注 " + formatSeconds(focus.seconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "已读 " + (book.progress * 100).toInt() + "%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showToc) {
        AlertDialog(
            onDismissRequest = { showToc = false },
            title = { Text("目录") },
            text = {
                if (chapters.isEmpty()) {
                    Text("未识别到章节标题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.height(420.dp)) {
                        items(chapters) { ch ->
                            val indent = (ch.level - 1).coerceAtLeast(0) * 16
                            Text(
                                text = ch.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isPdf) pdfJumpTo = ch.startIndex else jumpTo = ch.startIndex
                                        showToc = false
                                    }
                                    .padding(start = indent.dp, top = 10.dp, bottom = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToc = false }) { Text("关闭") }
            }
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("阅读速度") },
            text = {
                Column {
                    Slider(
                        value = speed,
                        onValueChange = { speed = it; SettingsStore.setReadingSpeed(context, it) },
                        valueRange = 100f..500f
                    )
                    Text("${speed.toInt()} 字/分钟", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("确定") }
            }
        )
    }

    if (showReminder) {
        ReminderDialog(
            minutes = focus.seconds / 60,
            onDismiss = { showReminder = false }
        )
    }
}

@Composable
private fun FocusReader(
    document: ReaderDocument,
    focus: FocusState,
    speed: Float,
    onProgress: (Float) -> Unit,
    jumpTo: Int? = null,
    autoPageTurn: Boolean = true,
    initialProgress: Float = 0f,
    style: MeteorStyle = MeteorStyles.first(),
    focusModeEnabled: Boolean = true
) {
    Box(Modifier.fillMaxSize().focusPress(focus, focusModeEnabled)) {
        MeteorReader(document, focus, speed, onProgress, jumpTo, autoPageTurn, initialProgress, style)
        FocusGlow(focus)
    }
}

@Composable
private fun UnsupportedFormat(format: BookFormat) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "无法解析该文件的文本内容",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "该 ${format.name} 文件可能为空或格式不受支持",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatSeconds(s: Int): String {
    val m = s / 60
    val sec = s % 60
    return m.toString().padStart(2, '0') + ":" + sec.toString().padStart(2, '0')
}

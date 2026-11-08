package com.inktone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.inktone.app.audio.MusicPlayer
import com.inktone.app.data.MusicRepository
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inktone.app.data.model.Track
import kotlin.math.max
import com.inktone.app.ui.components.FrostedSurface
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.theme.Gold
import com.inktone.app.ui.theme.GoldBright

@Composable
fun MusicLibraryScreen(
    tracks: List<Track>,
    selectedId: String?,
    volume: Float,
    onImport: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isPlaying = MusicPlayer.isPlaying()
            position = MusicPlayer.position()
            duration = MusicPlayer.duration()
            delay(500)
        }
    }

    val playCurrent: () -> Unit = {
        val id = MusicPlayer.currentId ?: selectedId
        val track = tracks.firstOrNull { it.id == id }
        if (track != null) {
            MusicPlayer.play(MusicRepository.trackFile(context, track).absolutePath, track.id)
        }
    }
    val playAt: (Int) -> Unit = { delta ->
        val current = tracks.firstOrNull { it.id == MusicPlayer.currentId }
        val idx = tracks.indexOf(current)
        val next = tracks.getOrNull(idx + delta)
        if (next != null) MusicPlayer.play(MusicRepository.trackFile(context, next).absolutePath, next.id)
    }
    val currentTrack = tracks.firstOrNull { it.id == MusicPlayer.currentId }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(
            title = "音乐库",
            subtitle = "专注背景音",
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(onClick = onImport) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "导入音乐",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )
        VolumeRow(volume, onVolumeChange)
        if (tracks.isEmpty()) {
            EmptyMusic(onImport)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        selected = track.id == selectedId,
                        onSelect = {
                            onSelect(track.id)
                            MusicPlayer.play(MusicRepository.trackFile(context, track).absolutePath, track.id)
                        },
                        onDelete = { onDelete(track.id) }
                    )
                }
            }
            MiniPlayerBar(
                title = currentTrack?.title ?: "未播放",
                isPlaying = isPlaying,
                onOpen = { showPlayer = true },
                onToggle = { if (isPlaying) MusicPlayer.pause() else playCurrent() },
                onNext = { playAt(1) }
            )
        }
    }

    if (showPlayer) {
        PlayerSheet(
            title = currentTrack?.title ?: "未播放",
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            onToggle = { if (isPlaying) MusicPlayer.pause() else playCurrent() },
            onPrev = { playAt(-1) },
            onNext = { playAt(1) },
            onDismiss = { showPlayer = false }
        )
    }
}

@Composable
private fun MiniPlayerBar(title: String, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("本地音乐", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onToggle) {
            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "播放/暂停", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun PlayerSheet(
    title: String,
    isPlaying: Boolean,
    position: Int,
    duration: Int,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "disc")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Restart), label = "rotation"
    )
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "收起", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("正在播放", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            Spacer(Modifier.size(20.dp))
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer { rotationZ = if (isPlaying) rotation else 0f }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
            }
            Spacer(Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(16.dp))
            Slider(
                value = dragPosition ?: position.toFloat(),
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { MusicPlayer.seekTo(it.toInt()) }
                    dragPosition = null
                },
                valueRange = 0f..max(1, duration).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(position), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(72.dp)) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "播放/暂停",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

private fun formatTime(ms: Int): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}

@Composable
private fun VolumeRow(volume: Float, onVolumeChange: (Float) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrackRow(
    track: Track,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val tint = if (selected) GoldBright else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.fileName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .background(GoldBright, CircleShape)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyMusic(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrostedSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "还没有音乐",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "导入本地 MP3 等音频，作为专注阅读的背景音。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = onImport,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("导入音乐", style = MaterialTheme.typography.labelLarge)
        }
    }
}

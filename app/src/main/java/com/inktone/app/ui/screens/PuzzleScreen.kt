package com.inktone.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inktone.app.ui.components.GlassTopBar
import kotlin.math.abs

private val gridSizes = listOf(3, 4, 5)

@Composable
fun PuzzleScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var gridSize by remember { mutableStateOf(3) }
    var tiles by remember { mutableStateOf<List<Int>>(emptyList()) }

    val pieces = remember(source, gridSize) { source?.let { splitBitmap(it, gridSize) } ?: emptyList() }
    val cellRatio = remember(source) { source?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            source = loadBitmap(context, uri)
            tiles = shuffleTiles(gridSize)
        }
    }

    val total = gridSize * gridSize
    val completed = tiles.isNotEmpty() && tiles.indices.all { tiles[it] == it || (it == total - 1 && tiles[it] == -1) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(
            title = "拼图记忆",
            subtitle = "${gridSize}×${gridSize} 滑动拼图",
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Filled.Add, contentDescription = "导入图片", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (source == null) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("导入一张图片，开始拼图记忆", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("图片保持完整，按原比例切成拼图块", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { launcher.launch(arrayOf("image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("导入图片", color = MaterialTheme.colorScheme.onPrimary) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    gridSizes.forEach { g ->
                        val sel = g == gridSize
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    gridSize = g
                                    tiles = shuffleTiles(g)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${g}×${g}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (completed) {
                    Text(
                        "已还原！",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)
                    )
                } else {
                    Text(
                        "点击空格相邻的块来移动",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridSize),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(total) { pos ->
                        val piece = tiles.getOrElse(pos) { -1 }
                        Box(
                            Modifier
                                .aspectRatio(cellRatio)
                                .clip(RoundedCornerShape(5.dp))
                                .clickable(enabled = !completed) { tiles = moveTile(tiles, pos, gridSize) }
                        ) {
                            if (piece in pieces.indices) {
                                Image(
                                    bitmap = pieces[piece].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { tiles = shuffleTiles(gridSize) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("重新打乱", color = MaterialTheme.colorScheme.onSecondary) }
            }
        }
    }
}

private fun moveTile(tiles: List<Int>, pos: Int, n: Int): List<Int> {
    val empty = tiles.indexOf(-1)
    if (empty < 0) return tiles
    val dr = abs(empty / n - pos / n)
    val dc = abs(empty % n - pos % n)
    if (dr + dc != 1) return tiles
    val m = tiles.toMutableList()
    val t = m[pos]
    m[pos] = m[empty]
    m[empty] = t
    return m
}

private fun shuffleTiles(n: Int): List<Int> {
    val list = (0 until n * n).toMutableList()
    list[n * n - 1] = -1
    repeat(500) {
        val empty = list.indexOf(-1)
        val r = empty / n
        val c = empty % n
        val neighbors = mutableListOf<Int>()
        if (r > 0) neighbors.add(empty - n)
        if (r < n - 1) neighbors.add(empty + n)
        if (c > 0) neighbors.add(empty - 1)
        if (c < n - 1) neighbors.add(empty + 1)
        val nb = neighbors.random()
        val t = list[empty]
        list[empty] = list[nb]
        list[nb] = t
    }
    return list
}

private fun splitBitmap(bitmap: Bitmap, n: Int): List<Bitmap> {
    val w = bitmap.width / n
    val h = bitmap.height / n
    return List(n * n) { i ->
        val row = i / n
        val col = i % n
        Bitmap.createBitmap(bitmap, col * w, row * h, w, h)
    }
}

private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1400) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null
    }
}

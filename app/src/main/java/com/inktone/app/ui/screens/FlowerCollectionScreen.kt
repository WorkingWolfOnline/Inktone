package com.inktone.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inktone.app.data.Flower
import com.inktone.app.data.FlowerCatalog
import com.inktone.app.data.FocusStore
import com.inktone.app.ui.components.GlassTopBar

@Composable
fun FlowerCollectionScreen(
    unlocked: Set<Int>,
    onOpenDrawer: () -> Unit,
    splashFlowerOrder: Int?,
    onSetSplashFlower: (Int?) -> Unit
) {
    val context = LocalContext.current
    val flowers = FlowerCatalog.all
    var selected by remember { mutableStateOf<Flower?>(null) }
    val totalMinutes = remember { (FocusStore.totalSeconds(context) / 60).toInt() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(title = "拾花集", subtitle = "读得越久，花开越多", onOpenDrawer = onOpenDrawer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已解锁 ${unlocked.size} / ${flowers.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "累计专注 ${formatMinutes(totalMinutes)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(flowers, key = { it.order }) { flower ->
                FlowerCard(
                    flower = flower,
                    unlocked = flower.order in unlocked,
                    onClick = { selected = flower }
                )
            }
        }
    }

    selected?.let { flower ->
        FlowerDetailDialog(
            flower = flower,
            unlocked = flower.order in unlocked,
            totalMinutes = totalMinutes,
            isSplash = splashFlowerOrder == flower.order,
            onSetSplash = { onSetSplashFlower(if (splashFlowerOrder == flower.order) null else flower.order) },
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun FlowerCard(flower: Flower, unlocked: Boolean, onClick: () -> Unit) {
    val tierColor = Color(flower.tier.color)
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (unlocked) tierColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            Image(
                painter = painterResource(flower.drawableRes),
                contentDescription = if (unlocked) flower.name else "未解锁",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (unlocked) null else ColorFilter.tint(Color.Black)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (unlocked) flower.name else "？？？",
            style = MaterialTheme.typography.titleSmall,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (unlocked) flower.tier.title else "？？？",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun FlowerDetailDialog(
    flower: Flower,
    unlocked: Boolean,
    totalMinutes: Int,
    isSplash: Boolean,
    onSetSplash: () -> Unit,
    onDismiss: () -> Unit
) {
    val tierColor = Color(flower.tier.color)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (unlocked) tierColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(flower.drawableRes),
                        contentDescription = if (unlocked) flower.name else "未解锁",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (unlocked) null else ColorFilter.tint(Color.Black)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (unlocked) "${flower.name} · ${flower.tier.title}" else "？？？",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (unlocked) "累计专注 ${formatMinutes(flower.unlockMinutes)}获得"
                    else "再累计专注 ${formatMinutes((flower.unlockMinutes - totalMinutes).coerceAtLeast(0))}即可解锁",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                DetailLine("形象", if (unlocked) flower.trait else "？？？")
                DetailLine("含义", if (unlocked) flower.meaning else "？？？")
                DetailLine("寓意", if (unlocked) flower.moral else "？？？")

                if (unlocked) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onSetSplash, modifier = Modifier.align(Alignment.End)) {
                        Text(if (isSplash) "已设为开屏背景 · 点击取消" else "设为开屏背景")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, text: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatMinutes(min: Int): String {
    if (min < 60) return "$min 分钟"
    val h = min / 60
    val m = min % 60
    return if (m == 0) "$h 小时" else "$h 小时 $m 分钟"
}

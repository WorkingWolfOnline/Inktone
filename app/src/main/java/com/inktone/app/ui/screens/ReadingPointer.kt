package com.inktone.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inktone.app.data.ReaderDocument
import com.inktone.app.ui.focus.FocusState
import com.inktone.app.ui.theme.Gold
import com.inktone.app.ui.theme.GoldBright
import kotlinx.coroutines.delay
import kotlin.math.abs

data class MeteorStyle(val name: String, val head: Color, val star: Color)

val MeteorStyles = listOf(
    MeteorStyle("鎏金", GoldBright, Gold),
    MeteorStyle("冰蓝", Color(0xFF59B8FF), Color(0xFF7FCBFF)),
    MeteorStyle("青绿", Color(0xFF4FD8A8), Color(0xFF79E3BE)),
    MeteorStyle("紫罗兰", Color(0xFFB088FF), Color(0xFFC6A6FF)),
    MeteorStyle("暖橙", Color(0xFFFF9B6A), Color(0xFFFFB48B))
)

private data class LevelMetrics(
    val fontSizeSp: Float,
    val lineHeightSp: Float,
    val topSpacingDp: Float,
    val bottomSpacingDp: Float
)

private val bodyMetrics = LevelMetrics(18f, 32f, 6f, 6f)

private fun metricsFor(level: Int): LevelMetrics = when (level) {
    1 -> LevelMetrics(24f, 40f, 16f, 10f)
    2 -> LevelMetrics(21f, 36f, 14f, 8f)
    3 -> LevelMetrics(19f, 32f, 12f, 8f)
    4 -> LevelMetrics(18f, 30f, 10f, 6f)
    else -> bodyMetrics
}

private data class ReaderChar(val char: Char, val index: Int, val widthPx: Float)

private data class ReaderLine(
    val chars: List<ReaderChar>,
    val level: Int,
    val indentPx: Float,
    val topSpacingPx: Float,
    val bottomSpacingPx: Float,
    val lineHeightPx: Float,
    val fontSizeSp: Float
)

private class ReaderLayout(
    val lines: List<ReaderLine>,
    val lineOfIndex: IntArray,
    val pauseAfter: BooleanArray,
    val total: Int
)

@Composable
fun MeteorReader(
    document: ReaderDocument,
    focus: FocusState,
    speed: Float,
    onProgress: (Float) -> Unit,
    jumpTo: Int? = null,
    autoPageTurn: Boolean = true,
    initialProgress: Float = 0f,
    style: MeteorStyle = MeteorStyles.first()
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth.toFloat()
        val layout = remember(document, maxWidthPx) {
            buildLines(document, measurer, maxWidthPx, density)
        }

        var currentIndex by remember(document, layout.total) {
            mutableStateOf((layout.total * initialProgress).toInt().coerceIn(0, (layout.total - 1).coerceAtLeast(0)))
        }
        val currentIndexF = remember(document) { Animatable(currentIndex.toFloat()) }
        val listState = rememberLazyListState()
        val base = (60000f / speed).toLong().coerceAtLeast(80L)
        val currentLine = if (layout.total == 0) 0 else layout.lineOfIndex.getOrElse(currentIndex) { 0 }

        LaunchedEffect(jumpTo) {
            if (jumpTo != null) {
                val target = jumpTo.coerceIn(0, (layout.total - 1).coerceAtLeast(0))
                currentIndexF.snapTo(target.toFloat())
                currentIndex = target
            }
        }

        LaunchedEffect(focus.isPressing, speed, autoPageTurn) {
            if (!focus.isPressing || !autoPageTurn || layout.total == 0) return@LaunchedEffect
            while (currentIndex < layout.total - 1) {
                delay(base)
                currentIndex++
                if (layout.pauseAfter[currentIndex]) delay((base * 1.5f).toLong())
            }
        }

        LaunchedEffect(currentIndex) {
            currentIndexF.animateTo(currentIndex.toFloat(), tween(base.toInt().coerceAtLeast(60), easing = LinearEasing))
        }

        LaunchedEffect(currentIndex) {
            if (layout.total > 0) onProgress(currentIndex.toFloat() / layout.total)
        }

        val viewportHeight = listState.layoutInfo.viewportSize.height
        LaunchedEffect(currentLine, viewportHeight) {
            if (layout.total == 0 || viewportHeight == 0) return@LaunchedEffect
            val lineHeight = layout.lines[currentLine].lineHeightPx
            val offset = -(viewportHeight * 0.25f - lineHeight / 2f).toInt()
            listState.animateScrollToItem(currentLine, offset)
        }

        if (layout.total == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "无内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                userScrollEnabled = !focus.isPressing
            ) {
                items(layout.lines.size) { li ->
                    LineView(
                        line = layout.lines[li],
                        currentIndex = currentIndex,
                        currentIndexF = currentIndexF.asState(),
                        glideMs = base,
                        style = style,
                        onClick = { currentIndex = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun LineView(
    line: ReaderLine,
    currentIndex: Int,
    currentIndexF: State<Float>,
    glideMs: Long,
    style: MeteorStyle,
    onClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    val currentCharIndex = line.chars.indexOfFirst { it.index == currentIndex }
    val targetX = if (currentCharIndex >= 0) {
        line.indentPx + line.chars.take(currentCharIndex).fold(0f) { a, c -> a + c.widthPx } +
            line.chars[currentCharIndex].widthPx / 2f
    } else {
        0f
    }
    val animatedX = remember(line) { Animatable(targetX) }
    LaunchedEffect(targetX) {
        if (targetX > 0f) animatedX.animateTo(targetX, tween(glideMs.toInt().coerceAtLeast(60)))
    }

    val baseColor = MaterialTheme.colorScheme.onBackground
    val dimColor = baseColor.copy(alpha = 0.35f)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                top = with(density) { line.topSpacingPx.toDp() },
                bottom = with(density) { line.bottomSpacingPx.toDp() }
            )
            .height(with(density) { line.lineHeightPx.toDp() })
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = with(density) { line.indentPx.toDp() }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            line.chars.forEach { ch ->
                CharacterCell(
                    ch = ch,
                    fontSizeSp = line.fontSizeSp,
                    currentIndex = currentIndex,
                    currentIndexF = currentIndexF,
                    baseColor = baseColor,
                    dimColor = dimColor,
                    onClick = onClick
                )
            }
        }

        if (currentCharIndex >= 0) {
            val charWidth = line.chars[currentCharIndex].widthPx
            Canvas(Modifier.matchParentSize()) {
                drawPointerBar(animatedX.value, charWidth, line.lineHeightPx, style)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterCell(
    ch: ReaderChar,
    fontSizeSp: Float,
    currentIndex: Int,
    currentIndexF: State<Float>,
    baseColor: Color,
    dimColor: Color,
    onClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    val isRead = ch.index < currentIndex
    val isNear = abs(ch.index - currentIndex) <= 2
    val base = if (isRead) dimColor else baseColor
    val text = if (ch.char == ' ') "\u00A0" else ch.char.toString()
    Box(
        Modifier
            .width(with(density) { ch.widthPx.toDp() })
            .fillMaxHeight()
            .combinedClickable(onClick = {}, onDoubleClick = { onClick(ch.index) }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = base,
            fontSize = fontSizeSp.sp,
            lineHeight = fontSizeSp.sp,
            modifier = Modifier.graphicsLayer {
                val d = (ch.index - currentIndexF.value).coerceIn(-2f, 2f)
                val wave = (1f - abs(d) / 2f).coerceIn(0f, 1f)
                scaleX = 1f + 0.16f * wave
                scaleY = 1f + 0.16f * wave
                translationY = -2.dp.toPx() * wave
            }
        )
        if (isNear) {
            Text(
                text = text,
                color = GoldBright,
                fontSize = fontSizeSp.sp,
                lineHeight = fontSizeSp.sp,
                modifier = Modifier.graphicsLayer {
                    val d = (ch.index - currentIndexF.value).coerceIn(-2f, 2f)
                    val wave = (1f - abs(d) / 2f).coerceIn(0f, 1f)
                    alpha = wave
                    scaleX = 1f + 0.16f * wave
                    scaleY = 1f + 0.16f * wave
                    translationY = -2.dp.toPx() * wave
                }
            )
        }
    }
}

private fun DrawScope.drawPointerBar(centerX: Float, charWidth: Float, lineHeightPx: Float, style: MeteorStyle) {
    if (centerX <= 0f) return
    val cy = lineHeightPx - 4.dp.toPx()
    val barWidth = charWidth
    val barHeight = 2.4.dp.toPx()
    val left = centerX - barWidth / 2f
    val top = cy - barHeight / 2f
    drawRoundRect(
        color = style.head,
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f, barHeight / 2f)
    )
}

private fun buildLines(
    document: ReaderDocument,
    measurer: TextMeasurer,
    maxWidthPx: Float,
    density: Density
): ReaderLayout {
    val total = document.total
    if (total == 0) {
        return ReaderLayout(emptyList(), IntArray(0), BooleanArray(0), 0)
    }

    val lines = mutableListOf<ReaderLine>()
    val lineOfIndex = IntArray(total) { -1 }
    val pauseAfter = BooleanArray(total) { false }
    val widthCache = mutableMapOf<Pair<Char, Int>, Float>()
    val letterSpacing = with(density) { 1.dp.toPx() }

    fun charWidth(c: Char, level: Int): Float {
        val m = metricsFor(level)
        val base = if (isFullWidth(c)) {
            with(density) { m.fontSizeSp.sp.toPx() }
        } else {
            widthCache.getOrPut(c to level) {
                val text = if (c == ' ') "\u00A0" else c.toString()
                measurer.measure(AnnotatedString(text), TextStyle(fontSize = m.fontSizeSp.sp)).size.width.toFloat()
            }
        }
        return base + letterSpacing
    }

    val marginPx = with(density) { 24.dp.toPx() }
    val available = (maxWidthPx - marginPx * 2f).coerceAtLeast(1f)

    document.blocks.forEachIndexed { bi, block ->
        val level = block.level
        val m = metricsFor(level)
        val lineHeightPx = with(density) { m.lineHeightSp.sp.toPx() }
        val topSpacingPx = with(density) { m.topSpacingDp.dp.toPx() }
        val bottomSpacingPx = with(density) { m.bottomSpacingDp.dp.toPx() }
        val indentPx = if (block.firstLineIndent && level == 0) with(density) { (m.fontSizeSp * 2f).sp.toPx() } else 0f
        val text = block.text
        val lineStart = document.blockStarts[bi]

        var pos = 0
        var firstLine = true
        while (pos < text.length) {
            val lineWidthLimit = available - (if (firstLine) indentPx else 0f)
            var widthAccum = 0f
            var end = pos
            while (end < text.length) {
                val c = text[end]
                val w = charWidth(c, level)
                if (end > pos && widthAccum + w > lineWidthLimit) break
                widthAccum += w
                end++
            }
            if (end == pos) end = pos + 1

            val lineChars = mutableListOf<ReaderChar>()
            for (k in pos until end) {
                val c = text[k]
                val gi = lineStart + k
                val w = charWidth(c, level)
                lineChars.add(ReaderChar(c, gi, w))
                if (isPunctuation(c)) pauseAfter[gi] = true
            }
            if (end == text.length && lineChars.isNotEmpty()) {
                pauseAfter[lineChars.last().index] = true
            }

            val top = if (firstLine) topSpacingPx else 0f
            val bottom = if (end == text.length) bottomSpacingPx else 0f
            val li = lines.size
            lines.add(ReaderLine(lineChars, level, if (firstLine) indentPx else 0f, top, bottom, lineHeightPx, m.fontSizeSp))
            lineChars.forEach { lineOfIndex[it.index] = li }
            pos = end
            firstLine = false
        }
    }

    var last = 0
    for (i in lineOfIndex.indices) {
        if (lineOfIndex[i] == -1) lineOfIndex[i] = last else last = lineOfIndex[i]
    }
    return ReaderLayout(lines, lineOfIndex, pauseAfter, total)
}

private fun isFullWidth(c: Char): Boolean {
    val code = c.code
    return code in 0x4E00..0x9FFF ||
        code in 0x3400..0x4DBF ||
        code in 0xF900..0xFAFF ||
        code in 0x3040..0x30FF ||
        code in 0xAC00..0xD7AF ||
        code in 0xFF00..0xFFEF ||
        code in 0x3000..0x303F
}

private fun isPunctuation(c: Char): Boolean = c in "，。！？；：、,.!?;:…—"

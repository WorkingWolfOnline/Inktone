package com.inktone.app.ui.focus

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.inktone.app.ui.theme.Gold
import com.inktone.app.ui.theme.GoldBright
import kotlinx.coroutines.delay

class FocusState {
    var isPressing by mutableStateOf(false)
        internal set
    var pointer by mutableStateOf(Offset.Zero)
        internal set
    var seconds by mutableStateOf(0)
        internal set

    internal fun press(at: Offset) {
        isPressing = true
        pointer = at
    }

    internal fun move(at: Offset) {
        pointer = at
    }

    internal fun release() {
        isPressing = false
    }

    internal fun tick() {
        seconds += 1
    }
}

@Composable
fun rememberFocusState(): FocusState {
    val state = remember { FocusState() }
    LaunchedEffect(state.isPressing) {
        if (state.isPressing) {
            while (true) {
                delay(1000)
                state.tick()
            }
        }
    }
    return state
}

fun Modifier.focusPress(state: FocusState, enabled: Boolean = true): Modifier = pointerInput(state, enabled) {
    if (!enabled) return@pointerInput
    detectDragGesturesAfterLongPress(
        onDragStart = { state.press(it) },
        onDrag = { change, _ -> state.move(change.position) },
        onDragEnd = { state.release() },
        onDragCancel = { state.release() }
    )
}

@Composable
fun FocusGlow(state: FocusState, modifier: Modifier = Modifier) {
    if (!state.isPressing) return
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldBright.copy(alpha = 0.45f),
                        Gold.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = state.pointer,
                    radius = radius
                ),
                radius = radius,
                center = state.pointer
            )
        }
    }
}

@Composable
fun FocusLamp(isFocused: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lamp")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "lampAlpha"
    )
    val color = if (isFocused) Gold else MaterialTheme.colorScheme.onSurfaceVariant
    val a = if (isFocused) alpha else 0.5f
    Box(
        modifier
            .size(10.dp)
            .background(color.copy(alpha = a), CircleShape)
    )
}

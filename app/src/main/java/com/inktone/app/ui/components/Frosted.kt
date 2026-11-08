package com.inktone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF3F6FA)))
        )
    ) {
        Box(
            Modifier
                .offset(x = (-90).dp, y = (-70).dp)
                .size(340.dp)
                .blur(90.dp)
                .background(Color(0xFF1E5AA8).copy(alpha = 0.07f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 90.dp)
                .size(380.dp)
                .blur(100.dp)
                .background(Color(0xFF1E5AA8).copy(alpha = 0.05f), CircleShape)
        )
    }
}

@Composable
fun FrostedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    alpha: Float = 0.06f,
    borderAlpha: Float = 0.10f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = (alpha * 1.6f).coerceAtMost(0.18f)),
                        tint.copy(alpha = alpha)
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = borderAlpha), shape)
    ) {
        content()
    }
}

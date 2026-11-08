package com.inktone.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inktone.app.data.Flower
import com.inktone.app.ui.theme.GoldBright

@Composable
fun SplashScreen(onFinish: () -> Unit, flower: Flower? = null) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onFinish),
        contentAlignment = Alignment.Center
    ) {
        if (flower != null) {
            Image(
                painter = painterResource(flower.drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.25f),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.62f)))
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                text = "墨音 inktone",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "让阅读成为一种奖赏。\n将手指放在屏幕上，音乐与光晕将伴随你\n的专注。离开屏幕，世界归于宁静。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "点击任意处开始",
            style = MaterialTheme.typography.labelMedium,
            color = GoldBright,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

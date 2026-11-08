package com.inktone.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inktone.app.data.ReminderScripts
import com.inktone.app.data.SettingsStore
import com.inktone.app.ui.theme.GoldBright

@Composable
fun ReminderDialog(minutes: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val script = remember {
        val last = SettingsStore.lastScriptIndex(context)
        val candidates = ReminderScripts.scripts.indices.filter { it != last }
        val idx = if (candidates.isNotEmpty()) candidates.random()
        else ReminderScripts.scripts.indices.random()
        SettingsStore.setLastScriptIndex(context, idx)
        ReminderScripts.scripts[idx].map { it.replace("{X}", "$minutes") }
    }

    var lineIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable {
                    if (lineIndex < script.size - 1) lineIndex++ else onDismiss()
                }
        ) {
            Crossfade(targetState = lineIndex, label = "moe") { idx ->
                Image(
                    painter = painterResource(ReminderScripts.images[idx]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.78f),
                            0.25f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f)
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.78f),
                            0.25f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f)
                        )
                    )
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "墨音",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = GoldBright
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = script[lineIndex],
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Start
                    )
                }
                Text(
                    text = "点击继续 · ${lineIndex + 1}/${script.size}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp, end = 4.dp)
                )
            }
        }
    }
}

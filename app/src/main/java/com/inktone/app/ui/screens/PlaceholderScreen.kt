package com.inktone.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inktone.app.ui.components.FrostedSurface
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.navigation.AppDestination
import com.inktone.app.ui.theme.GoldBright

@Composable
fun PlaceholderScreen(
    destination: AppDestination,
    onOpenDrawer: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(title = destination.title, onOpenDrawer = onOpenDrawer)
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            FrostedSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "该功能将在后续步骤中实现",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

package com.inktone.app.ui.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Movie
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.inktone.app.data.WallpaperStore
import java.io.File
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.delay

private val videoExts = setOf("mp4", "3gp", "mkv", "webm", "mov", "avi", "ts", "m4v")

@Composable
fun WallpaperBackground(
    type: WallpaperStore.Type,
    staticPath: String?,
    dynamicKind: WallpaperStore.DynamicKind,
    modifier: Modifier = Modifier
) {
    when (type) {
        WallpaperStore.Type.STATIC -> StaticBackground(staticPath, modifier)
        WallpaperStore.Type.DYNAMIC -> when (dynamicKind) {
            WallpaperStore.DynamicKind.FLOW -> FlowBackground(modifier)
            WallpaperStore.DynamicKind.PARTICLES -> ParticlesBackground(modifier)
        }
        WallpaperStore.Type.NONE -> DefaultBackground(modifier)
    }
}

@Composable
private fun StaticBackground(path: String?, modifier: Modifier) {
    if (path == null) {
        DefaultBackground(modifier)
        return
    }
    val ext = path.substringAfterLast('.', "").lowercase()
    when {
        ext == "gif" -> GifBackground(path, modifier)
        ext in videoExts -> VideoBackground(path, modifier)
        else -> ImageBackground(path, modifier)
    }
}

@Composable
private fun DefaultBackground(modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(bg, surface))
        )
    )
}

@Composable
private fun ImageBackground(path: String, modifier: Modifier) {
    val bitmap = remember(path) { loadBitmap(path) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        DefaultBackground(modifier)
    }
}

@Composable
private fun GifBackground(path: String, modifier: Modifier) {
    val movie = remember(path) { runCatching { Movie.decodeFile(path) }.getOrNull() }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(movie) {
        while (movie != null) {
            tick++
            delay(33)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val m = movie ?: return@Canvas
        @Suppress("UNUSED_VARIABLE")
        val frame = tick
        val dur = if (m.duration() > 0) m.duration() else 1000
        m.setTime((SystemClock.uptimeMillis() % dur).toInt())
        val scale = max(size.width / m.width().toFloat(), size.height / m.height().toFloat())
        val w = m.width() * scale
        val h = m.height() * scale
        val x = (size.width - w) / 2f
        val y = (size.height - h) / 2f
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.scale(scale, scale)
            canvas.translate(x / scale, y / scale)
            m.draw(canvas.nativeCanvas, 0f, 0f)
            canvas.restore()
        }
    }
}

@Composable
private fun VideoBackground(path: String, modifier: Modifier) {
    val player = remember(path) {
        try {
            MediaPlayer().apply {
                setDataSource(path)
                isLooping = true
                setVolume(0f, 0f)
            }
        } catch (e: Exception) {
            null
        }
    }
    DisposableEffect(player) {
        onDispose { runCatching { player?.release() } }
    }
    if (player == null) {
        DefaultBackground(modifier)
        return
    }
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                        runCatching {
                            player.setSurface(Surface(surface))
                            player.setOnPreparedListener { it.start() }
                            player.setOnErrorListener { _, _, _ -> true }
                            player.prepareAsync()
                        }
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun FlowBackground(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "flow")
    val x1 by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Reverse), label = "x1"
    )
    val x2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(15000), RepeatMode.Reverse), label = "x2"
    )
    Canvas(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFBFD7F2).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * x1, h * 0.30f), radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * x1, h * 0.30f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFD6E4F8).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w * x2, h * 0.75f), radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * x2, h * 0.75f)
        )
    }
}

private data class Particle(val x: Float, val y: Float, val radius: Float, val speed: Float)

@Composable
private fun ParticlesBackground(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "particles")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Restart), label = "t"
    )
    val particles = remember {
        List(36) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 3.5f + 1.5f,
                speed = Random.nextFloat() * 0.5f + 0.3f
            )
        }
    }
    Canvas(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        particles.forEach { p ->
            val y = ((p.y - t * p.speed) % 1f + 1f) % 1f
            drawCircle(
                color = Color(0xFF8AB4E8).copy(alpha = 0.5f),
                radius = p.radius,
                center = Offset(p.x * size.width, y * size.height)
            )
        }
    }
}

private fun loadBitmap(path: String): Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, opts)
    } catch (e: Exception) {
        null
    }
}

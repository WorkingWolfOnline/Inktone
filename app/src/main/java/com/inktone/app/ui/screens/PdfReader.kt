@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.inktone.app.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReader(
    file: File,
    onProgress: (Float) -> Unit = {},
    jumpToPage: Int? = null,
    modifier: Modifier = Modifier
) {
    val path = file.absolutePath
    val renderer = remember(path) {
        try {
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(renderer) {
        onDispose { renderer?.close() }
    }

    if (renderer == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "无法打开该 PDF 文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val pageCount = renderer.pageCount
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    LaunchedEffect(pagerState.currentPage, pageCount) {
        if (pageCount > 0) onProgress(pagerState.currentPage.toFloat() / pageCount)
    }

    LaunchedEffect(jumpToPage) {
        if (jumpToPage != null && jumpToPage in 0 until pageCount) {
            pagerState.animateScrollToPage(jumpToPage)
        }
    }

    Column(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            beyondBoundsPageCount = 0
        ) { page ->
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = rememberPdfPage(renderer, page, constraints.maxWidth)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "第 ${page + 1} 页",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${pagerState.currentPage + 1} / $pageCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun rememberPdfPage(renderer: PdfRenderer, index: Int, maxWidthPx: Int): Bitmap? {
    val state = produceState<Bitmap?>(initialValue = null, index, maxWidthPx) {
        val bmp = withContext(Dispatchers.Default) { renderPage(renderer, index, maxWidthPx) }
        value = bmp
        awaitDispose { value?.recycle() }
    }
    return state.value
}

private fun renderPage(renderer: PdfRenderer, index: Int, maxWidthPx: Int): Bitmap? {
    if (index < 0 || index >= renderer.pageCount || maxWidthPx <= 0) return null
    return synchronized(renderer) {
        try {
            val page = renderer.openPage(index)
            try {
                val scale = maxWidthPx / page.width.toFloat()
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        } catch (e: Exception) {
            null
        }
    }
}

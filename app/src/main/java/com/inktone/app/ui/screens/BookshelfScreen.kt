@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.inktone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inktone.app.data.model.Book
import com.inktone.app.data.model.BookFormat
import com.inktone.app.ui.components.FrostedSurface
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.theme.Gold
import com.inktone.app.ui.theme.GoldBright

private val coverPalettes = listOf(
    listOf(Color(0xFF3A352B), Color(0xFF6B5A2E)),
    listOf(Color(0xFF2A2E38), Color(0xFF4A5568)),
    listOf(Color(0xFF33222A), Color(0xFF6E3B4E)),
    listOf(Color(0xFF1F2E2B), Color(0xFF3C5A4B)),
    listOf(Color(0xFF2B2633), Color(0xFF5A4A72))
)

private enum class SortMode(val label: String) {
    RECENT_READ("最近阅读"),
    RECENT_IMPORT("最近导入"),
    TITLE("书名")
}

@Composable
fun BookshelfScreen(
    books: List<Book>,
    onOpenDrawer: () -> Unit,
    onImport: () -> Unit,
    onOpenBook: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortMode.RECENT_READ) }
    var category by remember { mutableStateOf<BookFormat?>(null) }
    var pendingDelete by remember { mutableStateOf<Book?>(null) }

    val visibleBooks = remember(books, query, sort, category) {
        val filtered = books
            .filter { category == null || it.format == category }
            .filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
        when (sort) {
            SortMode.RECENT_READ -> filtered.sortedByDescending { it.lastReadAt }
            SortMode.RECENT_IMPORT -> filtered
            SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassTopBar(
            title = "墨音",
            subtitle = "书架",
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(onClick = onImport) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "导入文件",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        if (books.isEmpty()) {
            EmptyBookshelf(onImport = onImport)
        } else {
            Column(Modifier.fillMaxSize()) {
                BookshelfControls(
                    query = query,
                    onQueryChange = { query = it },
                    sort = sort,
                    onSortChange = { sort = it },
                    category = category,
                    onCategoryChange = { category = it }
                )

                BookshelfGrid(
                    books = visibleBooks,
                    onOpenBook = onOpenBook,
                    onLongPress = { pendingDelete = it }
                )
            }
        }
    }

    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除书籍") },
            text = { Text("确定从书架删除《${book.title}》吗？本地文件也将被移除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(book.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BookshelfControls(
    query: String,
    onQueryChange: (String) -> Unit,
    sort: SortMode,
    onSortChange: (SortMode) -> Unit,
    category: BookFormat?,
    onCategoryChange: (BookFormat?) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("搜索书名") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortMode.values().forEach { mode ->
                Chip(text = mode.label, selected = sort == mode) { onSortChange(mode) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(text = "全部", selected = category == null) { onCategoryChange(null) }
            listOf(BookFormat.PDF, BookFormat.DOCX, BookFormat.TXT, BookFormat.EPUB).forEach { f ->
                Chip(text = f.name, selected = category == f) { onCategoryChange(f) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun BookshelfGrid(
    books: List<Book>,
    onOpenBook: (String) -> Unit,
    onLongPress: (Book) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(book = book, onClick = { onOpenBook(book.id) }, onLongPress = { onLongPress(book) })
        }
    }
}

@Composable
private fun BookCard(book: Book, onClick: () -> Unit, onLongPress: () -> Unit) {
    val palette = coverPalettes[Math.abs(book.id.hashCode()) % coverPalettes.size]
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(palette))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = book.format.name,
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f))
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (book.progress > 0f) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Gold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyBookshelf(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrostedSurface(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "书架空空如也",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "导入一本 PDF、Word、TXT 或 EPUB，\n开始你的沉浸式阅读。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = onImport,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("导入文件", style = MaterialTheme.typography.labelLarge)
        }
    }
}

package com.inktone.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.inktone.app.R
import com.inktone.app.data.CompassMod
import com.inktone.app.data.CompassRepository
import com.inktone.app.data.model.Direction
import com.inktone.app.data.model.Photo
import com.inktone.app.data.model.directionOf
import com.inktone.app.sensor.rememberCompassDegree
import com.inktone.app.ui.components.GlassTopBar
import com.inktone.app.ui.theme.Gold
import com.inktone.app.ui.theme.GoldBright
import com.inktone.app.ui.theme.GoldDim
import com.inktone.app.ui.theme.Ink900
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DialCenter = Color(0xFF5C4C28)
private val DialEdge = Color(0xFF15110B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val degree = rememberCompassDegree()
    val initialFloors = remember { CompassRepository.loadFloors(context) }
    var floors by remember { mutableStateOf(initialFloors) }
    var currentFloorName by remember {
        mutableStateOf(CompassRepository.currentFloorName(context) ?: initialFloors.firstOrNull()?.name)
    }
    var photos by remember {
        val fn = CompassRepository.currentFloorName(context) ?: initialFloors.firstOrNull()?.name
        mutableStateOf(fn?.let { CompassRepository.loadPhotos(context, it) } ?: emptyList())
    }
    var openFolder by remember { mutableStateOf<Direction?>(null) }
    var viewerPhoto by remember { mutableStateOf<Photo?>(null) }
    var showFloorSheet by remember { mutableStateOf(false) }
    var showNewFloorDialog by remember { mutableStateOf(false) }
    var newFloorName by remember { mutableStateOf("") }
    var shotDegree by remember { mutableStateOf(0f) }
    var dialFile by remember { mutableStateOf(CompassMod.dialFile(context)) }
    var needleFile by remember { mutableStateOf(CompassMod.needleFile(context)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { CompassMod.ensureDefaults(context) }
        dialFile = CompassMod.dialFile(context)
        needleFile = CompassMod.needleFile(context)
    }

    val tempFile = remember { File.createTempFile("photo", ".jpg", context.cacheDir) }
    val tempUri = remember {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", tempFile)
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val floorName = currentFloorName
        if (success && floorName != null) {
            val photo = CompassRepository.addPhoto(context, floorName, directionOf(shotDegree), tempFile)
            photos = CompassRepository.loadPhotos(context, floorName)
            viewerPhoto = photo
        }
    }

    val currentFloor = floors.firstOrNull { it.name == currentFloorName }
    val photoFile = { p: Photo -> CompassRepository.photoFile(context, p) }

    if (openFolder != null) {
        DirectionFolderScreen(
            direction = openFolder!!,
            photos = photos.filter { it.direction == openFolder },
            photoFile = photoFile,
            onBack = { openFolder = null },
            onOpenPhoto = { viewerPhoto = it }
        )
    } else {
        CompassMain(
            degree = degree,
            floorName = currentFloor?.name ?: "未选择楼层",
            photos = photos,
            dialFile = dialFile,
            needleFile = needleFile,
            onOpenDrawer = onOpenDrawer,
            onTakePhoto = {
                shotDegree = degree
                takePicture.launch(tempUri)
            },
            onSwitchFloor = { showFloorSheet = true },
            onOpenFolder = { openFolder = it }
        )
    }

    if (showFloorSheet) {
        ModalBottomSheet(onDismissRequest = { showFloorSheet = false }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "切换楼层",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    showFloorSheet = false
                    showNewFloorDialog = true
                }) { Text("新建", color = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.padding(bottom = 32.dp)) {
                if (floors.isEmpty()) {
                    Text(
                        "暂无楼层，请点右上角新建",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                floors.forEach { f ->
                    val selected = f.name == currentFloorName
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentFloorName = f.name
                                CompassRepository.setCurrentFloor(context, f.name)
                                photos = CompassRepository.loadPhotos(context, f.name)
                                showFloorSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            f.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }

    if (showNewFloorDialog) {
        AlertDialog(
            onDismissRequest = { showNewFloorDialog = false },
            title = { Text("新建楼层") },
            text = {
                OutlinedTextField(
                    value = newFloorName,
                    onValueChange = { newFloorName = it },
                    label = { Text("楼层名称，如 3楼") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newFloorName.isNotBlank(),
                    onClick = {
                        CompassRepository.addFloor(context, newFloorName.trim())
                        floors = CompassRepository.loadFloors(context)
                        currentFloorName = CompassRepository.currentFloorName(context)
                        photos = currentFloorName?.let { CompassRepository.loadPhotos(context, it) } ?: emptyList()
                        newFloorName = ""
                        showNewFloorDialog = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFloorDialog = false }) { Text("取消") }
            }
        )
    }

    viewerPhoto?.let { photo ->
        PhotoViewerDialog(
            photo = photo,
            file = photoFile(photo),
            onRename = { name ->
                CompassRepository.renamePhoto(context, photo, name)
                photos = currentFloorName?.let { CompassRepository.loadPhotos(context, it) } ?: emptyList()
            },
            onDismiss = { viewerPhoto = null }
        )
    }
}

@Composable
private fun CompassMain(
    degree: Float,
    floorName: String,
    photos: List<Photo>,
    dialFile: File?,
    needleFile: File?,
    onOpenDrawer: () -> Unit,
    onTakePhoto: () -> Unit,
    onSwitchFloor: () -> Unit,
    onOpenFolder: (Direction) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        GlassTopBar(title = "指南针", subtitle = floorName, onOpenDrawer = onOpenDrawer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Spacer(Modifier.size(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CompassDial(degree = degree, modifier = Modifier.size(330.dp), dialFile = dialFile, needleFile = needleFile)
        }

        Text(
            text = "方位 ${degree.roundToInt()}° · ${directionOf(degree).label}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onTakePhoto),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "拍照", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }

            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onSwitchFloor)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("切换楼层", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Direction.entries.forEach { d ->
                val count = photos.count { it.direction == d }
                FolderCard(label = d.label, count = count, onClick = { onOpenFolder(d) })
                Spacer(Modifier.size(10.dp))
            }
        }
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun FolderCard(label: String, count: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("$label 方向", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("$count 张照片", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DirectionFolderScreen(
    direction: Direction,
    photos: List<Photo>,
    photoFile: (Photo) -> File,
    onBack: () -> Unit,
    onOpenPhoto: (Photo) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "${direction.label}方向照片",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.size(12.dp))
        PhotoWheel(
            photos = photos,
            photoFile = photoFile,
            onOpen = onOpenPhoto
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CompassDial(degree: Float, modifier: Modifier, dialFile: File?, needleFile: File?) {
    val dialBitmap = remember(dialFile?.absolutePath) { dialFile?.let { loadBitmap(it) } }
    val needleBitmap = remember(needleFile?.absolutePath) { needleFile?.let { loadBitmap(it) } }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (dialBitmap != null) {
            Image(
                bitmap = dialBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .rotate(-degree)
            )
        }
        if (needleBitmap != null) {
            Image(
                bitmap = needleBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
            )
        }
    }
}

private fun loadBitmap(file: File): Bitmap? {
    if (!file.exists()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1200) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    } catch (e: Exception) {
        null
    }
}

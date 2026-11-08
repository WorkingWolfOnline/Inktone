package com.inktone.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.inktone.app.audio.MusicPlayer
import com.inktone.app.data.BookRepository
import com.inktone.app.data.FlowerCatalog
import com.inktone.app.data.FlowerStore
import com.inktone.app.data.MusicPrefs
import com.inktone.app.data.MusicRepository
import com.inktone.app.data.SettingsStore
import com.inktone.app.data.SplashStore
import com.inktone.app.data.ThemeStore
import com.inktone.app.data.model.Book
import com.inktone.app.data.WallpaperRepository
import com.inktone.app.data.WallpaperStore
import com.inktone.app.ui.components.SideDrawer
import com.inktone.app.ui.navigation.AppDestination
import com.inktone.app.ui.screens.BookshelfScreen
import com.inktone.app.ui.screens.CompassScreen
import com.inktone.app.ui.screens.FlowerCollectionScreen
import com.inktone.app.ui.screens.KnowledgeScreen
import com.inktone.app.ui.screens.MusicLibraryScreen
import com.inktone.app.ui.screens.PlaceholderScreen
import com.inktone.app.ui.screens.PuzzleScreen
import com.inktone.app.ui.screens.ReaderScreen
import com.inktone.app.ui.screens.SettingsScreen
import com.inktone.app.ui.screens.SplashScreen
import com.inktone.app.ui.screens.WallpaperScreen
import com.inktone.app.ui.theme.AppThemes
import com.inktone.app.ui.theme.InktoneTheme
import com.inktone.app.ui.wallpaper.WallpaperBackground
import kotlinx.coroutines.launch

private val importMimeTypes = arrayOf(
    "application/pdf",
    "application/epub+zip",
    "text/plain",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)

@Composable
fun InktoneApp() {
    val context = LocalContext.current
    var themeIndex by remember { mutableStateOf(ThemeStore.index(context)) }
    var darkMode by remember { mutableStateOf(ThemeStore.darkMode(context)) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var current by rememberSaveable { mutableStateOf(AppDestination.Bookshelf) }
    var openBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf(BookRepository.loadAll(context)) }
    var tracks by remember { mutableStateOf(MusicRepository.loadAll(context)) }
    var selectedTrackId by remember { mutableStateOf(MusicPrefs.selectedId(context)) }
    var volume by remember { mutableStateOf(MusicPrefs.volume(context)) }
    var wallpaperType by remember { mutableStateOf(WallpaperStore.type(context)) }
    var staticPath by remember { mutableStateOf(WallpaperStore.staticPath(context)) }
    var dynamicKind by remember { mutableStateOf(WallpaperStore.dynamicKind(context)) }
    var unlockedFlowers by remember { mutableStateOf(FlowerStore.unlocked(context)) }
    var splashFlowerOrder by remember { mutableStateOf(SplashStore.flowerOrder(context)) }
    var drawerLocked by remember { mutableStateOf(SettingsStore.drawerLocked(context)) }

    LaunchedEffect(Unit) {
        MusicPlayer.setVolume(MusicPrefs.volume(context))
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val path = WallpaperRepository.import(context, uri)
            staticPath = path
            WallpaperStore.setStaticPath(context, path)
            wallpaperType = WallpaperStore.Type.STATIC
            WallpaperStore.setType(context, WallpaperStore.Type.STATIC)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            BookRepository.import(context, uri)
            books = BookRepository.loadAll(context)
        }
    }

    val musicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            MusicRepository.import(context, uri)
            tracks = MusicRepository.loadAll(context)
        }
    }

    val openImport: () -> Unit = { importLauncher.launch(importMimeTypes) }
    val openMusicImport: () -> Unit = { musicLauncher.launch(arrayOf("audio/*")) }

    val openBook: Book? = books.firstOrNull { it.id == openBookId }

    Crossfade(targetState = darkTheme, animationSpec = tween(350), label = "theme") { dark ->
        InktoneTheme(theme = AppThemes[themeIndex], darkTheme = dark) {
            if (showSplash) {
                SplashScreen(
                    onFinish = { showSplash = false },
                    flower = SplashStore.flowerOrder(context)?.let { FlowerCatalog.byId(it) }
                )
            } else {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = openBook == null || !drawerLocked,
        drawerContent = {
            SideDrawer(
                current = current,
                onSelect = { dest ->
                    if (dest == AppDestination.Import) {
                        scope.launch { drawerState.close() }
                        openImport()
                    } else {
                        current = dest
                        scope.launch { drawerState.close() }
                    }
                },
                flowerUnlocked = unlockedFlowers.size,
                flowerTotal = FlowerCatalog.all.size
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperBackground(type = wallpaperType, staticPath = staticPath, dynamicKind = dynamicKind, modifier = Modifier.fillMaxSize())
            when {
                openBook != null -> ReaderScreen(
                    book = openBook,
                    onBack = {
                        openBookId = null
                        books = BookRepository.loadAll(context)
                        unlockedFlowers = FlowerStore.unlocked(context)
                    },
                    drawerLocked = drawerLocked,
                    onToggleLock = {
                        drawerLocked = !drawerLocked
                        SettingsStore.setDrawerLocked(context, drawerLocked)
                    }
                )
                current == AppDestination.Bookshelf -> BookshelfScreen(
                    books = books,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onImport = openImport,
                    onOpenBook = { openBookId = it },
                    onDelete = { id ->
                        val book = books.firstOrNull { it.id == id }
                        if (book != null) BookRepository.delete(context, book)
                        books = BookRepository.loadAll(context)
                    }
                )
                current == AppDestination.FlowerCollection -> FlowerCollectionScreen(
                    unlocked = unlockedFlowers,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    splashFlowerOrder = splashFlowerOrder,
                    onSetSplashFlower = { order ->
                        splashFlowerOrder = order
                        SplashStore.setFlowerOrder(context, order)
                    }
                )
                current == AppDestination.MusicLibrary -> MusicLibraryScreen(
                    tracks = tracks,
                    selectedId = selectedTrackId,
                    volume = volume,
                    onImport = openMusicImport,
                    onSelect = { id ->
                        selectedTrackId = id
                        MusicPrefs.setSelected(context, id)
                    },
                    onDelete = { id ->
                        val track = tracks.firstOrNull { it.id == id }
                        if (track != null) MusicRepository.delete(context, track)
                        tracks = MusicRepository.loadAll(context)
                        if (selectedTrackId == id) {
                            selectedTrackId = null
                            MusicPrefs.setSelected(context, null)
                            MusicPlayer.stop()
                        }
                    },
                    onVolumeChange = { v ->
                        volume = v
                        MusicPlayer.setVolume(v)
                        MusicPrefs.setVolume(context, v)
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                current == AppDestination.Compass -> CompassScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                current == AppDestination.Wallpaper -> WallpaperScreen(
                    type = wallpaperType,
                    dynamicKind = dynamicKind,
                    themeIndex = themeIndex,
                    onThemeChange = { i ->
                        themeIndex = i
                        ThemeStore.setIndex(context, i)
                    },
                    onSelectType = { t ->
                        wallpaperType = t
                        WallpaperStore.setType(context, t)
                    },
                    onSelectDynamic = { k ->
                        dynamicKind = k
                        WallpaperStore.setDynamicKind(context, k)
                    },
                    onImportStatic = { wallpaperLauncher.launch(arrayOf("image/*", "video/*")) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                current == AppDestination.Puzzle -> PuzzleScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                current == AppDestination.Knowledge -> KnowledgeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                current == AppDestination.Settings -> SettingsScreen(
                    themeIndex = themeIndex,
                    onThemeChange = { i ->
                        themeIndex = i
                        ThemeStore.setIndex(context, i)
                    },
                    darkMode = darkMode,
                    onDarkModeChange = { mode ->
                        darkMode = mode
                        ThemeStore.setDarkMode(context, mode)
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                else -> PlaceholderScreen(
                    destination = current,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
            }
        }
    }
}

package dev.jj.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF111217)
private val Panel = Color(0xFF191B20)
private val Muted = Color(0xFFA7AAB5)
private val White = Color(0xFFF8F8FA)
private val SoftBorder = Color(0xFF30333C)

private val jjColors = darkColorScheme(
    primary = White,
    onPrimary = Ink,
    background = Ink,
    onBackground = White,
    surface = Panel,
    onSurface = White,
    onSurfaceVariant = Muted,
    outline = SoftBorder,
)

@Composable
fun JJLauncherApp(
    repository: AppRepository,
    onOpenApp: (InstalledApp) -> Unit,
    onRequestDefault: () -> Unit,
    isDefaultLauncher: () -> Boolean,
    onOpenReleases: () -> Unit,
) {
    val context = LocalContext.current
    MaterialTheme(colorScheme = jjColors) {
        var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        var favorites by remember { mutableStateOf<List<String>>(emptyList()) }
        var loadError by remember { mutableStateOf(false) }
        var drawerOpen by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        var settingsOpen by remember { mutableStateOf(false) }
        var isHomeDefault by remember { mutableStateOf(isDefaultLauncher()) }
        var refreshToken by remember { mutableStateOf(0) }
        val lifecycle = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()

        DisposableEffect(lifecycle, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isHomeDefault = isDefaultLauncher()
                    refreshToken++
                }
            }
            val packageReceiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {
                    repository.invalidateInstalledApps()
                    refreshToken++
                }
            }
            val packages = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            lifecycle.lifecycle.addObserver(observer)
            ContextCompat.registerReceiver(
                context, packageReceiver, packages, ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                lifecycle.lifecycle.removeObserver(observer)
                context.unregisterReceiver(packageReceiver)
            }
        }
        LaunchedEffect(refreshToken) {
            try {
                val loaded = withContext(Dispatchers.IO) { repository.loadInstalledApps() }
                apps = loaded
                favorites = repository.getFavorites(loaded)
                loadError = false
            } catch (_: Exception) {
                loadError = true
            }
        }
        val favoritesForDock = remember(apps, favorites) {
            val indexed = apps.associateBy { it.key }
            favorites.mapNotNull(indexed::get)
        }
        val toggleFavorite: (InstalledApp) -> Unit = { app ->
            val next = when {
                app.key in favorites -> favorites - app.key
                favorites.size < AppRepository.MAX_FAVORITES -> favorites + app.key
                else -> favorites
            }
            if (next == favorites) {
                Toast.makeText(context, "O dock aceita até 5 apps", Toast.LENGTH_SHORT).show()
            } else {
                favorites = next
                scope.launch(Dispatchers.IO) { repository.saveFavorites(next) }
            }
        }

        BackHandler(drawerOpen || settingsOpen) {
            when {
                settingsOpen -> settingsOpen = false
                else -> { drawerOpen = false; query = "" }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)
            var isDragging by remember { mutableStateOf(false) }
            var dragProgress by remember { mutableFloatStateOf(0f) }
            val animatedProgress by animateFloatAsState(
                targetValue = if (drawerOpen) 1f else 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "Deslizar gaveta",
            )
            val drawerProgress = if (isDragging) dragProgress else animatedProgress

            HomeScreen(
                favorites = favoritesForDock,
                isHomeDefault = isHomeDefault,
                onRequestDefault = onRequestDefault,
                onOpenApp = onOpenApp,
                onOpenDrawer = { drawerOpen = true },
                onToggleFavorite = toggleFavorite,
                onSwipeStart = {
                    dragProgress = animatedProgress
                    isDragging = true
                },
                onSwipeDelta = { delta ->
                    dragProgress = (dragProgress - delta / heightPx).coerceIn(0f, 1f)
                },
                onSwipeEnd = {
                    drawerOpen = dragProgress > 0.10f
                    isDragging = false
                },
                onSwipeCancel = { isDragging = false },
            )

            // Offset is updated without re-rasterizing icons. The drawer follows the finger
            // on the home screen; when released it settles into place in 320 ms.
            Box(
                Modifier.fillMaxSize().offset {
                    IntOffset(0, ((1f - drawerProgress) * heightPx).roundToInt())
                },
            ) {
                AppDrawer(
                    apps = apps,
                    favorites = favorites,
                    query = query,
                    onQueryChange = { query = it },
                    onOpenApp = { app ->
                        drawerOpen = false
                        query = ""
                        onOpenApp(app)
                    },
                    onToggleFavorite = toggleFavorite,
                    onClose = { drawerOpen = false; query = "" },
                    onSettings = { settingsOpen = true },
                    onRefresh = { repository.invalidateInstalledApps(); refreshToken++ },
                    error = loadError,
                )
            }
        }

        if (settingsOpen) {
            AlertDialog(
                onDismissRequest = { settingsOpen = false },
                containerColor = Panel,
                title = { StyledText("JJ Launcher", 21.sp, White, FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StyledText("Configurações", 13.sp, Muted)
                        TextButton(onClick = {
                            settingsOpen = false
                            onRequestDefault()
                        }) { StyledText("Definir como tela inicial", 15.sp, White) }
                        TextButton(onClick = {
                            settingsOpen = false
                            onOpenReleases()
                        }) { StyledText("Atualizações", 15.sp, White) }
                        StyledText("Versão experimental 0.2.0", 12.sp, Muted)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { settingsOpen = false }) {
                        StyledText("Concluído", 14.sp, White)
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    favorites: List<InstalledApp>,
    isHomeDefault: Boolean,
    onRequestDefault: () -> Unit,
    onOpenApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeDelta: (Float) -> Unit,
    onSwipeEnd: () -> Unit,
    onSwipeCancel: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.16f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onSwipeStart() },
                    onVerticalDrag = { change, amount ->
                        onSwipeDelta(amount)
                        change.consume()
                    },
                    onDragEnd = onSwipeEnd,
                    onDragCancel = onSwipeCancel,
                )
            },
    ) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(64.dp))
            StyledText(
                text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 48.sp,
                color = White,
                weight = FontWeight.Light,
                letterSpacing = (-1.5).sp,
            )
            Spacer(Modifier.height(5.dp))
            StyledText(
                text = now.format(DateTimeFormatter.ofPattern(
                    "EEEE, d 'de' MMMM", Locale("pt", "BR"),
                )),
                fontSize = 14.sp,
                color = White.copy(alpha = .83f),
            )

            Spacer(Modifier.weight(1f))

            if (!isHomeDefault) {
                Surface(
                    color = Color(0xFF242832).copy(alpha = .88f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            StyledText("Usar JJ Launcher", 14.sp, White, FontWeight.Medium)
                            StyledText("Defina como tela inicial", 11.sp, Muted)
                        }
                        TextButton(onClick = onRequestDefault) {
                            StyledText("Ativar", 13.sp, White, FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (favorites.isNotEmpty()) {
                Surface(
                    color = Color(0xFF1A1D25).copy(alpha = .69f),
                    shape = RoundedCornerShape(27.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        favorites.forEach { app ->
                            AppTile(
                                app = app,
                                iconSize = 51.dp,
                                showLabel = false,
                                onClick = { onOpenApp(app) },
                                onLongClick = { onToggleFavorite(app) },
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onOpenDrawer,
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 5.dp, bottom = 13.dp)
                    .semantics { contentDescription = "Abrir lista de aplicativos" },
            ) {
                StyledText("⌃  Aplicativos", 11.sp, White.copy(alpha = .75f))
            }
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<InstalledApp>,
    favorites: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenApp: (InstalledApp) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    error: Boolean,
) {
    val filtered = remember(apps, query) { apps.filter { appMatchesSearch(it.label, query) } }
    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 34.dp, height = 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SoftBorder)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyledText(
                    "Aplicativos", 25.sp, White, FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSettings) {
                    StyledText("Opções", 13.sp, Muted, FontWeight.Medium)
                }
                TextButton(onClick = onClose) {
                    StyledText("Fechar", 13.sp, Muted)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { StyledText("Pesquisar aplicativos", 14.sp, Muted) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    color = White,
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = SoftBorder,
                    focusedBorderColor = Color(0xFF9199AC),
                    unfocusedContainerColor = Panel,
                    focusedContainerColor = Panel,
                ),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
            when {
                error -> {
                    StyledText(
                        "Não foi possível carregar os aplicativos.",
                        14.sp, Muted,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                    TextButton(onClick = onRefresh) {
                        StyledText("Tentar novamente", 14.sp, White)
                    }
                }
                filtered.isEmpty() -> {
                    StyledText(
                        if (apps.isEmpty()) "Carregando aplicativos…" else "Nenhum resultado",
                        14.sp, Muted,
                        modifier = Modifier.fillMaxWidth().padding(top = 45.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 74.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 25.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    items(filtered, key = { it.key }, contentType = { "app" }) { app ->
                        AppTile(
                            app = app,
                            iconSize = 54.dp,
                            showLabel = true,
                            onClick = { onOpenApp(app) },
                            onLongClick = { onToggleFavorite(app) },
                            favorite = app.key in favorites,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: InstalledApp,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    favorite: Boolean = false,
) {
    Column(
        modifier = Modifier.widthIn(min = iconSize)
            .combinedClickable(
                onClickLabel = "Abrir ${app.label}",
                onLongClickLabel = "Adicionar ou remover dos favoritos",
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(iconSize).clip(RoundedCornerShape(16.dp)),
        )
        if (showLabel) {
            StyledText(
                text = app.label,
                fontSize = 11.sp,
                color = if (favorite) White else White.copy(alpha = .83f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StyledText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        textAlign = textAlign,
        overflow = overflow,
    )
}

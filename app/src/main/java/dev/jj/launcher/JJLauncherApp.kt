package dev.jj.launcher

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DeepBackground = Color(0xFF17191F)
private val SubtleText = Color(0xFFCACBD1)

@Composable
fun JJLauncherApp(
    repository: AppRepository,
    onOpenApp: (InstalledApp) -> Unit,
    onRequestDefault: () -> Unit,
    onOpenReleases: () -> Unit,
) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors) {
        var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        var favorites by remember { mutableStateOf<List<String>>(emptyList()) }
        var loadError by remember { mutableStateOf(false) }
        var drawerOpen by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        var refreshToken by remember { mutableStateOf(0) }
        val lifecycle = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()

        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshToken += 1
            }
            lifecycle.lifecycle.addObserver(observer)
            onDispose { lifecycle.lifecycle.removeObserver(observer) }
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

        val favoriteApps = remember(apps, favorites) {
            favorites.mapNotNull { key -> apps.firstOrNull { it.key == key } }
        }
        val toggleFavorite: (InstalledApp) -> Unit = { app ->
            val selected = if (app.key in favorites) {
                favorites - app.key
            } else if (favorites.size < AppRepository.MAX_FAVORITES) {
                favorites + app.key
            } else {
                favorites
            }
            if (selected == favorites) {
                Toast.makeText(context, "O dock aceita até 5 apps", Toast.LENGTH_SHORT).show()
            } else {
                favorites = selected
                scope.launch(Dispatchers.IO) { repository.saveFavorites(selected) }
                Toast.makeText(
                    context,
                    if (app.key in selected) "Adicionado aos favoritos" else "Removido dos favoritos",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        BackHandler(drawerOpen) {
            drawerOpen = false
            query = ""
        }
        AnimatedContent(
            targetState = drawerOpen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "Transição entre início e aplicativos",
        ) { showDrawer ->
            if (showDrawer) {
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
                    onRequestDefault = onRequestDefault,
                    onOpenReleases = onOpenReleases,
                    onRefresh = { refreshToken += 1 },
                    error = loadError,
                )
            } else {
                HomeScreen(
                    favorites = favoriteApps,
                    onOpenApp = onOpenApp,
                    onOpenDrawer = { drawerOpen = true },
                    onToggleFavorite = toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    favorites: List<InstalledApp>,
    onOpenApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(30_000)
        }
    }
    var swipeDistance by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { swipeDistance = 0f },
                    onVerticalDrag = { change, amount ->
                        swipeDistance += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (swipeDistance < -80f) onOpenDrawer()
                        swipeDistance = 0f
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(52.dp))
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.4).sp,
            )
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))),
                color = SubtleText,
                fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            if (favorites.isNotEmpty()) {
                Surface(
                    color = DeepBackground.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        favorites.forEach { app ->
                            AppTile(
                                app = app,
                                size = 48.dp,
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
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 24.dp),
            ) {
                Text("⌃  Aplicativos", color = Color.White, fontSize = 13.sp)
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
    onRequestDefault: () -> Unit,
    onRefresh: () -> Unit,
    error: Boolean,
) {
    val filtered = remember(apps, query) { apps.filter { appMatchesSearch(it.label, query) } }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Aplicativos", modifier = Modifier.weight(1f), fontSize = 25.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = onClose) { Text("Fechar") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar aplicativos") },
                leadingIcon = { Text("⌕", fontSize = 26.sp) },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
            Text(
                "Segure um aplicativo para adicionar ou remover do dock",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRequestDefault) {
                    Text("Padrão", fontSize = 12.sp)
                }
                TextButton(onClick = onOpenReleases) {
                    Text("Atualizações", fontSize = 12.sp)
                }
            }
            when {
                error -> {
                    Text("Não foi possível carregar os aplicativos.", modifier = Modifier.padding(top = 24.dp))
                    TextButton(onClick = onRefresh) { Text("Tentar novamente") }
                }
                filtered.isEmpty() -> Text(
                    if (apps.isEmpty()) "Carregando aplicativos…" else "Nenhum aplicativo encontrado",
                    modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(filtered, key = { it.key }) { app ->
                        AppTile(
                            app = app,
                            size = 54.dp,
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
    size: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    favorite: Boolean = false,
) {
    val bitmap = remember(app.key) { app.icon.toBitmap(width = 144, height = 144).asImageBitmap() }
    Column(
        modifier = Modifier.widthIn(min = size)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(size).clip(RoundedCornerShape(16.dp)),
        )
        if (showLabel) {
            Text(
                text = if (favorite) "• ${app.label}" else app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

package dev.jj.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap,
) {
    val key: String get() = component.flattenToString()
}

/**
 * App icons are rasterized off the main thread and retained between resumes.
 * Call loadInstalledApps() from Dispatchers.IO. Invalidate when packages change.
 */
class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val preferences = context.getSharedPreferences("jj_launcher", Context.MODE_PRIVATE)

    @Volatile private var cachedApps: List<InstalledApp>? = null

    fun invalidateInstalledApps() {
        cachedApps = null
    }

    fun loadInstalledApps(): List<InstalledApp> = synchronized(this) {
        cachedApps?.let { return@synchronized it }

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val iconSize = (56 * context.resources.displayMetrics.density)
            .toInt().coerceIn(96, 192)

        results.mapNotNull { info ->
            runCatching {
                val activity = info.activityInfo
                if (activity.packageName == context.packageName) return@runCatching null
                InstalledApp(
                    label = info.loadLabel(packageManager).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                    icon = info.loadIcon(packageManager)
                        .toBitmap(width = iconSize, height = iconSize)
                        .asImageBitmap(),
                )
            }.getOrNull()
        }.distinctBy { it.key }
            .sortedWith(compareBy<InstalledApp> { normalizeForSearch(it.label) }.thenBy { it.key })
            .also { cachedApps = it }
    }

    fun getFavorites(apps: List<InstalledApp>): List<String> {
        val raw = preferences.getString(FAVORITES, null)
        if (raw == null) return initialFavorites(apps)
        val available = apps.mapTo(mutableSetOf()) { it.key }
        return raw.split('|').filter { it in available }.distinct().take(MAX_FAVORITES)
    }

    fun saveFavorites(keys: List<String>) {
        preferences.edit().putString(FAVORITES, keys.distinct().take(MAX_FAVORITES).joinToString("|")).apply()
    }

    private fun initialFavorites(apps: List<InstalledApp>): List<String> {
        val suggestions = listOf(
            "telefone", "phone", "mensagens", "messages",
            "camera", "câmera", "browser", "navegador", "chrome",
        ).mapNotNull { target ->
            apps.firstOrNull { normalizeForSearch(it.label).contains(normalizeForSearch(target)) }
        }
        return (suggestions + apps).distinctBy { it.key }
            .take(MAX_FAVORITES).map { it.key }
    }

    companion object {
        const val MAX_FAVORITES = 5
        private const val FAVORITES = "favorites"
    }
}

package dev.jj.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

data class InstalledApp(
    val label: String,
    val component: ComponentName,
    val icon: Drawable,
) {
    val key: String get() = component.flattenToString()
}

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val preferences = context.getSharedPreferences("jj_launcher", Context.MODE_PRIVATE)

    fun loadInstalledApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        return results.mapNotNull { info ->
            runCatching {
                val activity = info.activityInfo
                if (activity.packageName == context.packageName) return@runCatching null
                InstalledApp(
                    label = info.loadLabel(packageManager).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                    icon = info.loadIcon(packageManager),
                )
            }.getOrNull()
        }.distinctBy { it.key }
            .sortedWith(compareBy<InstalledApp> { normalizeForSearch(it.label) }.thenBy { it.key })
    }

    /** Favorites are persisted in order; removed/uninstalled apps are not displayed. */
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
        val suggestions = listOf("telefone", "phone", "mensagens", "messages", "camera", "câmera", "browser", "navegador", "chrome")
            .mapNotNull { target -> apps.firstOrNull { normalizeForSearch(it.label).contains(normalizeForSearch(target)) } }
        return (suggestions + apps).distinctBy { it.key }.take(MAX_FAVORITES).map { it.key }
    }

    companion object {
        const val MAX_FAVORITES = 5
        private const val FAVORITES = "favorites"
    }
}

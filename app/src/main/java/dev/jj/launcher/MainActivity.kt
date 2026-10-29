package dev.jj.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        val repository = AppRepository(applicationContext)
        setContent {
            JJLauncherApp(
                repository = repository,
                onOpenApp = { app ->
                    runCatching {
                        startActivity(
                            Intent.makeMainActivity(app.component)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        )
                    }.onFailure {
                        Toast.makeText(this, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                },
                onRequestDefault = ::requestDefaultLauncher,
                onOpenReleases = {
                    runCatching {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/joaoldsxyzbr/Jj-launcher/releases"))
                        )
                    }.onFailure {
                        Toast.makeText(this, "Não foi possível abrir as atualizações", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }
}

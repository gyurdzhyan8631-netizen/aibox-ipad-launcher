package com.aibox.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aibox.launcher.controlcenter.TopSwipeControlCenterHost
import com.aibox.launcher.core.ui.theme.AiBoxLauncherTheme
import com.aibox.launcher.shell.LauncherRootScreen
import com.aibox.launcher.wallpaper.SlowWallpaper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            AiBoxLauncherTheme {
                // Оборачиваем все в Box, чтобы фон был позади интерфейса
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // Наш медленный динамический фон
                    SlowWallpaper() 
                    
                    // Основной интерфейс лаунчера
                    TopSwipeControlCenterHost { openControlCenter ->
                        LauncherRootScreen(onOpenControlCenter = openControlCenter)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

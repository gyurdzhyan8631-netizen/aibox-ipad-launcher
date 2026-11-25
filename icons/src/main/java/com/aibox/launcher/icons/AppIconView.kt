package com.aibox.launcher.icons

import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.aibox.launcher.core.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(256)
    fun get(packageName: String): ImageBitmap? = cache.get(packageName)
    fun put(packageName: String, icon: ImageBitmap) {
        cache.put(packageName, icon)
    }
}

private fun loadIcon(pm: PackageManager, packageName: String): ImageBitmap? =
    runCatching {
        pm.getApplicationIcon(packageName).toBitmap(width = 160, height = 160).asImageBitmap()
    }.getOrNull()?.also { AppIconCache.put(packageName, it) }

/** Загружает иконку приложения в фоне и кэширует её. */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val state = produceState<ImageBitmap?>(
        initialValue = AppIconCache.get(packageName),
        key1 = packageName
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { loadIcon(context.packageManager, packageName) }
        }
    }
    return state.value
}

/** Иконка приложения со скруглением и подписью. */
@Composable
fun AppIconView(
    app: InstalledApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
    showLabel: Boolean = true,
    customIconUri: String? = null,
    cornerRadiusFraction: Float = 0.22f,
    contentPaddingDp: Dp = 8.dp
) {
    val icon = rememberAppIcon(app.packageName)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(contentPaddingDp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = RoundedCornerShape(iconSize * cornerRadiusFraction.coerceIn(0f, 0.5f))
        if (customIconUri != null) {
            AsyncImage(
                model = customIconUri,
                contentDescription = app.label,
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape)
            )
        } else if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.14f))
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(iconSize + 16.dp)
            )
        }
    }
}

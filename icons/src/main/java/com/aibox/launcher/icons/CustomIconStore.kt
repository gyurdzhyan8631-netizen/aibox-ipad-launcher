package com.aibox.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlin.math.min

/** Свои иконки приложений: картинка обрезается до квадрата и хранится в памяти лаунчера. */
object CustomIconStore {

    /** Растёт при каждом изменении — по нему иконки на экране перерисовываются. */
    var version by mutableIntStateOf(0)
        private set

    private fun dir(context: Context): File =
        File(context.filesDir, "custom_icons").apply { mkdirs() }

    fun file(context: Context, packageName: String): File =
        File(dir(context), "$packageName.png")

    fun has(context: Context, packageName: String): Boolean = file(context, packageName).exists()

    /** Вызывать не из главного потока. */
    fun set(context: Context, packageName: String, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching false

        val side = min(source.width, source.height)
        val square = Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side
        )
        val scaled = Bitmap.createScaledBitmap(square, 192, 192, true)
        file(context, packageName).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }

        AppIconCache.remove(packageName)
        version += 1
        true
    }.getOrDefault(false)

    fun clear(context: Context, packageName: String) {
        file(context, packageName).delete()
        AppIconCache.remove(packageName)
        version += 1
    }
}

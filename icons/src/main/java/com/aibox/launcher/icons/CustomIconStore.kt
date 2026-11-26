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

/**
 * Свои иконки приложений и библиотека скачанных иконок.
 * Картинка обрезается до квадрата и хранится в памяти лаунчера.
 */
object CustomIconStore {

    /** Растёт при каждом изменении — по нему иконки на экране перерисовываются. */
    var version by mutableIntStateOf(0)
        private set

    private fun dir(context: Context): File =
        File(context.filesDir, "custom_icons").apply { mkdirs() }

    private fun libraryDir(context: Context): File =
        File(context.filesDir, "icon_library").apply { mkdirs() }

    fun file(context: Context, packageName: String): File =
        File(dir(context), "$packageName.png")

    fun has(context: Context, packageName: String): Boolean = file(context, packageName).exists()

    /** Скачанные пользователем иконки, сначала новые. */
    fun library(context: Context): List<File> =
        libraryDir(context).listFiles { f -> f.isFile && f.name.endsWith(".png") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** Добавляет выбранный файл в библиотеку, но ещё не применяет его к приложению. */
    fun importToLibrary(context: Context, uri: Uri): File? = runCatching {
        val bitmap = decode(context, uri) ?: return@runCatching null
        val target = File(libraryDir(context), "icon_${System.currentTimeMillis()}.png")
        save(target, bitmap)
        target
    }.getOrNull()

    fun deleteFromLibrary(context: Context, file: File) {
        file.delete()
    }

    /** Вызывать не из главного потока. */
    fun set(context: Context, packageName: String, uri: Uri): Boolean = runCatching {
        val bitmap = decode(context, uri) ?: return@runCatching false
        save(file(context, packageName), bitmap)
        changed(packageName)
        true
    }.getOrDefault(false)

    /** Применить готовую картинку (например, из пакета иконок). */
    fun setBitmap(context: Context, packageName: String, bitmap: Bitmap): Boolean = runCatching {
        save(file(context, packageName), bitmap)
        changed(packageName)
        true
    }.getOrDefault(false)

    /** Применить иконку из библиотеки к приложению. */
    fun setFromLibrary(context: Context, packageName: String, source: File): Boolean = runCatching {
        val bitmap = BitmapFactory.decodeFile(source.path) ?: return@runCatching false
        save(file(context, packageName), bitmap)
        changed(packageName)
        true
    }.getOrDefault(false)

    fun clear(context: Context, packageName: String) {
        file(context, packageName).delete()
        changed(packageName)
    }

    private fun changed(packageName: String) {
        AppIconCache.remove(packageName)
        version += 1
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun save(target: File, source: Bitmap) {
        val side = min(source.width, source.height)
        val square = Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side
        )
        val scaled = Bitmap.createScaledBitmap(square, 192, 192, true)
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

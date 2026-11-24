package com.aibox.launcher.settings

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/** Свои обои лаунчера: файлы копируются в память приложения, поэтому доступ не пропадает. */
object WallpaperStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "wallpapers").apply { mkdirs() }

    fun list(context: Context): List<File> =
        dir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun isVideo(file: File): Boolean = file.name.startsWith("vid_")

    /** Копирует выбранный пользователем файл. Вызывать не из главного потока. */
    fun import(context: Context, uri: Uri, video: Boolean): File? = runCatching {
        val mime = context.contentResolver.getType(uri) ?: ""
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (video) "mp4" else "jpg"
        val file = File(dir(context), "${if (video) "vid" else "img"}_${System.currentTimeMillis()}.$extension")
        val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
        input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
        file
    }.getOrNull()
}

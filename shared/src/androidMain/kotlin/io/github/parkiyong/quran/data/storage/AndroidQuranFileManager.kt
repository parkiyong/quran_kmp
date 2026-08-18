package io.github.parkiyong.quran.data.storage

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

class AndroidQuranFileManager(
    private val context: Context,
    override val fileSystem: FileSystem = FileSystem.SYSTEM
) : QuranFileManager {

    override val appDataDirectory: Path by lazy {
        val dir = context.filesDir.toOkioPath()
        fileSystem.createDirectories(dir)
        dir
    }

    override val cacheDirectory: Path by lazy {
        val dir = context.cacheDir.toOkioPath()
        fileSystem.createDirectories(dir)
        dir
    }

    override val databaseDirectory: Path by lazy {
        val dir = appDataDirectory / "databases"
        fileSystem.createDirectories(dir)
        dir
    }

    override val imagesDirectory: Path by lazy {
        val dir = appDataDirectory / "quran_images"
        fileSystem.createDirectories(dir)
        dir
    }

    override val audioDirectory: Path by lazy {
        val dir = (context.getExternalFilesDir(null)?.toOkioPath() ?: appDataDirectory) / "audio"
        fileSystem.createDirectories(dir)
        dir
    }
}

package io.github.parkiyong.quran.data.storage

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class DesktopQuranFileManager(
    override val fileSystem: FileSystem = FileSystem.SYSTEM,
    customBaseDirectory: Path? = null
) : QuranFileManager {

    override val appDataDirectory: Path by lazy {
        val dir = customBaseDirectory ?: run {
            val userHome = System.getProperty("user.home") ?: "."
            val os = System.getProperty("os.name", "").lowercase()
            when {
                os.contains("win") -> {
                    val appData = System.getenv("APPDATA")
                    if (appData != null) "$appData/QuranKmp".toPath()
                    else "$userHome/.quran_kmp".toPath()
                }
                os.contains("mac") -> {
                    "$userHome/Library/Application Support/QuranKmp".toPath()
                }
                else -> {
                    val xdgData = System.getenv("XDG_DATA_HOME")
                    if (xdgData != null) "$xdgData/quran_kmp".toPath()
                    else "$userHome/.local/share/quran_kmp".toPath()
                }
            }
        }
        fileSystem.createDirectories(dir)
        dir
    }

    override val cacheDirectory: Path by lazy {
        val dir = appDataDirectory / "cache"
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
        val dir = appDataDirectory / "audio"
        fileSystem.createDirectories(dir)
        dir
    }
}

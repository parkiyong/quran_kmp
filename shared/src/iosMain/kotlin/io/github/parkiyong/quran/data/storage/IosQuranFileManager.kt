package io.github.parkiyong.quran.data.storage

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosQuranFileManager(
    override val fileSystem: FileSystem = FileSystem.SYSTEM
) : QuranFileManager {

    @OptIn(ExperimentalForeignApi::class)
    override val appDataDirectory: Path by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        val dir = (paths.firstOrNull() as? String ?: "").toPath()
        fileSystem.createDirectories(dir)
        dir
    }

    @OptIn(ExperimentalForeignApi::class)
    override val cacheDirectory: Path by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val dir = (paths.firstOrNull() as? String ?: "").toPath()
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

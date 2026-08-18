package io.github.parkiyong.quran.data.storage

import okio.FileSystem
import okio.Path

interface QuranFileManager {
    val fileSystem: FileSystem
    val appDataDirectory: Path
    val cacheDirectory: Path
    val databaseDirectory: Path
    val imagesDirectory: Path
    val audioDirectory: Path

    fun resolveDatabasePath(databaseName: String): Path {
        return databaseDirectory / databaseName
    }

    fun resolveImagePath(imageName: String): Path {
        return imagesDirectory / imageName
    }

    fun resolveAudioPath(audioName: String): Path {
        return audioDirectory / audioName
    }
}

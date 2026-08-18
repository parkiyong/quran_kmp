package io.github.parkiyong.quran.ui.audio

import io.github.parkiyong.quran.core.viewmodel.BaseViewModel

data class AudioDownloadsUiState(
    val isLoading: Boolean = false,
    val downloads: List<String> = emptyList()
)

sealed interface AudioDownloadsIntent {
    data class CancelDownload(val id: String) : AudioDownloadsIntent
}

sealed interface AudioDownloadsEffect {
    data class ShowMessage(val message: String) : AudioDownloadsEffect
}

class AudioDownloadsViewModel : BaseViewModel<AudioDownloadsUiState, AudioDownloadsIntent, AudioDownloadsEffect>(
    initialState = AudioDownloadsUiState()
) {
    override fun onIntent(intent: AudioDownloadsIntent) {
        when (intent) {
            is AudioDownloadsIntent.CancelDownload -> {
                sendEffect(AudioDownloadsEffect.ShowMessage("Cancelled download ${intent.id}"))
            }
        }
    }
}

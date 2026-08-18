package io.github.parkiyong.quran.ui.surah

import io.github.parkiyong.quran.core.viewmodel.BaseViewModel

data class SurahIndexUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val items: List<String> = emptyList()
)

sealed interface SurahIndexIntent {
    data class SearchQueryChanged(val query: String) : SurahIndexIntent
    data class SurahClicked(val surahNumber: Int) : SurahIndexIntent
}

sealed interface SurahIndexEffect {
    data class NavigateToReader(val surahNumber: Int) : SurahIndexEffect
}

class SurahIndexViewModel : BaseViewModel<SurahIndexUiState, SurahIndexIntent, SurahIndexEffect>(
    initialState = SurahIndexUiState()
) {
    override fun onIntent(intent: SurahIndexIntent) {
        when (intent) {
            is SurahIndexIntent.SearchQueryChanged -> {
                setState { copy(searchQuery = intent.query) }
            }
            is SurahIndexIntent.SurahClicked -> {
                sendEffect(SurahIndexEffect.NavigateToReader(intent.surahNumber))
            }
        }
    }
}

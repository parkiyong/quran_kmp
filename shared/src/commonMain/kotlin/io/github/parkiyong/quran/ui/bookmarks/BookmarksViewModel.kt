package io.github.parkiyong.quran.ui.bookmarks

import io.github.parkiyong.quran.core.viewmodel.BaseViewModel

data class BookmarksUiState(
    val isLoading: Boolean = false,
    val bookmarks: List<String> = emptyList()
)

sealed interface BookmarksIntent {
    data class BookmarkClicked(val id: Long) : BookmarksIntent
}

sealed interface BookmarksEffect {
    data class NavigateToAyah(val id: Long) : BookmarksEffect
}

class BookmarksViewModel : BaseViewModel<BookmarksUiState, BookmarksIntent, BookmarksEffect>(
    initialState = BookmarksUiState()
) {
    override fun onIntent(intent: BookmarksIntent) {
        when (intent) {
            is BookmarksIntent.BookmarkClicked -> {
                sendEffect(BookmarksEffect.NavigateToAyah(intent.id))
            }
        }
    }
}

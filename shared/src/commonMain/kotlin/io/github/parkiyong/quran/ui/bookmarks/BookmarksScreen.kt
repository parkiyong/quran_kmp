package io.github.parkiyong.quran.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bookmarks",
            style = MaterialTheme.typography.headlineMedium
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No bookmarks yet")
        }
    }
}

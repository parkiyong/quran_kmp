package io.github.parkiyong.quran.ui.surah

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SurahIndexScreen(
    viewModel: SurahIndexViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Surah Index",
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onIntent(SurahIndexIntent.SearchQueryChanged(it)) },
            label = { Text("Search Surah...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Surah list will appear here")
        }
    }
}

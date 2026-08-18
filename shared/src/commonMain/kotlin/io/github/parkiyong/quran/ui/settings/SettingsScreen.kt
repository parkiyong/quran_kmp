package io.github.parkiyong.quran.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.nightMode,
                    role = Role.Switch,
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetNightMode(it)) }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Night Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = uiState.nightMode,
                onCheckedChange = null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.keepScreenOn,
                    role = Role.Switch,
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetKeepScreenOn(it)) }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keep Screen On", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = uiState.keepScreenOn,
                onCheckedChange = null
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Translation Font Size: ${uiState.translationFontSize} sp",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = uiState.translationFontSize.toFloat(),
                onValueChange = { viewModel.onIntent(SettingsIntent.SetFontSize(it.toInt())) },
                valueRange = 12f..32f,
                steps = 19
            )
        }
    }
}

package io.github.parkiyong.quran.ui.settings

import io.github.parkiyong.quran.core.viewmodel.BaseViewModel

data class SettingsUiState(
    val nightMode: Boolean = false,
    val keepScreenOn: Boolean = true,
    val translationFontSize: Int = 16
)

sealed interface SettingsIntent {
    data class SetNightMode(val enabled: Boolean) : SettingsIntent
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsIntent
    data class SetFontSize(val size: Int) : SettingsIntent
}

sealed interface SettingsEffect {
    data class RestartAppRequired(val reason: String) : SettingsEffect
}

class SettingsViewModel : BaseViewModel<SettingsUiState, SettingsIntent, SettingsEffect>(
    initialState = SettingsUiState()
) {
    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetNightMode -> setState { copy(nightMode = intent.enabled) }
            is SettingsIntent.SetKeepScreenOn -> setState { copy(keepScreenOn = intent.enabled) }
            is SettingsIntent.SetFontSize -> setState { copy(translationFontSize = intent.size) }
        }
    }
}
